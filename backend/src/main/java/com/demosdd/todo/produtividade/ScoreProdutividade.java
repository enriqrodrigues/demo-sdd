package com.demosdd.todo.produtividade;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta de {@code GET /api/produtividade/score}; {@code percentual} é nulo sem tarefas concluídas. */
public record ScoreProdutividade(
        LocalDate inicioSemana,
        LocalDate fimSemana,
        long concluidasNoPrazo,
        long totalConcluidas,
        BigDecimal percentual) {
}
