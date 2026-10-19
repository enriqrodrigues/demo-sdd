package br.com.demo.todo.tarefa;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import br.com.demo.todo.prazo.CalculadoraPrazo;
import br.com.demo.todo.prazo.SituacaoPrazo;

/** Critérios de consulta da listagem de tarefas. */
final class ConsultasTarefa {

    private ConsultasTarefa() {
    }

    /**
     * Vencimento crescente, prioridade decrescente (ALTA, MEDIA, BAIXA) e título crescente. Define a
     * ordenação dentro da Specification, então não serve para consultas de contagem ou paginadas.
     */
    static Specification<Tarefa> ordenadaParaListagem() {
        return (raiz, consulta, cb) -> {
            consulta.orderBy(
                    cb.asc(raiz.get("dataVencimento")),
                    cb.desc(pesoDaPrioridade(raiz, cb)),
                    cb.asc(raiz.get("titulo")));
            return null;
        };
    }

    static Specification<Tarefa> comStatus(StatusTarefa status) {
        return (raiz, consulta, cb) -> status == null ? null : cb.equal(raiz.get("status"), status);
    }

    /**
     * Tradução de {@link CalculadoraPrazo} para predicados sobre status e vencimento, relativa a
     * {@code hoje}. Precisa permanecer equivalente à calculadora (há teste que confronta os dois).
     */
    static Specification<Tarefa> comSituacaoPrazo(SituacaoPrazo situacao, LocalDate hoje) {
        return (raiz, consulta, cb) -> {
            if (situacao == null) {
                return null;
            }
            Predicate pendente = cb.equal(raiz.get("status"), StatusTarefa.PENDENTE);
            Expression<LocalDate> vencimento = raiz.get("dataVencimento");
            return switch (situacao) {
                case CONCLUIDA -> cb.equal(raiz.get("status"), StatusTarefa.CONCLUIDA);
                case ATRASADA -> cb.and(pendente, cb.lessThan(vencimento, hoje));
                case PARA_HOJE -> cb.and(pendente, cb.equal(vencimento, hoje));
                case NO_PRAZO -> cb.and(pendente, cb.greaterThan(vencimento, hoje));
            };
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
