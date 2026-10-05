package br.demo.usuarios.compartilhado;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * {@link Clock} controlável dos testes, sempre em UTC (AD-14).
 */
public class RelogioAjustavel extends Clock {

    private volatile Instant agora;

    public RelogioAjustavel(Instant inicial) {
        this.agora = inicial;
    }

    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    public void definir(Instant instante) {
        agora = instante;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (ZoneOffset.UTC.equals(zone)) {
            return this;
        }
        return new VisaoNoFuso(zone);
    }

    /** Visão em outro fuso que continua lendo o instante atual deste relógio. */
    private final class VisaoNoFuso extends Clock {

        private final ZoneId fuso;

        private VisaoNoFuso(ZoneId fuso) {
            this.fuso = fuso;
        }

        @Override
        public ZoneId getZone() {
            return fuso;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return RelogioAjustavel.this.withZone(zone);
        }

        @Override
        public Instant instant() {
            return RelogioAjustavel.this.instant();
        }
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
