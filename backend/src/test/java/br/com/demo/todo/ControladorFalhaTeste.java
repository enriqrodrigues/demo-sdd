package br.com.demo.todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint só de teste que simula uma falha interna com detalhes que não podem vazar na resposta. */
@RestController
public class ControladorFalhaTeste {

    public static final String CAMINHO = "/api/teste/falha";

    @GetMapping(CAMINHO)
    public String falhar() {
        throw new IllegalStateException(
                "org.postgresql.util.PSQLException: ERROR: deadlock detected em select * from tarefa where id = ?");
    }
}
