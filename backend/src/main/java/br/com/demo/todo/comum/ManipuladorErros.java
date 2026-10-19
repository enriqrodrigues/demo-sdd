package br.com.demo.todo.comum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Formato único de erro da API (Problem Details, RFC 9457). Exceções do Spring MVC e
 * {@code ErrorResponseException} são tratadas pela classe base; o resto vira 500 genérico.
 */
@RestControllerAdvice
public class ManipuladorErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManipuladorErros.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception excecao) {
        LOG.error("Erro inesperado ao processar a requisição", excecao);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problema.setTitle("Erro interno");
        return problema;
    }
}
