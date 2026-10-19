package br.com.demo.todo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base dos testes de integração: contexto Spring completo contra PostgreSQL 16
 * em container, com a tabela limpa antes de cada teste.
 */
@SpringBootTest
@Import(ContainersConfig.class)
public abstract class TesteIntegracao {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limparTabela() {
        jdbc.execute("delete from tarefa");
    }
}
