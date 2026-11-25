package br.com.roleta.jogo;

public enum TipoPremio {
    /** Crédito em reais no saldo. */
    DINHEIRO,
    /** Fichas: convertidas em reais no saldo. */
    FICHAS,
    /** Bônus em reais no saldo. */
    BONUS,
    /** Item colecionável (vai para "Meus prêmios"). */
    ROUPA,
    /** Item colecionável (vai para "Meus prêmios"). */
    VEICULO,
    /** Tickets grátis: creditados em reais no valor dos tickets. */
    TICKET,
    /** Valor surpresa em reais, sorteado na hora do giro. */
    MISTERIO
}
