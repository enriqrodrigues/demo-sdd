package br.com.demo.todo.prazo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Spec prazo-tarefa › Cálculo da situação de prazo; a data corrente é 2026-10-21. */
class CalculadoraPrazoTest {

    private static final LocalDate HOJE = LocalDate.parse("2026-10-21");

    @ParameterizedTest(name = "{0}: concluída={1}, vencendo em {2} → {3}")
    @CsvSource({
            "Pendente com vencimento ontem,         false, 2026-10-20, ATRASADA",
            "Pendente com vencimento hoje,          false, 2026-10-21, PARA_HOJE",
            "Pendente com vencimento amanhã,        false, 2026-10-22, NO_PRAZO",
            "Concluída com vencimento no passado,   true,  2026-10-01, CONCLUIDA",
    })
    void calculaASituacaoDePrazo(String cenario, boolean concluida, LocalDate vencimento, SituacaoPrazo esperada) {
        assertThat(CalculadoraPrazo.calcular(concluida, vencimento, HOJE)).isEqualTo(esperada);
    }
}
