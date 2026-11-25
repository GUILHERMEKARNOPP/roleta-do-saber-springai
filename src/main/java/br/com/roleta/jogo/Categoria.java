package br.com.roleta.jogo;

/** Allowlist de temas que o jogador pode escolher para a pergunta. */
public enum Categoria {
    CONHECIMENTOS_GERAIS("Conhecimentos Gerais"),
    HISTORIA("História"),
    GEOGRAFIA("Geografia"),
    CIENCIAS("Ciências"),
    TECNOLOGIA("Tecnologia"),
    ESPORTES("Esportes"),
    CINEMA_E_MUSICA("Cinema e Música");

    private final String nome;

    Categoria(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
