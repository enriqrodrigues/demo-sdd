package com.demosdd.todo.produtividade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Semana de segunda a domingo, inclusive. */
public record Semana(LocalDate inicio, LocalDate fim) {

    public static Semana contendo(LocalDate data) {
        LocalDate segunda = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Semana(segunda, segunda.plusDays(6));
    }
}
