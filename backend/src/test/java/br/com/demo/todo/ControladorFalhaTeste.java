package br.com.demo.todo;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.demo.todo.tarefa.Tarefa;

/** Endpoints só de teste que simulam falhas difíceis de provocar por requisições reais. */
@RestController
public class ControladorFalhaTeste {

    public static final String CAMINHO = "/api/teste/falha";
    public static final String CAMINHO_CORRIDA_DE_VERSAO = "/api/teste/corrida-de-versao";

    @GetMapping(CAMINHO)
    public String falhar() {
        throw new IllegalStateException(
                "org.postgresql.util.PSQLException: ERROR: deadlock detected em select * from tarefa where id = ?");
    }

    /** O que o JPA lança quando outra transação muda a linha entre a leitura e o commit. */
    @GetMapping(CAMINHO_CORRIDA_DE_VERSAO)
    public String perderCorridaDeVersao() {
        throw new ObjectOptimisticLockingFailureException(Tarefa.class, UUID.randomUUID());
    }
}
