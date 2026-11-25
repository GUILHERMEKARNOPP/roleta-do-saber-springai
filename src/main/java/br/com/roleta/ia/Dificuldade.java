package br.com.roleta.ia;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public enum Dificuldade {
    FACIL, MEDIO, DIFICIL, MUITO_DIFICIL;

    /** Aceita variações comuns que os modelos devolvem ("Médio", "media", "muito difícil"). */
    public static Optional<Dificuldade> interpretar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "_");
        if (normalizado.equals("MEDIA")) {
            normalizado = "MEDIO";
        }
        try {
            return Optional.of(valueOf(normalizado));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
