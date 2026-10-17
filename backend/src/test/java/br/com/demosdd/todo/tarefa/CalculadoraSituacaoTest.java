package br.com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CalculadoraSituacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 16);

    @Test
    void pendenteVencidaOntemEstaAtrasada() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.PENDENTE, HOJE.minusDays(1), HOJE))
                .isEqualTo(Situacao.ATRASADA);
    }

    @Test
    void pendenteQueVenceHojeEstaParaHoje() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.PENDENTE, HOJE, HOJE))
                .isEqualTo(Situacao.PARA_HOJE);
    }

    @Test
    void pendenteQueVenceAmanhaEstaNoPrazo() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.PENDENTE, HOJE.plusDays(1), HOJE))
                .isEqualTo(Situacao.NO_PRAZO);
    }

    @Test
    void concluidaComVencimentoPassadoEstaConcluida() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.CONCLUIDA, HOJE.minusDays(10), HOJE))
                .isEqualTo(Situacao.CONCLUIDA);
    }

    @Test
    void concluidaComVencimentoFuturoEstaConcluida() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.CONCLUIDA, HOJE.plusDays(10), HOJE))
                .isEqualTo(Situacao.CONCLUIDA);
    }
}
