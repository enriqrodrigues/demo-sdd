package br.com.demosdd.todo;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegracaoBase {

    /** Quarta-feira; a semana corrente vai de 2026-10-12 a 2026-10-18. */
    protected static final LocalDate HOJE = LocalDate.of(2026, 10, 14);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ClockAjustavel clock;

    @BeforeEach
    void prepararBase() {
        jdbc.update("DELETE FROM tarefa");
        clock.definirHoje(HOJE);
    }
}
