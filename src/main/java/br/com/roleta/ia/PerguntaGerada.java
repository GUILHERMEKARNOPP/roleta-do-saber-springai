package br.com.roleta.ia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;

/** Pergunta já validada e pronta para o jogo. */
public record PerguntaGerada(
        String pergunta,
        List<String> alternativas,
        String respostaCorreta,
        Dificuldade dificuldade,
        int bonusChance,
        String explicacao) {

    public static final List<String> LETRAS = List.of("A", "B", "C", "D");
    /** A IA escolhe o bônus, mas sempre dentro desta faixa: acertar sempre aumenta a chance. */
    public static final int BONUS_MINIMO = 5;
    public static final int BONUS_MAXIMO = 40;

    private static final int MAX_PERGUNTA = 600;
    private static final int MAX_ALTERNATIVA = 250;
    private static final int MAX_EXPLICACAO = 900;

    /** Valida a saída do modelo; lança {@link IllegalArgumentException} se vier fora do formato. */
    public static PerguntaGerada validar(RespostaIA resposta) {
        if (resposta == null) {
            throw new IllegalArgumentException("Resposta vazia da IA");
        }
        String pergunta = textoObrigatorio(resposta.pergunta(), MAX_PERGUNTA, "pergunta");
        List<String> alternativas = List.of(
                textoObrigatorio(resposta.alternativaA(), MAX_ALTERNATIVA, "alternativaA"),
                textoObrigatorio(resposta.alternativaB(), MAX_ALTERNATIVA, "alternativaB"),
                textoObrigatorio(resposta.alternativaC(), MAX_ALTERNATIVA, "alternativaC"),
                textoObrigatorio(resposta.alternativaD(), MAX_ALTERNATIVA, "alternativaD"));
        var distintas = new HashSet<String>();
        alternativas.forEach(a -> distintas.add(a.toLowerCase(Locale.ROOT)));
        if (distintas.size() != 4) {
            throw new IllegalArgumentException("Alternativas repetidas");
        }
        String correta = letraCorreta(resposta.respostaCorreta(), alternativas);
        Dificuldade dificuldade = Dificuldade.interpretar(resposta.dificuldade())
                .orElseThrow(() -> new IllegalArgumentException("Dificuldade inválida: " + resposta.dificuldade()));
        if (resposta.bonusChance() == null) {
            throw new IllegalArgumentException("Bônus de chance ausente");
        }
        int bonus = Math.clamp(resposta.bonusChance(), BONUS_MINIMO, BONUS_MAXIMO);
        String explicacao = textoObrigatorio(resposta.explicacao(), MAX_EXPLICACAO, "explicacao");
        return new PerguntaGerada(pergunta, alternativas, correta, dificuldade, bonus, explicacao);
    }

    /** Modelos tendem a repetir a mesma letra correta; embaralhar garante distribuição justa. */
    public PerguntaGerada embaralhar(RandomGenerator random) {
        String textoCorreto = alternativas.get(LETRAS.indexOf(respostaCorreta));
        List<String> embaralhadas = new ArrayList<>(alternativas);
        for (int i = embaralhadas.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String temp = embaralhadas.get(i);
            embaralhadas.set(i, embaralhadas.get(j));
            embaralhadas.set(j, temp);
        }
        String novaLetra = LETRAS.get(embaralhadas.indexOf(textoCorreto));
        return new PerguntaGerada(pergunta, List.copyOf(embaralhadas), novaLetra, dificuldade, bonusChance, explicacao);
    }

    /**
     * Aceita a letra ("B", "b)", "B.") ou o texto exato de uma alternativa, que alguns modelos
     * devolvem no lugar da letra. Nunca deduz a letra pela inicial de uma palavra ("Canberra" não é "C").
     */
    private static String letraCorreta(String bruta, List<String> alternativas) {
        String limpa = bruta == null ? "" : bruta.strip();
        for (int i = 0; i < alternativas.size(); i++) {
            if (alternativas.get(i).equalsIgnoreCase(limpa)) {
                return LETRAS.get(i);
            }
        }
        boolean apenasLetra = limpa.length() == 1 || (limpa.length() > 1 && !Character.isLetterOrDigit(limpa.charAt(1)));
        String letra = limpa.isEmpty() ? "" : limpa.substring(0, 1).toUpperCase(Locale.ROOT);
        if (apenasLetra && LETRAS.contains(letra)) {
            return letra;
        }
        throw new IllegalArgumentException("Resposta correta inválida: " + bruta);
    }

    private static String textoObrigatorio(String valor, int max, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Campo vazio: " + campo);
        }
        String limpo = valor.strip();
        if (limpo.length() > max) {
            throw new IllegalArgumentException("Campo muito longo: " + campo);
        }
        return limpo;
    }
}
