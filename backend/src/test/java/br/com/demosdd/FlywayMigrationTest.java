package br.com.demosdd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.demosdd.support.IntegrationTest;

class FlywayMigrationTest extends IntegrationTest {

    @Test
    void aplicaMigracaoInicialDoModuloDatabase() {
        String version = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);

        assertThat(version).isEqualTo("1");
    }
}
