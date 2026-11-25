package br.com.roleta.jogo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.roleta.ia.GeradorPerguntaIA;
import br.com.roleta.ia.PerguntaGerada;
import br.com.roleta.jogo.JogoDto.CategoriaView;
import br.com.roleta.jogo.JogoDto.EstadoJogo;
import br.com.roleta.jogo.JogoDto.HistoricoItem;
import br.com.roleta.jogo.JogoDto.PaginaHistorico;
import br.com.roleta.jogo.JogoDto.PremioView;
import br.com.roleta.jogo.JogoDto.ResultadoGiro;
import br.com.roleta.jogo.JogoDto.ResultadoResposta;
import br.com.roleta.jogo.JogoDto.RodadaView;
import br.com.roleta.usuario.Usuario;
import br.com.roleta.usuario.UsuarioRepository;
import br.com.roleta.web.RecursoNaoEncontradoException;
import br.com.roleta.web.RegraDoJogoException;

@Service
public class JogoService {

    public static final BigDecimal CUSTO_TICKET = new BigDecimal("200.00");
    public static final Duration PRAZO_RESPOSTA = Duration.ofSeconds(60);
    /** Folga para a latência da rede entre o fim do cronômetro no navegador e a chegada da resposta. */
    private static final Duration TOLERANCIA_RESPOSTA = Duration.ofSeconds(5);
    private static final int CHANCE_MAXIMA = 95;
    private static final int PERGUNTAS_RECENTES_PARA_IA = 15;
    private static final int ITENS_POR_PAGINA = 8;
    private static final Set<StatusRodada> STATUS_ABERTOS =
            Set.of(StatusRodada.AGUARDANDO_RESPOSTA, StatusRodada.LIBERADA_PARA_GIRO);

    private final UsuarioRepository usuarios;
    private final RodadaRepository rodadas;
    private final TabelaPremios tabela;
    private final GeradorPerguntaIA geradorPergunta;
    private final SorteadorRoleta sorteador;
    private final TransactionTemplate transacao;
    private final Clock clock;
    /** Evita que o mesmo jogador dispare duas compras ao mesmo tempo (duas chamadas à IA e dois débitos). */
    private final Set<Long> comprasEmAndamento = ConcurrentHashMap.newKeySet();

    public JogoService(UsuarioRepository usuarios, RodadaRepository rodadas, TabelaPremios tabela,
            GeradorPerguntaIA geradorPergunta, SorteadorRoleta sorteador, TransactionTemplate transacao,
            Clock clock) {
        this.usuarios = usuarios;
        this.rodadas = rodadas;
        this.tabela = tabela;
        this.geradorPergunta = geradorPergunta;
        this.sorteador = sorteador;
        this.transacao = transacao;
        this.clock = clock;
    }

    public int chanceBase() {
        return 100 / tabela.quantidade();
    }

    @Transactional
    public EstadoJogo estado(Long usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        RodadaView rodadaAtual = buscarRodadaAberta(usuarioId).map(this::paraView).orElse(null);
        return new EstadoJogo(usuario.getUsername(), usuario.getSaldo(), usuario.getXp(), CUSTO_TICKET,
                chanceBase(), PRAZO_RESPOSTA.toSeconds(),
                tabela.todos().stream().map(p -> new PremioView(p.indice(), p.tipo(), p.rotulo())).toList(),
                Arrays.stream(Categoria.values()).map(c -> new CategoriaView(c, c.getNome())).toList(),
                rodadaAtual);
    }

