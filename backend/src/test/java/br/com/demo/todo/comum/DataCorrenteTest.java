package br.com.demo.todo.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import br.com.demo.todo.prazo.CalculadoraPrazo;
import br.com.demo.todo.prazo.SituacaoPrazo;

/** Spec prazo-tarefa › Data corrente no fuso configurado. O Clock é UTC, como o bean de produção. */
class DataCorrenteTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T01:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate VENCIMENTO = LocalDate.parse("2026-10-20");

    @Test
    void expoeOFusoUsadoNasConversoes() {
        assertThat(new DataCorrente(CLOCK, ZoneId.of("America/Sao_Paulo")).fuso())
                .isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    void viradaDeDiaEmUtcAntesDoFusoLocal() {
        DataCorrente dataCorrente = new DataCorrente(CLOCK, ZoneId.of("America/Sao_Paulo"));

        assertThat(dataCorrente.hoje()).isEqualTo(LocalDate.parse("2026-10-20"));
        assertThat(CalculadoraPrazo.calcular(false, VENCIMENTO, dataCorrente.hoje()))
                .isEqualTo(SituacaoPrazo.PARA_HOJE);
    }

    @Test
    void fusoConfiguradoDiferente() {
        DataCorrente dataCorrente = new DataCorrente(CLOCK, ZoneId.of("UTC"));

        assertThat(dataCorrente.hoje()).isEqualTo(LocalDate.parse("2026-10-21"));
        assertThat(CalculadoraPrazo.calcular(false, VENCIMENTO, dataCorrente.hoje()))
                .isEqualTo(SituacaoPrazo.ATRASADA);
    }
}
