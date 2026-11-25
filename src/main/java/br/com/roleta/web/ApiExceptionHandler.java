package br.com.roleta.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.roleta.ia.IaIndisponivelException;
import br.com.roleta.jogo.JogoApiController;
import jakarta.validation.ConstraintViolationException;

/** Respostas de erro genéricas para o cliente; detalhes ficam só no log do servidor. */
@RestControllerAdvice(assignableTypes = JogoApiController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record Erro(String mensagem) {
    }

    @ExceptionHandler(RegraDoJogoException.class)
    public ResponseEntity<Erro> regra(RegraDoJogoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Erro> naoEncontrado(RecursoNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<Erro> iaIndisponivel(IaIndisponivelException e) {
        log.error("IA indisponível", e);
        return resposta(HttpStatus.SERVICE_UNAVAILABLE,
                "A IA não conseguiu gerar a pergunta agora. Seu saldo não foi descontado; tente novamente.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Erro> concorrencia(ObjectOptimisticLockingFailureException e) {
        return resposta(HttpStatus.CONFLICT, "Esta ação já foi processada.");
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Erro> requisicaoInvalida(Exception e) {
        log.debug("Requisição inválida: {}", e.getMessage());
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Erro> inesperado(Exception e) {
        log.error("Erro inesperado na API do jogo", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado. Tente novamente.");
    }

    private static ResponseEntity<Erro> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new Erro(mensagem));
    }
}
