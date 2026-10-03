package br.com.demosdd.support;

import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

/** Postgres real (Testcontainers) e relógio controlável para os testes de integração. */
@TestConfiguration(proxyBeanMethods = false)
public class TestInfrastructureConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(Instant.now(), ZoneId.of("America/Sao_Paulo"));
    }
}
