package br.com.demo.todo.comum;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

/** Única forma de obter a data corrente de negócio: o instante do {@link Clock} no fuso configurado. */
@Component
public class DataCorrente {

    private final Clock clock;
    private final ZoneId fuso;

    public DataCorrente(Clock clock, ZoneId fuso) {
        this.clock = clock;
        this.fuso = fuso;
    }

    public LocalDate hoje() {
        return LocalDate.ofInstant(clock.instant(), fuso);
    }

    /** Fuso configurado, para converter outros instantes em datas locais da mesma forma. */
    public ZoneId fuso() {
        return fuso;
    }
}
