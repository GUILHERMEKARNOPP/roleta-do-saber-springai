package br.com.roleta.jogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Random;

import org.junit.jupiter.api.Test;

class SorteadorRoletaTest {

    private static final int FATIAS = 20;

    @Test
    void chanceDe100PorCentoSempreDaOPremioAlvo() {
        var sorteador = new SorteadorRoleta(new Random(1));
        for (int i = 0; i < 1_000; i++) {
            assertThat(sorteador.sortear(7, 100, FATIAS)).isEqualTo(7);
        }
    }

    @Test
    void chanceZeroNuncaDaOPremioAlvoESempreCaiEmFatiaValida() {
        var sorteador = new SorteadorRoleta(new Random(2));
        for (int i = 0; i < 5_000; i++) {
            int sorteado = sorteador.sortear(0, 0, FATIAS);
            assertThat(sorteado).isBetween(1, FATIAS - 1);
        }
    }

    @Test
    void frequenciaDoAlvoAcompanhaAChanceAumentada() {
        var sorteador = new SorteadorRoleta(new Random(42));
        int rodadas = 200_000;
        int acertosAlvo = 0;
        int[] porFatia = new int[FATIAS];
        for (int i = 0; i < rodadas; i++) {
            int sorteado = sorteador.sortear(19, 30, FATIAS);
            porFatia[sorteado]++;
            if (sorteado == 19) {
                acertosAlvo++;
            }
        }
        assertThat(acertosAlvo / (double) rodadas).isCloseTo(0.30, within(0.01));
        double esperadoOutras = 0.70 / (FATIAS - 1);
        for (int i = 0; i < FATIAS - 1; i++) {
            assertThat(porFatia[i] / (double) rodadas).isCloseTo(esperadoOutras, within(0.005));
        }
    }

    @Test
    void rejeitaParametrosInvalidos() {
        var sorteador = new SorteadorRoleta(new Random(3));
        assertThatThrownBy(() -> sorteador.sortear(20, 10, FATIAS)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sorteador.sortear(0, 101, FATIAS)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valorMisterioEhMultiploDeMilDentroDaFaixa() {
        var sorteador = new SorteadorRoleta(new Random(4));
        for (int i = 0; i < 2_000; i++) {
            int valor = sorteador.valorMisterio();
            assertThat(valor % 1000).isZero();
            assertThat(valor).isBetween(2_000, TabelaPremios.MISTERIO_MAXIMO);
        }
    }
}
