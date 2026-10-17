package br.com.demosdd.todo.comum;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Fonte única da data de hoje. Os testes substituem por um relógio fixo. */
@Configuration
public class ClockConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(FUSO);
    }
}
