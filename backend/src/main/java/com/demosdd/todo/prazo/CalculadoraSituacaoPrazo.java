package com.demosdd.todo.prazo;

import java.time.LocalDate;

import com.demosdd.todo.tarefa.StatusTarefa;

/** Regra pura da situação de prazo: depende só do status, do vencimento e da data de hoje. */
public final class CalculadoraSituacaoPrazo {

    private CalculadoraSituacaoPrazo() {
    }

    public static SituacaoPrazo calcular(StatusTarefa status, LocalDate dataVencimento, LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return SituacaoPrazo.CONCLUIDA;
        }
        if (hoje.isAfter(dataVencimento)) {
            return SituacaoPrazo.ATRASADA;
        }
        if (hoje.isEqual(dataVencimento)) {
            return SituacaoPrazo.PARA_HOJE;
        }
        return SituacaoPrazo.NO_PRAZO;
    }
}
