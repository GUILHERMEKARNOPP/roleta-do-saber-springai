package br.com.roleta.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PerguntaGeradaTest {

    private static RespostaIA resposta(String correta, String dificuldade, Integer bonus, String altD) {
        return new RespostaIA("Qual é a capital da Austrália?", "Sydney", "Canberra", "Melbourne", altD,
                correta, dificuldade, bonus, "Canberra foi planejada para ser a capital.");
    }

    @Test
    void aceitaRespostaValidaENormalizaCampos() {
        PerguntaGerada pergunta = PerguntaGerada.validar(resposta(" b ", "Médio", 15, "Perth"));

        assertThat(pergunta.respostaCorreta()).isEqualTo("B");
        assertThat(pergunta.dificuldade()).isEqualTo(Dificuldade.MEDIO);
        assertThat(pergunta.bonusChance()).isEqualTo(15);
        assertThat(pergunta.alternativas()).containsExactly("Sydney", "Canberra", "Melbourne", "Perth");
    }

    @Test
    void bonusDaIaSempreFicaDentroDaFaixaPermitida() {
        assertThat(PerguntaGerada.validar(resposta("B", "MUITO_DIFICIL", 90, "Perth")).bonusChance())
                .isEqualTo(PerguntaGerada.BONUS_MAXIMO);
        assertThat(PerguntaGerada.validar(resposta("B", "FACIL", -3, "Perth")).bonusChance())
                .isEqualTo(PerguntaGerada.BONUS_MINIMO);
    }

    @Test
    void rejeitaLetraCorretaForaDeAaD() {
        assertThatThrownBy(() -> PerguntaGerada.validar(resposta("E", "FACIL", 10, "Perth")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aceitaTextoDaAlternativaOuLetraComPontuacaoNoLugarDaLetra() {
        assertThat(PerguntaGerada.validar(resposta("Canberra", "FACIL", 10, "Perth")).respostaCorreta())
                .as("texto da alternativa B, mesmo começando com C").isEqualTo("B");
        assertThat(PerguntaGerada.validar(resposta("b)", "FACIL", 10, "Perth")).respostaCorreta()).isEqualTo("B");
    }

    @Test
    void naoDeduzLetraPelaInicialDeUmaPalavraQueNaoEhAlternativa() {
        assertThatThrownBy(() -> PerguntaGerada.validar(resposta("Brasília", "FACIL", 10, "Perth")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaAlternativasRepetidasEDificuldadeDesconhecida() {
        assertThatThrownBy(() -> PerguntaGerada.validar(resposta("B", "FACIL", 10, "sydney")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PerguntaGerada.validar(resposta("B", "impossível", 10, "Perth")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaBonusAusente() {
        assertThatThrownBy(() -> PerguntaGerada.validar(resposta("B", "FACIL", null, "Perth")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void embaralharMantemALetraApontandoParaATextoCorreto() {
        PerguntaGerada original = PerguntaGerada.validar(resposta("B", "FACIL", 10, "Perth"));
        Random random = new Random(7);
        for (int i = 0; i < 50; i++) {
            PerguntaGerada embaralhada = original.embaralhar(random);
            int indiceCorreto = PerguntaGerada.LETRAS.indexOf(embaralhada.respostaCorreta());
            assertThat(embaralhada.alternativas().get(indiceCorreto)).isEqualTo("Canberra");
            assertThat(embaralhada.alternativas()).containsExactlyInAnyOrderElementsOf(original.alternativas());
        }
    }
}
