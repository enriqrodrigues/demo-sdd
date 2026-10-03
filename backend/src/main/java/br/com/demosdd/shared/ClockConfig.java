package br.com.demosdd.shared;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável: as regras dependentes de tempo (expiração do link de
 * ativação, data de nascimento não futura) usam este bean, o que permite aos
 * testes avançar o tempo sem esperar.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
