package br.com.roleta.config;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.roleta.usuario.LimitadorTentativas;

@Configuration
public class ConfiguracaoGeral {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Sorteios da roleta precisam ser imprevisíveis para o jogador: usa CSPRNG. */
    @Bean
    public RandomGenerator geradorAleatorio() {
        return new SecureRandom();
    }

    @Bean
    @Qualifier("limitadorLogin")
    public LimitadorTentativas limitadorLogin(Clock clock) {
        return new LimitadorTentativas(5, Duration.ofMinutes(15), clock);
    }

    @Bean
    @Qualifier("limitadorCadastro")
    public LimitadorTentativas limitadorCadastro(Clock clock) {
        return new LimitadorTentativas(10, Duration.ofHours(1), clock);
    }
}
