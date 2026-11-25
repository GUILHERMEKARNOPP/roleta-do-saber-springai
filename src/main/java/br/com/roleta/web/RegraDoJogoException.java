package br.com.roleta.web;

/** Violação de regra de negócio; a mensagem é segura para exibir ao jogador. */
public class RegraDoJogoException extends RuntimeException {

    public RegraDoJogoException(String mensagem) {
        super(mensagem);
    }
}
