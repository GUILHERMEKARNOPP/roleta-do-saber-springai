package br.com.roleta.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExtrairJsonTest {

    @Test
    void removeRaciocinioECercaDeMarkdown() {
        String texto = "<think>vou pensar {nao e json}</think>\nAqui está:\n```json\n{\"pergunta\":\"x\"}\n```";
        assertThat(GeradorPerguntaIA.extrairJson(texto)).isEqualTo("{\"pergunta\":\"x\"}");
    }

    @Test
    void falhaQuandoNaoHaJson() {
        assertThatThrownBy(() -> GeradorPerguntaIA.extrairJson("desculpe, não posso ajudar"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
