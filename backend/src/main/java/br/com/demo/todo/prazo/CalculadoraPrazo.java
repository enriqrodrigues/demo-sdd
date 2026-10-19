package br.com.demo.todo.prazo;

import java.time.LocalDate;

/** Situação de prazo derivada da conclusão, do vencimento e da data corrente; nunca persistida. */
public final class CalculadoraPrazo {

    private CalculadoraPrazo() {
    }

    public static SituacaoPrazo calcular(boolean concluida, LocalDate dataVencimento, LocalDate hoje) {
        if (concluida) {
            return SituacaoPrazo.CONCLUIDA;
        }
        if (hoje.isAfter(dataVencimento)) {
            return SituacaoPrazo.ATRASADA;
        }
        return hoje.isEqual(dataVencimento) ? SituacaoPrazo.PARA_HOJE : SituacaoPrazo.NO_PRAZO;
    }
}
