package br.com.demo.todo.tarefa;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Tarefa inexistente: vira 404 em Problem Details. */
public class TarefaNaoEncontradaException extends ErrorResponseException {

    public TarefaNaoEncontradaException(UUID id) {
        super(HttpStatus.NOT_FOUND, problema(id), null);
    }

    private static ProblemDetail problema(UUID id) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "Tarefa " + id + " não encontrada.");
        problema.setTitle("Tarefa não encontrada");
        return problema;
    }
}
