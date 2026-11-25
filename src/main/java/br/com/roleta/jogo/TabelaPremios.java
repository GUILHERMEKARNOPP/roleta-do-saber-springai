package br.com.roleta.jogo;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.roleta.web.RegraDoJogoException;

/** As 20 fatias da roleta, em sentido horário a partir do ponteiro (topo). */
@Component
public class TabelaPremios {

    /** Valor máximo que o prêmio "Mistério" pode pagar. */
    public static final int MISTERIO_MAXIMO = 30_000;

    private final List<Premio> premios = List.of(
            item(0, TipoPremio.ROUPA, "Jaqueta de Couro", 3_000),
            xp(1, 2_000),
            reais(2, TipoPremio.DINHEIRO, "R$ 15.000", 15_000),
            reais(3, TipoPremio.FICHAS, "8.000 em fichas", 8_000),
            reais(4, TipoPremio.TICKET, "2 Tickets grátis", 400),
            xp(5, 4_000),
            reais(6, TipoPremio.DINHEIRO, "R$ 25.000", 25_000),
            reais(7, TipoPremio.FICHAS, "12.000 em fichas", 12_000),
            item(8, TipoPremio.ROUPA, "Tênis Neon", 2_500),
            xp(9, 6_000),
            reais(10, TipoPremio.FICHAS, "18.000 em fichas", 18_000),
            item(11, TipoPremio.MISTERIO, "Prêmio Mistério", MISTERIO_MAXIMO),
            item(12, TipoPremio.ROUPA, "Óculos Dourado", 4_000),
            xp(13, 8_000),
            reais(14, TipoPremio.DINHEIRO, "R$ 35.000", 35_000),
            reais(15, TipoPremio.FICHAS, "22.000 em fichas", 22_000),
            item(16, TipoPremio.ROUPA, "Relógio de Ouro", 6_000),
            xp(17, 12_000),
            item(18, TipoPremio.VEICULO, "Carro Esportivo", 80_000),
            reais(19, TipoPremio.DINHEIRO, "R$ 50.000", 50_000));

    public List<Premio> todos() {
        return premios;
    }

    public int quantidade() {
        return premios.size();
    }

    public Premio porIndice(int indice) {
        if (indice < 0 || indice >= premios.size()) {
            throw new RegraDoJogoException("Prêmio inválido.");
        }
        return premios.get(indice);
    }

    /** Posição do prêmio no ranking de valor (1 = mais valioso). */
    public int posicaoNoRanking(Premio premio) {
        List<Premio> ordenados = premios.stream()
                .sorted(Comparator.comparing(Premio::valorReferencia).reversed())
                .toList();
        return ordenados.indexOf(premio) + 1;
    }

    private static Premio reais(int indice, TipoPremio tipo, String rotulo, int valor) {
        BigDecimal v = BigDecimal.valueOf(valor);
        return new Premio(indice, tipo, rotulo, v, v);
    }

    private static Premio item(int indice, TipoPremio tipo, String rotulo, int valorReferencia) {
        BigDecimal v = BigDecimal.valueOf(valorReferencia);
        return new Premio(indice, tipo, rotulo, v, v);
    }

    /** XP vale R$ 0,10 por ponto como referência de dificuldade. */
    private static Premio xp(int indice, int pontos) {
        return new Premio(indice, TipoPremio.XP, String.format("%,d XP", pontos).replace(',', '.'),
                BigDecimal.valueOf(pontos), BigDecimal.valueOf(pontos / 10));
    }
}
