package br.com.roleta.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import br.com.roleta.RelogioAjustavel;

class LimitadorTentativasTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimitadorTentativas limitador = new LimitadorTentativas(3, Duration.ofMinutes(15), relogio);

    @Test
    void bloqueiaAoAtingirOLimiteEDesbloqueiaQuandoAJanelaExpira() {
        limitador.registrar("ana|1.1.1.1");
        limitador.registrar("ana|1.1.1.1");
        assertThat(limitador.bloqueado("ana|1.1.1.1")).isFalse();

        limitador.registrar("ana|1.1.1.1");
        assertThat(limitador.bloqueado("ana|1.1.1.1")).isTrue();
        assertThat(limitador.bloqueado("ana|2.2.2.2")).as("outro IP não é afetado").isFalse();

        relogio.avancar(Duration.ofMinutes(16));
        assertThat(limitador.bloqueado("ana|1.1.1.1")).isFalse();
    }

    @Test
    void tentarConsumirRecusaAPartirDaTentativaAlemDoLimite() {
        assertThat(limitador.tentarConsumir("10.0.0.1")).isTrue();
        assertThat(limitador.tentarConsumir("10.0.0.1")).isTrue();
        assertThat(limitador.tentarConsumir("10.0.0.1")).isTrue();
        assertThat(limitador.tentarConsumir("10.0.0.1")).isFalse();
    }

    @Test
    void limparZeraAsTentativas() {
        limitador.registrar("bia|1.1.1.1");
        limitador.registrar("bia|1.1.1.1");
        limitador.registrar("bia|1.1.1.1");
        limitador.limpar("bia|1.1.1.1");
        assertThat(limitador.bloqueado("bia|1.1.1.1")).isFalse();
    }

    @Test
    void naoCresceAlemDoTetoDeMemoria() {
        for (int i = 0; i < LimitadorTentativas.LIMITE_ENTRADAS_EM_MEMORIA; i++) {
            limitador.registrar("chave-" + i);
        }
        assertThat(limitador.tentarConsumir("chave-nova")).as("memória cheia: falha fechada").isFalse();

        relogio.avancar(Duration.ofMinutes(16));
        assertThat(limitador.tentarConsumir("chave-nova")).as("entradas expiradas são liberadas").isTrue();
    }
}
