package br.com.demosdd.todo.produtividade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Regras puras do score semanal (RF03). */
public final class CalculadoraScore {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public record Conclusao(LocalDate dataConclusao, LocalDate dataVencimento) {

        boolean noPrazo() {
            return !dataConclusao.isAfter(dataVencimento);
        }
    }

    private CalculadoraScore() {
    }

    public static LocalDate inicioSemana(LocalDate hoje) {
        return hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate fimSemana(LocalDate hoje) {
        return inicioSemana(hoje).plusDays(6);
    }

    public static long contarNoPrazo(List<Conclusao> conclusoes) {
        return conclusoes.stream().filter(Conclusao::noPrazo).count();
    }

    /** Percentual com duas casas (HALF_UP); null quando não há conclusões. */
    public static BigDecimal percentual(long noPrazo, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(noPrazo).multiply(CEM).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
