package br.com.demosdd.todo.produtividade.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScoreSemanalResponse(
        LocalDate inicioSemana,
        LocalDate fimSemana,
        long totalConcluidas,
        long concluidasNoPrazo,
        BigDecimal percentualNoPrazo) {
}