    /**
     * Compra um ticket: a IA gera a pergunta e só depois o valor é debitado,
     * assim uma falha da IA não custa nada ao jogador.
     */
    public RodadaView comprarTicket(Long usuarioId, int premioIndice, Categoria categoria) {
        Premio alvo = tabela.porIndice(premioIndice);
        if (!comprasEmAndamento.add(usuarioId)) {
            throw new RegraDoJogoException("Sua pergunta já está sendo gerada. Aguarde.");
        }
        try {
            List<String> recentes = transacao.execute(status -> validarCompraEListarRecentes(usuarioId));
            PerguntaGerada pergunta = geradorPergunta.gerar(alvo, tabela.posicaoNoRanking(alvo), tabela.todos(),
                    categoria, recentes);
            return transacao.execute(status -> {
                if (usuarios.debitar(usuarioId, CUSTO_TICKET) == 0) {
                    throw new RegraDoJogoException("Saldo insuficiente para comprar um ticket.");
                }
                Rodada rodada = rodadas.save(new Rodada(usuarios.getReferenceById(usuarioId), alvo.indice(),
                        categoria, CUSTO_TICKET, pergunta, clock.instant()));
                return paraView(rodada);
            });
        } finally {
            comprasEmAndamento.remove(usuarioId);
        }
    }

    @Transactional
    public ResultadoResposta responder(Long usuarioId, Long rodadaId, String alternativa) {
        Rodada rodada = buscarRodadaDoUsuario(usuarioId, rodadaId);
        if (rodada.getStatus() != StatusRodada.AGUARDANDO_RESPOSTA) {
            throw new RegraDoJogoException("Esta pergunta já foi respondida.");
        }
        Instant agora = clock.instant();
        if (prazoEsgotado(rodada, agora)) {
            rodada.registrarTempoEsgotado(agora);
        } else if (rodada.getRespostaCorreta().equals(alternativa)) {
            int chanceFinal = Math.min(chanceBase() + rodada.getBonusChance(), CHANCE_MAXIMA);
            rodada.registrarAcerto(alternativa, chanceFinal, agora);
        } else {
            rodada.registrarErro(alternativa, agora);
        }
        boolean acertou = rodada.getStatus() == StatusRodada.LIBERADA_PARA_GIRO;
        return new ResultadoResposta(acertou, rodada.isTempoEsgotado(), rodada.getRespostaCorreta(),
                rodada.getExplicacao(), chanceBase(), rodada.getChanceFinal(), buscarUsuario(usuarioId).getSaldo());
    }

    @Transactional
    public ResultadoGiro girar(Long usuarioId, Long rodadaId) {
        Rodada rodada = buscarRodadaDoUsuario(usuarioId, rodadaId);
        if (rodada.getStatus() != StatusRodada.LIBERADA_PARA_GIRO) {
            throw new RegraDoJogoException("Você precisa acertar a pergunta antes de girar a roleta.");
        }
        int sorteado = sorteador.sortear(rodada.getPremioAlvo(), rodada.getChanceFinal(), tabela.quantidade());
        Premio premio = tabela.porIndice(sorteado);
        Entrega entrega = calcularEntrega(premio);
        // A rodada é atualizada antes dos créditos: os updates em lote limpam o contexto JPA.
        rodada.registrarGiro(sorteado, entrega.valor(), entrega.descricao(), clock.instant());
        aplicarEntrega(usuarioId, premio, entrega);
        Usuario usuario = buscarUsuario(usuarioId);
        return new ResultadoGiro(sorteado, entrega.descricao(), sorteado == rodada.getPremioAlvo(),
                usuario.getSaldo(), usuario.getXp());
    }

    @Transactional(readOnly = true)
    public PaginaHistorico historico(Long usuarioId, int pagina) {
        Page<Rodada> page = rodadas.findByUsuarioIdOrderByCriadaEmDesc(usuarioId,
                PageRequest.of(pagina, ITENS_POR_PAGINA));
        List<HistoricoItem> itens = page.getContent().stream().map(this::paraHistorico).toList();
        return new PaginaHistorico(itens, page.getNumber(), page.getTotalPages());
    }

    private List<String> validarCompraEListarRecentes(Long usuarioId) {
        if (buscarRodadaAberta(usuarioId).isPresent()) {
            throw new RegraDoJogoException("Você já tem uma rodada em andamento. Termine-a primeiro.");
        }
        if (buscarUsuario(usuarioId).getSaldo().compareTo(CUSTO_TICKET) < 0) {
            throw new RegraDoJogoException("Saldo insuficiente para comprar um ticket.");
        }
        return rodadas.perguntasRecentes(usuarioId, PageRequest.of(0, PERGUNTAS_RECENTES_PARA_IA));
    }

