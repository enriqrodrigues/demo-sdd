package com.demosdd.todo.prazo;

import static com.demosdd.todo.prazo.CalculadoraSituacaoPrazo.calcular;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.demosdd.todo.tarefa.StatusTarefa;

class CalculadoraSituacaoPrazoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 18);

    @Test
    void pendenteVencidaEstaAtrasada() {
        assertThat(calcular(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 17), HOJE))
                .isEqualTo(SituacaoPrazo.ATRASADA);
    }

    @Test
    void pendenteQueVenceHojeEstaParaHoje() {
        assertThat(calcular(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 18), HOJE))
                .isEqualTo(SituacaoPrazo.PARA_HOJE);
    }

    @Test
    void pendenteComVencimentoFuturoEstaNoPrazo() {
        assertThat(calcular(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 19), HOJE))
                .isEqualTo(SituacaoPrazo.NO_PRAZO);
    }

    @Test
    void concluidaDentroDoPrazoEstaConcluida() {
        // concluída em 2026-10-18, vencimento 2026-10-20
        assertThat(calcular(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 20), HOJE))
                .isEqualTo(SituacaoPrazo.CONCLUIDA);
    }

    @Test
    void concluidaComAtrasoEstaConcluida() {
        // concluída em 2026-10-18, vencimento 2026-10-10
        assertThat(calcular(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 10), HOJE))
                .isEqualTo(SituacaoPrazo.CONCLUIDA);
    }

    @Test
    void situacaoMudaComAPassagemDoTempo() {
        LocalDate vencimento = LocalDate.of(2026, 10, 20);

        assertThat(calcular(StatusTarefa.PENDENTE, vencimento, LocalDate.of(2026, 10, 19)))
                .isEqualTo(SituacaoPrazo.NO_PRAZO);
        assertThat(calcular(StatusTarefa.PENDENTE, vencimento, LocalDate.of(2026, 10, 20)))
                .isEqualTo(SituacaoPrazo.PARA_HOJE);
        assertThat(calcular(StatusTarefa.PENDENTE, vencimento, LocalDate.of(2026, 10, 21)))
                .isEqualTo(SituacaoPrazo.ATRASADA);
    }

    @Test
    void viradaDeDiaEmUtcAntesDeSaoPauloUsaADataDeSaoPaulo() {
        // 2026-10-18T01:00Z é 2026-10-17 22:00 em São Paulo
        Clock clock = Clock.fixed(Instant.parse("2026-10-18T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        LocalDate hoje = LocalDate.now(clock);

        assertThat(hoje).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(calcular(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 17), hoje))
                .isEqualTo(SituacaoPrazo.PARA_HOJE);
    }
}
