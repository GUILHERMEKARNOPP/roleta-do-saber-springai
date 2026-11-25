package br.com.roleta.ia;

/**
 * Formato JSON bruto pedido ao modelo. Tudo aqui é não confiável até passar por
 * {@link PerguntaGerada#validar(RespostaIA)}.
 */
public record RespostaIA(
        String pergunta,
        String alternativaA,
        String alternativaB,
        String alternativaC,
        String alternativaD,
        String respostaCorreta,
        String dificuldade,
        Integer bonusChance,
        String explicacao) {
}
