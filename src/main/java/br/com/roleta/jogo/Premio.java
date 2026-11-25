package br.com.roleta.jogo;

import java.math.BigDecimal;

/**
 * Uma fatia da roleta.
 *
 * @param valor            quantidade entregue (reais para dinheiro/fichas/ticket, pontos para XP;
 *                         para itens e mistério é igual à referência)
 * @param valorReferencia  valor equivalente em reais, usado pela IA para calibrar a dificuldade
 */
public record Premio(int indice, TipoPremio tipo, String rotulo, BigDecimal valor, BigDecimal valorReferencia) {
}
