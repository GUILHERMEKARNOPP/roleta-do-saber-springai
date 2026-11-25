package br.com.roleta.ia;

public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
