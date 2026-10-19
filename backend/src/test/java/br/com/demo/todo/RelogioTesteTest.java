package br.com.demo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

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
}
