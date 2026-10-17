package br.com.demosdd.todo.produtividade;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.demosdd.todo.produtividade.CalculadoraScore.Conclusao;

class CalculadoraScoreTest {

    @Test
    void semanaDeUmaQuartaVaiDeSegundaADomingo() {
        LocalDate quarta = LocalDate.of(2026, 10, 14);
        assertThat(CalculadoraScore.inicioSemana(quarta)).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(CalculadoraScore.fimSemana(quarta)).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void segundaFeiraIniciaAPropriaSemana() {
        LocalDate segunda = LocalDate.of(2026, 10, 12);
        assertThat(CalculadoraScore.inicioSemana(segunda)).isEqualTo(segunda);
        assertThat(CalculadoraScore.fimSemana(segunda)).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void domingoEncerraAPropriaSemana() {
        LocalDate domingo = LocalDate.of(2026, 10, 18);
        assertThat(CalculadoraScore.inicioSemana(domingo)).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(CalculadoraScore.fimSemana(domingo)).isEqualTo(domingo);
    }

    @Test
    void conclusaoNoDiaDoVencimentoContaNoPrazo() {
        LocalDate dia = LocalDate.of(2026, 10, 14);
        assertThat(CalculadoraScore.contarNoPrazo(List.of(new Conclusao(dia, dia)))).isEqualTo(1);
    }

    @Test
    void conclusaoUmDiaDepoisDoVencimentoNaoContaNoPrazo() {
        LocalDate vencimento = LocalDate.of(2026, 10, 14);
        assertThat(CalculadoraScore.contarNoPrazo(List.of(new Conclusao(vencimento.plusDays(1), vencimento))))
                .isZero();
    }

    @Test
    void percentualSemConclusoesENulo() {
        assertThat(CalculadoraScore.percentual(0, 0)).isNull();
    }

    @Test
    void percentualArredondaParaDuasCasasHalfUp() {
        assertThat(CalculadoraScore.percentual(2, 3)).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(CalculadoraScore.percentual(1, 3)).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    void percentualTemSempreDuasCasas() {
        assertThat(CalculadoraScore.percentual(4, 5)).isEqualTo(new BigDecimal("80.00"));
        assertThat(CalculadoraScore.percentual(3, 3)).isEqualTo(new BigDecimal("100.00"));
        assertThat(CalculadoraScore.percentual(0, 2)).isEqualTo(new BigDecimal("0.00"));
    }
}
