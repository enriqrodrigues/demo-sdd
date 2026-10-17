package br.com.demosdd.todo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

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

    /** Cria uma tarefa via API e devolve o id gerado. */
    protected String criarTarefa(String corpoJson) throws Exception {
        String resposta = mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$.id");
    }
}
