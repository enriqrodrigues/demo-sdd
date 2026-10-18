package com.demosdd.todo.produtividade;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CalculadoraScoreTest {

    // --- semana corrente ---

    @Test
    void dataNoMeioDaSemana() {
        Semana semana = Semana.contendo(LocalDate.of(2026, 10, 14)); // quarta-feira

        assertThat(semana.inicio()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(semana.fim()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void dataNoDomingo() {
        Semana semana = Semana.contendo(LocalDate.of(2026, 10, 18));

        assertThat(semana.inicio()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(semana.fim()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void dataNaSegundaFeira() {
        Semana semana = Semana.contendo(LocalDate.of(2026, 10, 19));

        assertThat(semana.inicio()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(semana.fim()).isEqualTo(LocalDate.of(2026, 10, 25));
    }

    // --- percentual ---

    @Test
    void percentualComArredondamento() {
        assertThat(CalculadoraScore.percentual(2, 3)).isEqualByComparingTo("66.67");
        assertThat(CalculadoraScore.percentual(1, 3)).isEqualByComparingTo("33.33");
        // 1/32 = 3,125%: o meio arredonda para cima
        assertThat(CalculadoraScore.percentual(1, 32)).isEqualByComparingTo("3.13");
    }

    @Test
    void todasNoPrazo() {
        assertThat(CalculadoraScore.percentual(4, 4)).isEqualByComparingTo("100");
    }

    @Test
    void nenhumaNoPrazo() {
        assertThat(CalculadoraScore.percentual(0, 2)).isEqualByComparingTo("0");
    }

    @Test
    void nenhumaTarefaConcluidaNaSemana() {
        ScoreProdutividade score = CalculadoraScore.calcular(Semana.contendo(LocalDate.of(2026, 10, 14)), 0, 0);

        assertThat(score.concluidasNoPrazo()).isZero();
        assertThat(score.totalConcluidas()).isZero();
        assertThat(score.percentual()).isNull();
    }

    @Test
    void scoreTrazPeriodoContagensEPercentualComDuasCasas() {
        ScoreProdutividade score = CalculadoraScore.calcular(Semana.contendo(LocalDate.of(2026, 10, 14)), 2, 3);

        assertThat(score.inicioSemana()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(score.fimSemana()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(score.concluidasNoPrazo()).isEqualTo(2);
        assertThat(score.totalConcluidas()).isEqualTo(3);
        assertThat(score.percentual()).isEqualTo(new BigDecimal("66.67"));
    }
}
