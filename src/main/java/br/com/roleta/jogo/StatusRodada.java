package br.com.roleta.jogo;

public enum StatusRodada {
    /** Ticket pago, pergunta gerada, esperando a resposta do jogador. */
    AGUARDANDO_RESPOSTA,
    /** Legado (versões antigas): errou e não girou. Hoje quem erra também gira. */
    ERROU,
    /** Respondeu (ou o tempo acabou): pode girar; com a chance aumentada só se acertou. */
    LIBERADA_PARA_GIRO,
    /** Roleta girada e prêmio entregue. */
    GIRADA
}
