package br.com.demo.todo.prazo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;

/**
 * Spec prazo-tarefa pela API, no fuso padrão (America/Sao_Paulo): Situação de prazo não
 * persistida e Virada de dia em UTC antes do fuso local. O relógio inicia em 2026-10-21.
 */
class SituacaoPrazoApiIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @Test
    void passagemDoTempo() throws Exception {
        String id = criar("2026-10-22");

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"))
                .andExpect(jsonPath("$.version").value(0));

        relogio.avancar(Duration.ofDays(1));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.situacaoPrazo").value("PARA_HOJE"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void situacaoAposTransicoes() throws Exception {
        String id = criar("2026-10-20");

        mvc.perform(post("/api/tarefas/{id}/concluir", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoPrazo").value("CONCLUIDA"));
        mvc.perform(post("/api/tarefas/{id}/reabrir", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoPrazo").value("ATRASADA"));
    }

    @Test
    void situacaoAposEditarOVencimento() throws Exception {
        String id = criar("2026-10-20");

        mvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"));
    }

    @Test
    void viradaDeDiaEmUtcAntesDoFusoLocal() throws Exception {
        String id = criar("2026-10-20");
        relogio.ajustar(Instant.parse("2026-10-21T01:30:00Z"));

        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(jsonPath("$.situacaoPrazo").value("PARA_HOJE"));
    }

    private String criar(String vencimento) throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Pagar conta","dataVencimento":"%s","prioridade":"ALTA"}
                                """.formatted(vencimento)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }
}
