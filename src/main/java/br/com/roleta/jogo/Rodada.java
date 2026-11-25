package br.com.roleta.jogo;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.roleta.ia.PerguntaGerada;
import br.com.roleta.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Um ticket comprado: pergunta da IA, resposta do jogador e resultado do giro. */
@Entity
@Table(name = "rodadas", indexes = @Index(name = "idx_rodada_usuario_data", columnList = "usuario_id, criada_em"))
public class Rodada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Impede que duas requisições simultâneas processem a mesma rodada (ex.: girar duas vezes). */
    @Version
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusRodada status;

    @Column(name = "premio_alvo", nullable = false)
    private int premioAlvo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categoria categoria;

    @Column(name = "custo_ticket", nullable = false, precision = 15, scale = 2)
    private BigDecimal custoTicket;

    @Column(nullable = false, length = 20)
    private String dificuldade;

    @Column(nullable = false, length = 600)
    private String pergunta;

    @Column(name = "alternativa_a", nullable = false, length = 250)
    private String alternativaA;

    @Column(name = "alternativa_b", nullable = false, length = 250)
    private String alternativaB;

    @Column(name = "alternativa_c", nullable = false, length = 250)
    private String alternativaC;

    @Column(name = "alternativa_d", nullable = false, length = 250)
    private String alternativaD;

    @Column(name = "resposta_correta", nullable = false, length = 1)
    private String respostaCorreta;

    @Column(nullable = false, length = 900)
    private String explicacao;

    /** Pontos percentuais que a IA decidiu somar à chance do prêmio escolhido. */
    @Column(name = "bonus_chance", nullable = false)
    private int bonusChance;

    @Column(name = "resposta_jogador", length = 1)
    private String respostaJogador;

    @Column(name = "tempo_esgotado", nullable = false)
    private boolean tempoEsgotado;

    @Column(name = "chance_final")
    private Integer chanceFinal;

    @Column(name = "premio_sorteado")
    private Integer premioSorteado;

    @Column(name = "valor_ganho", precision = 15, scale = 2)
    private BigDecimal valorGanho;

    @Column(name = "descricao_ganho", length = 120)
    private String descricaoGanho;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    protected Rodada() {
    }

    public Rodada(Usuario usuario, int premioAlvo, Categoria categoria, BigDecimal custoTicket,
            PerguntaGerada pergunta, Instant criadaEm) {
        this.usuario = usuario;
        this.status = StatusRodada.AGUARDANDO_RESPOSTA;
        this.premioAlvo = premioAlvo;
        this.categoria = categoria;
        this.custoTicket = custoTicket;
        this.dificuldade = pergunta.dificuldade().name();
        this.pergunta = pergunta.pergunta();
        this.alternativaA = pergunta.alternativas().get(0);
        this.alternativaB = pergunta.alternativas().get(1);
        this.alternativaC = pergunta.alternativas().get(2);
        this.alternativaD = pergunta.alternativas().get(3);
        this.respostaCorreta = pergunta.respostaCorreta();
        this.explicacao = pergunta.explicacao();
        this.bonusChance = pergunta.bonusChance();
        this.criadaEm = criadaEm;
    }

    public void registrarAcerto(String resposta, int chanceFinal, Instant agora) {
        this.respostaJogador = resposta;
        this.chanceFinal = chanceFinal;
        this.status = StatusRodada.LIBERADA_PARA_GIRO;
        this.finalizadaEm = agora;
    }

    /** Errou: ainda gira a roleta, mas só com a chance base (sem o bônus da IA). */
    public void registrarErro(String resposta, int chanceBase, Instant agora) {
        this.respostaJogador = resposta;
        this.chanceFinal = chanceBase;
        this.status = StatusRodada.LIBERADA_PARA_GIRO;
        this.finalizadaEm = agora;
    }

    public void registrarTempoEsgotado(int chanceBase, Instant agora) {
        this.tempoEsgotado = true;
        registrarErro(null, chanceBase, agora);
    }

    public boolean acertou() {
        return respostaCorreta.equals(respostaJogador);
    }

    public void registrarGiro(int premioSorteado, BigDecimal valorGanho, String descricaoGanho, Instant agora) {
        this.premioSorteado = premioSorteado;
        this.valorGanho = valorGanho;
        this.descricaoGanho = descricaoGanho;
        this.status = StatusRodada.GIRADA;
        this.finalizadaEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public StatusRodada getStatus() {
        return status;
    }

    public int getPremioAlvo() {
        return premioAlvo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getCustoTicket() {
        return custoTicket;
    }

    public String getDificuldade() {
        return dificuldade;
    }

    public String getPergunta() {
        return pergunta;
    }

    public String getAlternativaA() {
        return alternativaA;
    }

    public String getAlternativaB() {
        return alternativaB;
    }

    public String getAlternativaC() {
        return alternativaC;
    }

    public String getAlternativaD() {
        return alternativaD;
    }

    public String getRespostaCorreta() {
        return respostaCorreta;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public int getBonusChance() {
        return bonusChance;
    }

    public String getRespostaJogador() {
        return respostaJogador;
    }

    public boolean isTempoEsgotado() {
        return tempoEsgotado;
    }

    public Integer getChanceFinal() {
        return chanceFinal;
    }

    public Integer getPremioSorteado() {
        return premioSorteado;
    }

    public BigDecimal getValorGanho() {
        return valorGanho;
    }

    public String getDescricaoGanho() {
        return descricaoGanho;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }
}
