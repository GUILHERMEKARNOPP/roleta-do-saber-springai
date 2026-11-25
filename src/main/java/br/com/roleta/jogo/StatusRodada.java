package br.com.roleta.jogo;

public enum StatusRodada {
    /** Ticket pago, pergunta gerada, esperando a resposta do jogador. */
    AGUARDANDO_RESPOSTA,
    /** Errou (ou estourou o tempo): perdeu o valor do ticket. */
    ERROU,
    /** Acertou: pode girar a roleta com a chance aumentada. */
    LIBERADA_PARA_GIRO,
    /** Roleta girada e prêmio entregue. */
    GIRADA
}
