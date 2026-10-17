package br.com.demosdd.todo;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import br.com.demosdd.todo.comum.ClockConfig;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    @Bean
    @Primary
    ClockAjustavel clockAjustavel() {
        return new ClockAjustavel(Instant.parse("2026-10-14T15:00:00Z"), ClockConfig.FUSO);
    }
}
