package br.com.demo.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.ControladorFalhaTeste;
import br.com.demo.todo.TesteIntegracao;

/** Spec gestao-tarefas: Representação, Criar tarefa (válida), Consultar tarefa e erros 404/500. */
class CriarConsultarTarefaIT extends TesteIntegracao {

    private static final String CRIACAO_VALIDA = """
            {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA"}
            """;

    @Autowired
    private ObjectMapper json;

    @Test
    void criacaoValida() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CRIACAO_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Pagar conta"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()))
                .andExpect(jsonPath("$.descricao").value(nullValue()))
                .andReturn();

        String id = json.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
        assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/api/tarefas/" + id);
    }

    @Test
    void tarefaPendenteRepresentada() throws Exception {
        String id = criar();

        MvcResult resultado = mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pagar conta"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-25"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()))
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"))
                .andExpect(jsonPath("$.version").isNumber())
                .andReturn();

        Map<String, Object> corpo = json.readValue(resultado.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {
                });
        assertThat(corpo).containsOnlyKeys("id", "titulo", "descricao", "dataVencimento", "prioridade",
                "status", "concluidaEm", "situacaoPrazo", "version");
    }

    @Test
    void tarefaExistente() throws Exception {
        String id = criar();

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void tarefaInexistenteRespondeProblemDetails404() throws Exception {
        mvc.perform(get("/api/tarefas/{id}", "6f1c1a5e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void tarefaInexistenteNaoEcoaOIdentificadorNaMensagem() throws Exception {
        mvc.perform(get("/api/tarefas/{id}", "6f1c1a5e-0000-4000-8000-000000000000"))
                .andExpect(jsonPath("$.detail", not(containsString("6f1c1a5e"))));
    }

    @Test
    void identificadorMalformado() throws Exception {
        mvc.perform(get("/api/tarefas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void erroInesperadoNaoExpoeDetalhesInternos() throws Exception {
        mvc.perform(get(ControladorFalhaTeste.CAMINHO))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("org.postgresql"))))
                .andExpect(content().string(not(containsString("select"))))
                .andExpect(content().string(not(containsString("\tat "))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private String criar() throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CRIACAO_VALIDA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }
}