    /** Rodada em aberto do jogador; se a pergunta passou do prazo, encerra como erro. */
    private Optional<Rodada> buscarRodadaAberta(Long usuarioId) {
        Optional<Rodada> aberta = rodadas.findFirstByUsuarioIdAndStatusInOrderByCriadaEmDesc(usuarioId,
                STATUS_ABERTOS);
        Instant agora = clock.instant();
        if (aberta.isPresent() && aberta.get().getStatus() == StatusRodada.AGUARDANDO_RESPOSTA
                && prazoEsgotado(aberta.get(), agora)) {
            aberta.get().registrarTempoEsgotado(agora);
            return Optional.empty();
        }
        return aberta;
    }

    private boolean prazoEsgotado(Rodada rodada, Instant agora) {
        return agora.isAfter(rodada.getCriadaEm().plus(PRAZO_RESPOSTA).plus(TOLERANCIA_RESPOSTA));
    }

    private Rodada buscarRodadaDoUsuario(Long usuarioId, Long rodadaId) {
        return rodadas.findByIdAndUsuarioId(rodadaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rodada não encontrada."));
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private record Entrega(BigDecimal valor, String descricao) {
    }

    private Entrega calcularEntrega(Premio premio) {
        return switch (premio.tipo()) {
            case DINHEIRO -> new Entrega(premio.valor(), reais(premio.valor()) + " creditados no saldo");
            case FICHAS, TICKET -> new Entrega(premio.valor(),
                    premio.rotulo() + " (" + reais(premio.valor()) + " no saldo)");
            case MISTERIO -> {
                BigDecimal valor = BigDecimal.valueOf(sorteador.valorMisterio());
                yield new Entrega(valor, "Mistério revelado: " + reais(valor) + " no saldo");
            }
            case XP -> new Entrega(premio.valor(), "+" + premio.rotulo());
            case ROUPA, VEICULO -> new Entrega(premio.valorReferencia(),
                    premio.rotulo() + " adicionado aos seus prêmios");
        };
    }

    private void aplicarEntrega(Long usuarioId, Premio premio, Entrega entrega) {
        switch (premio.tipo()) {
            case DINHEIRO, FICHAS, TICKET, MISTERIO -> usuarios.creditar(usuarioId, entrega.valor());
            case XP -> usuarios.adicionarXp(usuarioId, entrega.valor().longValue());
            case ROUPA, VEICULO -> {
                // Itens ficam registrados na própria rodada (histórico de prêmios).
            }
        }
    }

    private RodadaView paraView(Rodada rodada) {
        long restantes = Math.max(0, PRAZO_RESPOSTA.minus(Duration.between(rodada.getCriadaEm(), clock.instant()))
                .toSeconds());
        return new RodadaView(rodada.getId(), rodada.getStatus(), rodada.getPremioAlvo(),
                rodada.getCategoria().getNome(), rodada.getDificuldade(), rodada.getPergunta(),
                List.of(rodada.getAlternativaA(), rodada.getAlternativaB(), rodada.getAlternativaC(),
                        rodada.getAlternativaD()),
                rodada.getBonusChance(), chanceBase(), rodada.getChanceFinal(), restantes);
    }

    private HistoricoItem paraHistorico(Rodada rodada) {
        String resultado = switch (rodada.getStatus()) {
            case AGUARDANDO_RESPOSTA -> "Aguardando resposta";
            case LIBERADA_PARA_GIRO -> "Acertou! Falta girar";
            case ERROU -> rodada.isTempoEsgotado()
                    ? "Tempo esgotado (-" + reais(rodada.getCustoTicket()) + ")"
                    : "Errou (-" + reais(rodada.getCustoTicket()) + ")";
            case GIRADA -> rodada.getDescricaoGanho();
        };
        return new HistoricoItem(rodada.getId(), rodada.getCriadaEm(),
                tabela.porIndice(rodada.getPremioAlvo()).rotulo(), rodada.getCategoria().getNome(),
                rodada.getDificuldade(), rodada.getStatus(), resultado);
    }

    private static String reais(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        formato.setMaximumFractionDigits(0);
        return formato.format(valor);
    }
}
