package br.com.demo.todo.tarefa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;

/** Spec gestao-tarefas: Excluir tarefa e Concluir tarefa › Concluir tarefa excluída em paralelo. */
class ExcluirTarefaIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @Test
    void excluirTarefaExistente() throws Exception {
        String id = criar();

        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void excluirTarefaInexistente() throws Exception {
        mvc.perform(delete("/api/tarefas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void editarTarefaExcluida() throws Exception {
        String id = criar();
        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void concluirTarefaExcluidaEmParalelo() throws Exception {
        String id = criar();
        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(post("/api/tarefas/{id}/concluir", id)).andExpect(status().isNotFound());
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
}
