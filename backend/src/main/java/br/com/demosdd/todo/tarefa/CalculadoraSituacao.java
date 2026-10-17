package br.com.demosdd.todo.tarefa;

import java.time.LocalDate;

public final class CalculadoraSituacao {

    private CalculadoraSituacao() {
    }

    public static Situacao calcular(StatusTarefa status, LocalDate dataVencimento, LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return Situacao.CONCLUIDA;
        }
        if (hoje.isAfter(dataVencimento)) {
            return Situacao.ATRASADA;
        }
        if (hoje.isEqual(dataVencimento)) {
            return Situacao.PARA_HOJE;
        }
        return Situacao.NO_PRAZO;
    }
}
