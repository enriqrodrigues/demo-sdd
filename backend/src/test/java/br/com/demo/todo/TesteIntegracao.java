package br.com.demo.todo;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base dos testes de integração: contexto Spring completo contra PostgreSQL 16
 * em container, relógio mutável e tabela limpa antes de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ContainersConfig.class, TesteIntegracao.RelogioConfig.class})
public abstract class TesteIntegracao {

    /** Quarta-feira, 2026-10-21, 09:00 em America/Sao_Paulo. */
    protected static final Instant INSTANTE_PADRAO = Instant.parse("2026-10-21T12:00:00Z");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelogioTeste relogio;

    @BeforeEach
    void prepararEstado() {
        jdbc.execute("delete from tarefa");
        relogio.ajustar(INSTANTE_PADRAO);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioConfig {

        @Bean
        @Primary
        RelogioTeste relogioTeste() {
            return new RelogioTeste(INSTANTE_PADRAO);
        }
    }
}
