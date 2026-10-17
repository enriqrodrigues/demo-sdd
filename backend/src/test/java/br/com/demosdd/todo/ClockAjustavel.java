package br.com.demosdd.todo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Relógio de teste cujo instante pode ser trocado entre os testes. */
public class ClockAjustavel extends Clock {

    private final ZoneId fuso;
    private volatile Instant instante;

    public ClockAjustavel(Instant instante, ZoneId fuso) {
        this.instante = instante;
        this.fuso = fuso;
    }

    public void definirInstante(Instant novoInstante) {
        this.instante = novoInstante;
    }

    /** Ajusta o relógio para o meio-dia da data informada, no fuso do relógio. */
    public void definirHoje(LocalDate hoje) {
        this.instante = hoje.atTime(LocalTime.NOON).atZone(fuso).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return fuso;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
