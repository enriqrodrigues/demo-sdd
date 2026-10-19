package br.com.demo.todo.tarefa;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A tarefa mudou desde que o cliente a leu: vira 409 em Problem Details. */
public class ConflitoVersaoException extends ErrorResponseException {

    public ConflitoVersaoException() {
        super(HttpStatus.CONFLICT, problema(), null);
    }

    static ProblemDetail problema() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A tarefa foi alterada por outra ação. Recarregue-a e tente novamente.");
        problema.setTitle("Conflito de versão");
        return problema;
    }
}
