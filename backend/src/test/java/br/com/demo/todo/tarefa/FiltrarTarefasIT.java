package br.com.demo.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;
import br.com.demo.todo.prazo.SituacaoPrazo;

/**
 * Spec gestao-tarefas › Filtrar a listagem, e o risco do design de os predicados do filtro
 * divergirem da CalculadoraPrazo. A data corrente padrão é 2026-10-21 em America/Sao_Paulo.
 */
class FiltrarTarefasIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void criarTarefasNosQuatroCasosDePrazo() throws Exception {
        criar("Atrasada", "2026-10-20");
        criar("Para hoje", "2026-10-21");
        criar("No prazo", "2026-10-22");
        inserirConcluida("Concluída", "2026-10-01");
    }

    @Test
    void filtroPorStatus() throws Exception {
        mvc.perform(get("/api/tarefas").param("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", containsInAnyOrder("Atrasada", "Para hoje", "No prazo")));
    }

    @Test
    void filtroPorSituacaoDePrazo() throws Exception {
        mvc.perform(get("/api/tarefas").param("situacaoPrazo", "ATRASADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Atrasada"))
                .andExpect(jsonPath("$[0].situacaoPrazo").value("ATRASADA"));
    }

    @Test
    void filtroInvalido() throws Exception {
        mvc.perform(get("/api/tarefas").param("situacaoPrazo", "VENCIDA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("situacaoPrazo")));
    }

    @Test
    void statusInvalido() throws Exception {
        mvc.perform(get("/api/tarefas").param("status", "pendente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("status")));
    }

    @Test
    void filtrosSaoCombinaveis() throws Exception {
        mvc.perform(get("/api/tarefas").param("status", "CONCLUIDA").param("situacaoPrazo", "ATRASADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/tarefas").param("status", "PENDENTE").param("situacaoPrazo", "PARA_HOJE"))
                .andExpect(jsonPath("$[*].titulo", containsInAnyOrder("Para hoje")));
    }

    @Test
    void cadaFiltroDeSituacaoCoincideComASituacaoCalculada() throws Exception {
        conferirFiltrosContraSituacaoCalculada();
    }

    @Test
    void filtroCoincideComASituacaoCalculadaNaViradaDeDiaUtc() throws Exception {
        // 22:30 de 2026-10-21 em São Paulo, já 2026-10-22 em UTC.
        relogio.ajustar(Instant.parse("2026-10-22T01:30:00Z"));

        conferirFiltrosContraSituacaoCalculada();
        mvc.perform(get("/api/tarefas").param("situacaoPrazo", "PARA_HOJE"))
                .andExpect(jsonPath("$[*].titulo", containsInAnyOrder("Para hoje")));
    }

    private void conferirFiltrosContraSituacaoCalculada() throws Exception {
        JsonNode todas = listar("/api/tarefas");
        for (SituacaoPrazo situacao : SituacaoPrazo.values()) {
            List<String> esperadas = new ArrayList<>();
            todas.forEach(t -> {
                if (t.get("situacaoPrazo").asText().equals(situacao.name())) {
                    esperadas.add(t.get("id").asText());
                }
            });
            List<String> filtradas = new ArrayList<>();
            listar("/api/tarefas?situacaoPrazo=" + situacao).forEach(t -> filtradas.add(t.get("id").asText()));

            assertThat(filtradas).as("filtro %s", situacao).containsExactlyInAnyOrderElementsOf(esperadas);
            assertThat(esperadas).as("cada caso de prazo tem uma tarefa (%s)", situacao).hasSize(1);
        }
    }

    private JsonNode listar(String url) throws Exception {
        String corpo = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(corpo);
    }

    private void criar(String titulo, String vencimento) throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"%s","dataVencimento":"%s","prioridade":"MEDIA"}
                                """.formatted(titulo, vencimento)))
                .andExpect(status().isCreated());
    }

    private void inserirConcluida(String titulo, String vencimento) {
        jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, concluida_em, version)
                values (?, ?, cast(? as date), 'MEDIA', 'CONCLUIDA', now(), 1)
                """, UUID.randomUUID(), titulo, vencimento);
    }
}
