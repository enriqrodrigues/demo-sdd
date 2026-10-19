package br.com.demo.todo.tarefa;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

/** Critérios de consulta da listagem de tarefas. */
final class ConsultasTarefa {

    private ConsultasTarefa() {
    }

    /** Vencimento crescente, prioridade decrescente (ALTA, MEDIA, BAIXA) e título crescente. */
    static Specification<Tarefa> ordenadaParaListagem() {
        return (raiz, consulta, cb) -> {
            consulta.orderBy(
                    cb.asc(raiz.get("dataVencimento")),
                    cb.desc(pesoDaPrioridade(raiz, cb)),
                    cb.asc(raiz.get("titulo")));
            return null;
        };
    }

    /** A ordem textual dos nomes não corresponde à importância, então ordena por peso. */
    private static Expression<Integer> pesoDaPrioridade(Root<Tarefa> raiz, CriteriaBuilder cb) {
        return cb.<Prioridade, Integer>selectCase(raiz.get("prioridade"))
                .when(Prioridade.ALTA, 3)
                .when(Prioridade.MEDIA, 2)
                .otherwise(1);
    }
}
