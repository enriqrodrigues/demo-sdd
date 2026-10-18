package com.demosdd.todo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Relógio de teste parado num instante que o teste pode mudar entre as requisições. */
public class RelogioAjustavel extends Clock {

    private final ZoneId fuso;
    private volatile Instant instante;

    public RelogioAjustavel(ZoneId fuso, Instant instante) {
        this.fuso = fuso;
        this.instante = instante;
    }

    /** Define "hoje" como a data informada, ao meio-dia no fuso do relógio. */
    public void definirHoje(LocalDate hoje) {
        this.instante = hoje.atTime(LocalTime.NOON).atZone(fuso).toInstant();
    }

    public void definirInstante(Instant instante) {
        this.instante = instante;
    }

    @Override
    public ZoneId getZone() {
        return fuso;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new RelogioAjustavel(zone, instante);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
