package br.com.roleta.jogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.roleta.RelogioAjustavel;
import br.com.roleta.ia.Dificuldade;
import br.com.roleta.ia.GeradorPerguntaIA;
import br.com.roleta.ia.IaIndisponivelException;
import br.com.roleta.ia.PerguntaGerada;
import br.com.roleta.jogo.JogoDto.ResultadoGiro;
import br.com.roleta.jogo.JogoDto.ResultadoResposta;
import br.com.roleta.jogo.JogoDto.RodadaView;
import br.com.roleta.usuario.CadastroForm;
import br.com.roleta.usuario.CadastroService;
import br.com.roleta.usuario.Usuario;
import br.com.roleta.usuario.UsuarioRepository;
import br.com.roleta.web.RecursoNaoEncontradoException;
import br.com.roleta.web.RegraDoJogoException;

@SpringBootTest
class JogoServiceTest {

    private static final int PREMIO_R15_MIL = 2;
    private static final int PREMIO_BONUS_500 = 1;
    private static final int PREMIO_CARRO = 18;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelogioAjustavel relogioDeTeste() {
            return new RelogioAjustavel();
        }
    }

    @Autowired
    private JogoService jogoService;
    @Autowired
    private CadastroService cadastroService;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private RodadaRepository rodadas;
    @Autowired
    private RelogioAjustavel relogio;
    @Autowired
    private TransactionTemplate transacao;

    @MockitoBean
    private GeradorPerguntaIA geradorPergunta;
    @MockitoBean
    private SorteadorRoleta sorteador;

    private Long jogadorId;

    @BeforeEach
    void setUp() {
        jogadorId = criarJogador();
        var pergunta = new PerguntaGerada("Quanto é 2 + 2?", List.of("3", "4", "5", "22"), "B",
                Dificuldade.FACIL, 20, "2 + 2 = 4.");
        when(geradorPergunta.gerar(any(), anyInt(), anyList(), any(), anyList())).thenReturn(pergunta);
    }

    @Test
    void novoJogadorComecaComMilReais() {
        assertThat(saldo(jogadorId)).isEqualByComparingTo("1000");
    }

    @Test
    void acertarLiberaGiroComChanceAumentadaEGirarEntregaOPremio() {
        RodadaView rodada = jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.CIENCIAS);
        assertThat(saldo(jogadorId)).isEqualByComparingTo("800");
        assertThat(rodada.status()).isEqualTo(StatusRodada.AGUARDANDO_RESPOSTA);

        ResultadoResposta resposta = jogoService.responder(jogadorId, rodada.id(), "B");
        assertThat(resposta.acertou()).isTrue();
        assertThat(resposta.chanceFinal()).isEqualTo(5 + 20);

        when(sorteador.sortear(PREMIO_R15_MIL, 25, 20)).thenReturn(PREMIO_R15_MIL);
        ResultadoGiro giro = jogoService.girar(jogadorId, rodada.id());

        assertThat(giro.ganhouPremioEscolhido()).isTrue();
        assertThat(giro.saldo()).isEqualByComparingTo("15800");
        assertThat(rodadas.findById(rodada.id()).orElseThrow().getStatus()).isEqualTo(StatusRodada.GIRADA);
    }

    @Test
    void naoPermiteGirarDuasVezesNaMesmaRodada() {
        RodadaView rodada = jogoService.comprarTicket(jogadorId, PREMIO_CARRO, Categoria.HISTORIA);
        jogoService.responder(jogadorId, rodada.id(), "B");
        when(sorteador.sortear(eq(PREMIO_CARRO), anyInt(), anyInt())).thenReturn(PREMIO_BONUS_500);

        jogoService.girar(jogadorId, rodada.id());

        assertThatThrownBy(() -> jogoService.girar(jogadorId, rodada.id()))
                .isInstanceOf(RegraDoJogoException.class);
        assertThat(saldo(jogadorId)).as("bônus creditado uma única vez").isEqualByComparingTo("1300");
    }

    @Test
    void errarAindaGiraMasSemBonusDeChance() {
        RodadaView rodada = jogoService.comprarTicket(jogadorId, PREMIO_CARRO, Categoria.HISTORIA);

        ResultadoResposta resposta = jogoService.responder(jogadorId, rodada.id(), "A");

        assertThat(resposta.acertou()).isFalse();
        assertThat(resposta.respostaCorreta()).isEqualTo("B");
        assertThat(resposta.chanceFinal()).as("sem bônus: chance base").isEqualTo(5);
        assertThat(saldo(jogadorId)).isEqualByComparingTo("800");

        when(sorteador.sortear(PREMIO_CARRO, 5, 20)).thenReturn(PREMIO_BONUS_500);
        ResultadoGiro giro = jogoService.girar(jogadorId, rodada.id());

        assertThat(giro.ganhouPremioEscolhido()).isFalse();
        assertThat(giro.saldo()).isEqualByComparingTo("1300");
    }

    @Test
    void naoGiraAntesDeResponder() {
        RodadaView rodada = jogoService.comprarTicket(jogadorId, PREMIO_CARRO, Categoria.HISTORIA);

        assertThatThrownBy(() -> jogoService.girar(jogadorId, rodada.id()))
                .isInstanceOf(RegraDoJogoException.class);
    }

    @Test
    void responderDepoisDoPrazoContaComoErro() {
        RodadaView rodada = jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.GEOGRAFIA);
        relogio.avancar(JogoService.PRAZO_RESPOSTA.plus(Duration.ofSeconds(30)));

        ResultadoResposta resposta = jogoService.responder(jogadorId, rodada.id(), "B");

        assertThat(resposta.acertou()).isFalse();
        assertThat(resposta.tempoEsgotado()).isTrue();
        assertThat(resposta.chanceFinal()).isEqualTo(5);
    }

    @Test
    void naoCompraSemSaldoENemChamaAIa() {
        transacao.executeWithoutResult(t -> usuarios.debitar(jogadorId, new BigDecimal("900")));

        assertThatThrownBy(() -> jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.ESPORTES))
                .isInstanceOf(RegraDoJogoException.class)
                .hasMessageContaining("Saldo insuficiente");
        verify(geradorPergunta, never()).gerar(any(), anyInt(), anyList(), any(), anyList());
    }

    @Test
    void naoPermiteComprarComRodadaEmAndamento() {
        jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.TECNOLOGIA);

        assertThatThrownBy(() -> jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.TECNOLOGIA))
                .isInstanceOf(RegraDoJogoException.class);
        assertThat(saldo(jogadorId)).isEqualByComparingTo("800");
    }

    @Test
    void falhaDaIaNaoDescontaSaldo() {
        when(geradorPergunta.gerar(any(), anyInt(), anyList(), any(), anyList()))
                .thenThrow(new IaIndisponivelException("fora do ar", null));

        assertThatThrownBy(() -> jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.CIENCIAS))
                .isInstanceOf(IaIndisponivelException.class);
        assertThat(saldo(jogadorId)).isEqualByComparingTo("1000");
    }

    @Test
    void jogadorNaoAcessaRodadaDeOutroJogador() {
        RodadaView rodada = jogoService.comprarTicket(jogadorId, PREMIO_R15_MIL, Categoria.CIENCIAS);
        Long intrusoId = criarJogador();

        assertThatThrownBy(() -> jogoService.responder(intrusoId, rodada.id(), "B"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void premioInvalidoEhRejeitado() {
        assertThatThrownBy(() -> jogoService.comprarTicket(jogadorId, 20, Categoria.CIENCIAS))
                .isInstanceOf(RegraDoJogoException.class);
    }

    private Long criarJogador() {
        var form = new CadastroForm();
        form.setUsername("j" + UUID.randomUUID().toString().substring(0, 12).replace("-", ""));
        form.setSenha("senha-forte-123");
        form.setConfirmacaoSenha("senha-forte-123");
        Usuario usuario = cadastroService.cadastrar(form);
        return usuario.getId();
    }

    private BigDecimal saldo(Long id) {
        return usuarios.findById(id).orElseThrow().getSaldo();
    }
}
