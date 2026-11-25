package br.com.roleta.jogo;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

/** Sorteio feito no servidor; o navegador só anima a roleta até a fatia sorteada. */
@Component
public class SorteadorRoleta {

    private final RandomGenerator random;

    public SorteadorRoleta(RandomGenerator random) {
        this.random = random;
    }

    /**
     * O prêmio alvo sai com {@code chanceAlvoPercentual}%; o restante é dividido
     * igualmente entre as outras fatias.
     */
    public int sortear(int indiceAlvo, int chanceAlvoPercentual, int totalFatias) {
        if (totalFatias < 2 || indiceAlvo < 0 || indiceAlvo >= totalFatias) {
            throw new IllegalArgumentException("Parâmetros de sorteio inválidos");
        }
        if (chanceAlvoPercentual < 0 || chanceAlvoPercentual > 100) {
            throw new IllegalArgumentException("Chance fora de 0..100: " + chanceAlvoPercentual);
        }
        if (random.nextInt(100) < chanceAlvoPercentual) {
            return indiceAlvo;
        }
        int outra = random.nextInt(totalFatias - 1);
        return outra >= indiceAlvo ? outra + 1 : outra;
    }

    /** Valor do prêmio mistério: múltiplo de R$ 1.000 entre R$ 2.000 e o máximo. */
    public int valorMisterio() {
        return random.nextInt(2, TabelaPremios.MISTERIO_MAXIMO / 1000 + 1) * 1000;
    }
}
