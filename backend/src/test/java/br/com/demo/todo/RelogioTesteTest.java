package br.com.demo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class RelogioTesteTest {

    private final RelogioTeste relogio = new RelogioTeste(Instant.parse("2026-10-21T12:00:00Z"));

    @Test
    void ajustarDefineOInstante() {
        relogio.ajustar(Instant.parse("2026-10-25T03:00:00Z"));

        assertThat(relogio.instant()).isEqualTo(Instant.parse("2026-10-25T03:00:00Z"));
    }

    @Test
    void avancarSomaADuracao() {
        relogio.avancar(Duration.ofDays(1));

        assertThat(relogio.instant()).isEqualTo(Instant.parse("2026-10-22T12:00:00Z"));
    }

    @Test
    void withZoneDevolveVisaoNoFusoQueAcompanhaOInstanteMutavel() {
        Clock emSaoPaulo = relogio.withZone(ZoneId.of("America/Sao_Paulo"));

        relogio.ajustar(Instant.parse("2026-10-21T01:30:00Z"));

        assertThat(emSaoPaulo.getZone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
        assertThat(emSaoPaulo.instant()).isEqualTo(Instant.parse("2026-10-21T01:30:00Z"));
        assertThat(LocalDate.now(emSaoPaulo)).isEqualTo(LocalDate.parse("2026-10-20"));
    }
}
