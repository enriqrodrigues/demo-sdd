package br.com.demo.cadastro;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.demo.cadastro.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationsTest extends IntegrationTest {

    @Test
    void aplicaTodasAsMigrationsEmBancoVazio() {
        List<String> versoes = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(versoes).containsExactly("1", "2", "3");

        List<String> tabelas = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tabelas).contains("usuario", "token_ativacao", "event_publication");
    }
}
