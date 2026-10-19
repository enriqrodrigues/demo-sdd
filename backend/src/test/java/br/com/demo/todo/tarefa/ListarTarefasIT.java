package br.com.demo.todo.tarefa;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.demo.todo.TesteIntegracao;

/** Spec gestao-tarefas › Listar tarefas. */
class ListarTarefasIT extends TesteIntegracao {

    @Test
    void listaVazia() throws Exception {
        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ordenacao() throws Exception {
        criar("A", "2026-10-21", "BAIXA");
        criar("B", "2026-10-20", "BAIXA");
        criar("C", "2026-10-21", "ALTA");

        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", contains("B", "C", "A")));
    }

    @Test
    void desempatePorPrioridadeETitulo() throws Exception {
        criar("Zeta", "2026-10-21", "MEDIA");
        criar("Beta", "2026-10-21", "BAIXA");
        criar("Alfa", "2026-10-21", "MEDIA");
        criar("Gama", "2026-10-21", "ALTA");

        mvc.perform(get("/api/tarefas"))
                .andExpect(jsonPath("$[*].titulo", contains("Gama", "Alfa", "Zeta", "Beta")));
    }

    private void criar(String titulo, String vencimento, String prioridade) throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"%s","dataVencimento":"%s","prioridade":"%s"}
                                """.formatted(titulo, vencimento, prioridade)))
                .andExpect(status().isCreated());
    }
}
