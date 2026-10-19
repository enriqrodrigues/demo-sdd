package br.com.demo.todo.tarefa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;

/**
 * Spec gestao-tarefas: Representação (tarefa concluída), Concluir tarefa, Reabrir tarefa e
 * Concorrência otimista › Editar enquanto outra ação conclui. Relógio inicia em
 * 2026-10-21T12:00Z (09:00 em São Paulo).
 */
class ConcluirReabrirTarefaIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @Test
    void tarefaConcluidaRepresentada() throws Exception {
        String id = criar();
        concluir(id);

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-21T09:00:00-03:00"));
    }

    // --- Concluir tarefa

    @Test
    void concluirTarefaPendente() throws Exception {
        String id = criar();

        concluir(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-21T09:00:00-03:00"))
                .andExpect(jsonPath("$.situacaoPrazo").value("CONCLUIDA"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void concluirDuasVezes() throws Exception {
        String id = criar();
        concluir(id).andExpect(status().isOk());
        relogio.avancar(Duration.ofHours(2));

        concluir(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-21T09:00:00-03:00"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void concluirTarefaInexistente() throws Exception {
        concluir(UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void instanteDeConclusaoComFracaoDeSegundoSobreviveARelida() throws Exception {
        relogio.ajustar(Instant.parse("2026-10-21T12:00:00.123456789Z"));
        String id = criar();

        concluir(id).andExpect(jsonPath("$.concluidaEm").value("2026-10-21T09:00:00.123456-03:00"));
        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-21T09:00:00.123456-03:00"));
    }

    // --- Reabrir tarefa

    @Test
    void reabrirTarefaConcluida() throws Exception {
        String id = criar();
        concluir(id);

        reabrir(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void reabrirTarefaPendente() throws Exception {
        String id = criar();

        reabrir(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void concluirDeNovoAposReabrir() throws Exception {
        String id = criar();
        concluir(id);
        reabrir(id);
        relogio.avancar(Duration.ofDays(1));

        concluir(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-22T09:00:00-03:00"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void reabrirTarefaInexistente() throws Exception {
        reabrir(UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // --- Concorrência otimista

    @Test
    void editarEnquantoOutraAcaoConclui() throws Exception {
        String id = criar();
        // Cliente A leu a tarefa na versão 0; cliente B conclui (versão 1).
        concluir(id).andExpect(jsonPath("$.version").value(1));

        mvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Editado por A","dataVencimento":"2026-10-30","prioridade":"BAIXA","version":0}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.titulo").value("Pagar conta"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-25"))
                .andExpect(jsonPath("$.version").value(1));
    }

    private String criar() throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }

    private ResultActions concluir(String id) throws Exception {
        return mvc.perform(post("/api/tarefas/{id}/concluir", id));
    }

    private ResultActions reabrir(String id) throws Exception {
        return mvc.perform(post("/api/tarefas/{id}/reabrir", id));
    }
}
