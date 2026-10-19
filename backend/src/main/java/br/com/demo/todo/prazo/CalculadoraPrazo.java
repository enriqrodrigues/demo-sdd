package br.com.demo.todo.prazo;

import java.time.LocalDate;

import br.com.demo.todo.tarefa.StatusTarefa;

/** Situação de prazo derivada de status, vencimento e data corrente; nunca persistida. */
public final class CalculadoraPrazo {

    private CalculadoraPrazo() {
    }

    public static SituacaoPrazo calcular(StatusTarefa status, LocalDate dataVencimento, LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return SituacaoPrazo.CONCLUIDA;
        }
        if (hoje.isAfter(dataVencimento)) {
            return SituacaoPrazo.ATRASADA;
        }
        return hoje.isEqual(dataVencimento) ? SituacaoPrazo.PARA_HOJE : SituacaoPrazo.NO_PRAZO;
    }
}
