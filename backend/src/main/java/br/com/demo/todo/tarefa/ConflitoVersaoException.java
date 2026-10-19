package br.com.demo.todo.tarefa;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

import br.com.demo.todo.comum.ManipuladorErros;

/** A tarefa mudou desde que o cliente a leu: vira 409 em Problem Details. */
public class ConflitoVersaoException extends ErrorResponseException {

    public ConflitoVersaoException() {
        super(HttpStatus.CONFLICT, ManipuladorErros.problemaDeConflito(), null);
    }
}
