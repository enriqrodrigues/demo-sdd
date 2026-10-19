package br.com.demo.todo.produtividade;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Score da semana corrente; {@code percentual} é {@code null} quando nenhuma tarefa foi concluída. */
public record ScoreSemanal(
        LocalDate inicioSemana,
        LocalDate fimSemana,
        int concluidas,
        int noPrazo,
        BigDecimal percentual) {
}
