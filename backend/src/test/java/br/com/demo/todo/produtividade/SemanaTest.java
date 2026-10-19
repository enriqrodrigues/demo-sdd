package br.com.demo.todo.produtividade;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import br.com.demo.todo.comum.DataCorrente;

/** Spec score-produtividade › Semana corrente, com Clock UTC e fuso America/Sao_Paulo. */
class SemanaTest {

    private static Semana semanaCorrenteEm(String instante) {
        Clock clock = Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
        return Semana.contendo(new DataCorrente(clock, ZoneId.of("America/Sao_Paulo")).hoje());
    }

    @Test
    void consultaNumaQuartaFeira() {
        assertThat(semanaCorrenteEm("2026-10-21T12:00:00Z"))
                .isEqualTo(new Semana(LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-25")));
    }

    @Test
    void consultaNumDomingo() {
        assertThat(semanaCorrenteEm("2026-10-25T12:00:00Z"))
                .isEqualTo(new Semana(LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-25")));
    }

    @Test
    void consultaNumaSegundaFeira() {
        assertThat(semanaCorrenteEm("2026-10-26T12:00:00Z"))
                .isEqualTo(new Semana(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-11-01")));
    }

    @Test
    void domingoANoiteNoFusoAindaEhASemanaAnteriorMesmoJaSendoSegundaEmUtc() {
        assertThat(semanaCorrenteEm("2026-10-26T02:00:00Z"))
                .isEqualTo(new Semana(LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-25")));
    }
}
