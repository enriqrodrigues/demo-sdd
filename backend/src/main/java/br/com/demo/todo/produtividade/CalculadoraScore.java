package br.com.demo.todo.produtividade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Regras puras do score semanal. */
public final class CalculadoraScore {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraScore() {
    }

    /** No prazo quando a data local da conclusão, no fuso, é igual ou anterior ao vencimento. */
    public static boolean concluidaNoPrazo(Instant concluidaEm, LocalDate dataVencimento, ZoneId fuso) {
        return !LocalDate.ofInstant(concluidaEm, fuso).isAfter(dataVencimento);
    }

    /** {@code noPrazo / concluidas × 100} com 2 casas, meio para cima; {@code null} sem conclusões. */
    public static BigDecimal percentual(int noPrazo, int concluidas) {
        if (concluidas == 0) {
            return null;
        }
        return BigDecimal.valueOf(noPrazo).multiply(CEM)
                .divide(BigDecimal.valueOf(concluidas), 2, RoundingMode.HALF_UP);
    }
}
