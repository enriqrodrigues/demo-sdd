package br.com.demo.todo.produtividade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Semana ISO-8601: de segunda-feira ({@code inicio}) a domingo ({@code fim}). */
public record Semana(LocalDate inicio, LocalDate fim) {

    public static Semana contendo(LocalDate data) {
        LocalDate segunda = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Semana(segunda, segunda.plusDays(6));
    }
}
