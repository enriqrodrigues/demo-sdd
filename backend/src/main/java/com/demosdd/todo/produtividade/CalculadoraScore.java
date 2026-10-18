package com.demosdd.todo.produtividade;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regra pura do score: concluídas no prazo ÷ concluídas na semana, em percentual. */
public final class CalculadoraScore {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraScore() {
    }

    public static ScoreProdutividade calcular(Semana semana, long concluidasNoPrazo, long totalConcluidas) {
        return new ScoreProdutividade(semana.inicio(), semana.fim(), concluidasNoPrazo, totalConcluidas,
                percentual(concluidasNoPrazo, totalConcluidas));
    }

    /** Duas casas decimais, meio para cima; {@code null} quando não há tarefas concluídas. */
    static BigDecimal percentual(long concluidasNoPrazo, long totalConcluidas) {
        if (totalConcluidas == 0) {
            return null;
        }
        return BigDecimal.valueOf(concluidasNoPrazo)
                .multiply(CEM)
                .divide(BigDecimal.valueOf(totalConcluidas), 2, RoundingMode.HALF_UP);
    }
}
