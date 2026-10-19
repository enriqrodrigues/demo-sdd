package br.com.demo.todo.tarefa;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Tarefa inexistente: vira 404 em Problem Details. */
public class TarefaNaoEncontradaException extends ErrorResponseException {

    public TarefaNaoEncontradaException(UUID id) {
        super(HttpStatus.NOT_FOUND, problema(), null);
    }

    /** Mensagem fixa: o identificador pedido não é repetido na resposta (design D7). */
    private static ProblemDetail problema() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "A tarefa não existe ou foi excluída.");
        problema.setTitle("Tarefa não encontrada");
        return problema;
    }
}
