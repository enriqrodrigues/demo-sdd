package br.com.demo.todo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio mutável para testes de integração: o instante só muda quando o teste manda. */
public class RelogioTeste extends Clock {

    private volatile Instant agora;

    public RelogioTeste(Instant inicial) {
        this.agora = inicial;
    }

    public void ajustar(Instant instante) {
        this.agora = instante;
    }

    public void avancar(Duration duracao) {
        this.agora = agora.plus(duracao);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Use o ZoneId configurado da aplicação");
    }
}
