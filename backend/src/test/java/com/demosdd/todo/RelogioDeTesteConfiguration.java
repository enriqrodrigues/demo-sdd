package com.demosdd.todo;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Substitui o relógio da aplicação por um {@link RelogioAjustavel} no mesmo fuso. */
@TestConfiguration(proxyBeanMethods = false)
public class RelogioDeTesteConfiguration {

    @Bean
    @Primary
    RelogioAjustavel relogioAjustavel(@Value("${app.fuso}") ZoneId fuso) {
        return new RelogioAjustavel(fuso, Instant.parse("2026-10-18T15:00:00Z"));
    }
}
