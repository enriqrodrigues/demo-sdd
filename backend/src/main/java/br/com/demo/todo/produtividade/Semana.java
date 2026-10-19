package br.com.demo.todo.produtividade;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** Semana ISO-8601: de segunda-feira ({@code inicio}) a domingo ({@code fim}). */
public record Semana(LocalDate inicio, LocalDate fim) {

    public static Semana contendo(LocalDate data) {
        LocalDate segunda = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Semana(segunda, segunda.plusDays(6));
    }

    /** Segunda-feira 00:00 no fuso, início (inclusivo) do intervalo da semana. */
    public Instant inicioEm(ZoneId fuso) {
        return inicio.atStartOfDay(fuso).toInstant();
    }

    /** Segunda-feira seguinte 00:00 no fuso, fim (exclusivo) do intervalo da semana. */
    public Instant fimExclusivoEm(ZoneId fuso) {
        return fim.plusDays(1).atStartOfDay(fuso).toInstant();
    }
}
