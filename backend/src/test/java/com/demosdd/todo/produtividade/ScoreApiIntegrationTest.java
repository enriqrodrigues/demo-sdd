package com.demosdd.todo.produtividade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.demosdd.todo.RelogioAjustavel;
import com.demosdd.todo.RelogioDeTesteConfiguration;
import com.demosdd.todo.TestcontainersConfiguration;
import com.demosdd.todo.tarefa.Prioridade;
import com.demosdd.todo.tarefa.StatusTarefa;
import com.demosdd.todo.tarefa.Tarefa;
import com.demosdd.todo.tarefa.TarefaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RelogioDeTesteConfiguration.class})
class ScoreApiIntegrationTest {

    /** Quarta-feira: a semana corrente vai de 2026-10-12 a 2026-10-18. */
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 14);

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    RelogioAjustavel relogio;

    @Autowired
    TarefaRepository repository;

    @BeforeEach
    void prepararCenario() {
        repository.deleteAll();
        relogio.definirHoje(HOJE);
    }

    private void tarefaConcluida(String vencimento, String conclusao) {
        Tarefa tarefa = new Tarefa("Tarefa", null, LocalDate.parse(vencimento), Prioridade.MEDIA);
        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.parse(conclusao));
        repository.save(tarefa);
    }

    private void tarefaPendente(String vencimento) {
        repository.save(new Tarefa("Tarefa", null, LocalDate.parse(vencimento), Prioridade.MEDIA));
    }

    private ResultActions consultarScore() throws Exception {
        return mvc.perform(get("/api/produtividade/score")).andExpect(status().isOk());
    }

    @Test
    void scoreSemTarefasConcluidasTemPercentualNulo() throws Exception {
        consultarScore()
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-12"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-18"))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(0))
                .andExpect(jsonPath("$.totalConcluidas").value(0))
                .andExpect(jsonPath("$.percentual").hasJsonPath())
                .andExpect(jsonPath("$.percentual").isEmpty());
    }

    @Test
    void concluidasNoPrazoEComAtraso() throws Exception {
        tarefaConcluida("2026-10-16", "2026-10-13"); // antes do vencimento
        tarefaConcluida("2026-10-14", "2026-10-14"); // no dia do vencimento
        tarefaConcluida("2026-10-10", "2026-10-12"); // depois do vencimento

        consultarScore()
                .andExpect(jsonPath("$.totalConcluidas").value(3))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(2))
                .andExpect(jsonPath("$.percentual").value(66.67));
    }

    @Test
    void pendentesEConcluidasAntesDaSemanaFicamForaDaContagem() throws Exception {
        tarefaPendente("2026-10-01");                 // pendente atrasada
        tarefaPendente("2026-10-20");                 // pendente no prazo
        tarefaConcluida("2026-10-20", "2026-10-11"); // domingo da semana anterior
        tarefaConcluida("2026-10-14", "2026-10-18"); // domingo da semana corrente, com atraso

        consultarScore()
                .andExpect(jsonPath("$.totalConcluidas").value(1))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(0))
                .andExpect(jsonPath("$.percentual").value(0.0));
    }

    @Test
    void tarefaReabertaDeixaDeContar() throws Exception {
        String corpo = mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA"}
                """)).andReturn().getResponse().getContentAsString();
        String id = json.readTree(corpo).get("id").asText();
        String atualizacao = """
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"%s"}
                """;

        mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(atualizacao.formatted("CONCLUIDA"))).andExpect(status().isOk());
        consultarScore()
                .andExpect(jsonPath("$.totalConcluidas").value(1))
                .andExpect(jsonPath("$.percentual").value(100.0));

        mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(atualizacao.formatted("PENDENTE"))).andExpect(status().isOk());
        consultarScore()
                .andExpect(jsonPath("$.totalConcluidas").value(0))
                .andExpect(jsonPath("$.percentual").isEmpty());
    }

    @Test
    void semanaAcompanhaADataAtual() throws Exception {
        tarefaConcluida("2026-10-20", "2026-10-15");

        relogio.definirHoje(LocalDate.of(2026, 10, 19)); // segunda-feira seguinte

        consultarScore()
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-19"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-25"))
                .andExpect(jsonPath("$.totalConcluidas").value(0));
    }
}
