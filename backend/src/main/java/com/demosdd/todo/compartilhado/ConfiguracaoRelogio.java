package com.demosdd.todo.compartilhado;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Única fonte de "data atual" da aplicação: todo código que precisa de hoje usa
 * {@code LocalDate.now(clock)} com este relógio, no fuso de {@code app.fuso}.
 */
@Configuration
public class ConfiguracaoRelogio {

    @Bean
    public Clock clock(@Value("${app.fuso}") ZoneId fuso) {
        return Clock.system(fuso);
    }
}
