package br.com.demo.todo.produtividade;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/** Spec score-produtividade: Conclusão no prazo e Percentual do score. Fuso America/Sao_Paulo. */
class CalculadoraScoreTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    // --- Conclusão no prazo

    @Test
    void concluidaAntesDoVencimento() {
        assertThat(CalculadoraScore.concluidaNoPrazo(
                Instant.parse("2026-10-21T15:00:00Z"), LocalDate.parse("2026-10-23"), SAO_PAULO)).isTrue();
    }

    @Test
    void concluidaNoDiaDoVencimento() {
        // 23:59 de 2026-10-21 em São Paulo; em UTC já é 2026-10-22.
        assertThat(CalculadoraScore.concluidaNoPrazo(
                Instant.parse("2026-10-22T02:59:00Z"), LocalDate.parse("2026-10-21"), SAO_PAULO)).isTrue();
    }

    @Test
    void concluidaDepoisDoVencimento() {
        assertThat(CalculadoraScore.concluidaNoPrazo(
                Instant.parse("2026-10-21T15:00:00Z"), LocalDate.parse("2026-10-20"), SAO_PAULO)).isFalse();
    }

    // --- Percentual do score

    @Test
    void semTarefasFinalizadas() {
        assertThat(CalculadoraScore.percentual(0, 0)).isNull();
    }

    @Test
    void todasNoPrazo() {
        assertThat(CalculadoraScore.percentual(4, 4)).isEqualByComparingTo("100");
    }

    @Test
    void dizimaArredondada() {
        assertThat(CalculadoraScore.percentual(2, 3)).isEqualTo(new BigDecimal("66.67"));
    }

    @Test
    void arredondamentoMeioParaCima() {
        // 1/160 = 0,625%: meio para cima dá 0,63 (meio para o par daria 0,62).
        assertThat(CalculadoraScore.percentual(1, 160)).isEqualTo(new BigDecimal("0.63"));
        assertThat(CalculadoraScore.percentual(1, 6)).isEqualTo(new BigDecimal("16.67"));
    }

    @Test
    void nenhumaNoPrazo() {
        assertThat(CalculadoraScore.percentual(0, 2)).isEqualByComparingTo("0");
    }
}
