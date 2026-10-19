package br.com.demo.todo.prazo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;

/**
 * Spec prazo-tarefa › Data corrente no fuso configurado › Fuso configurado diferente. Contexto
 * Spring próprio com {@code app.fuso-horario=UTC}; testes que dependem desse fuso ficam aqui.
 */
@TestPropertySource(properties = "app.fuso-horario=UTC")
class FusoUtcIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @Test
    void fusoConfiguradoDiferente() throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(corpo).get("id").asText();
        relogio.ajustar(Instant.parse("2026-10-21T01:30:00Z"));

        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(jsonPath("$.situacaoPrazo").value("ATRASADA"));
        mvc.perform(get("/api/tarefas").param("situacaoPrazo", "ATRASADA"))
                .andExpect(jsonPath("$[0].id").value(id));
    }
}
