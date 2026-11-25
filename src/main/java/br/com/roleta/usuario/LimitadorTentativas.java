package br.com.roleta.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conta tentativas por chave (usuário+IP, IP...) dentro de uma janela de tempo fixa.
 * Usado para frear força bruta no login e criação em massa de contas.
 */
public class LimitadorTentativas {

    /** Teto de chaves em memória: impede que um atacante esgote a memória com chaves únicas. */
    static final int LIMITE_ENTRADAS_EM_MEMORIA = 10_000;

    private record Janela(int tentativas, Instant inicio) {
    }

    private final int maxTentativas;
    private final Duration duracaoJanela;
    private final Clock clock;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimitadorTentativas(int maxTentativas, Duration duracaoJanela, Clock clock) {
        this.maxTentativas = maxTentativas;
        this.duracaoJanela = duracaoJanela;
        this.clock = clock;
    }

    public boolean bloqueado(String chave) {
        Janela janela = janelas.get(chave);
        return janela != null && !expirou(janela) && janela.tentativas() >= maxTentativas;
    }

    /** Registra uma tentativa (ex.: login que falhou). */
    public void registrar(String chave) {
        if (semEspacoPara(chave)) {
            return;
        }
        incrementar(chave);
    }

    /**
     * Registra a tentativa e diz, de forma atômica, se ela ainda está dentro do limite.
     * Com a memória cheia, recusa (falha fechada).
     */
    public boolean tentarConsumir(String chave) {
        if (semEspacoPara(chave)) {
            return false;
        }
        return incrementar(chave).tentativas() <= maxTentativas;
    }

    public void limpar(String chave) {
        janelas.remove(chave);
    }

    private Janela incrementar(String chave) {
        return janelas.compute(chave, (k, atual) -> atual == null || expirou(atual)
                ? new Janela(1, clock.instant())
                : new Janela(atual.tentativas() + 1, atual.inicio()));
    }

    private boolean semEspacoPara(String chave) {
        if (janelas.size() < LIMITE_ENTRADAS_EM_MEMORIA || janelas.containsKey(chave)) {
            return false;
        }
        janelas.values().removeIf(this::expirou);
        return janelas.size() >= LIMITE_ENTRADAS_EM_MEMORIA;
    }

    private boolean expirou(Janela janela) {
        return clock.instant().isAfter(janela.inicio().plus(duracaoJanela));
    }
}
