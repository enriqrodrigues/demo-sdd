package br.com.demosdd.todo.produtividade;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.demosdd.todo.IntegracaoBase;
import br.com.demosdd.todo.tarefa.Prioridade;
import br.com.demosdd.todo.tarefa.Tarefa;
import br.com.demosdd.todo.tarefa.TarefaRepository;

class ProdutividadeApiIntegrationTest extends IntegracaoBase {

    @Autowired
    private TarefaRepository repository;

    private Tarefa concluida(String titulo, LocalDate vencimento, LocalDate conclusao) {
        Tarefa tarefa = new Tarefa(titulo, null, vencimento, Prioridade.MEDIA);
        tarefa.concluir(conclusao);
        return tarefa;
    }

    @Test
    void semConclusoesNaSemanaPercentualENulo() throws Exception {
        repository.save(new Tarefa("Pendente", null, HOJE, Prioridade.ALTA));

        mockMvc.perform(get("/api/produtividade/score-semanal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-12"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-18"))
                .andExpect(jsonPath("$.totalConcluidas").value(0))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(0))
                .andExpect(jsonPath("$.percentualNoPrazo").value(nullValue()));
    }

    @Test
    void calculaScoreSomenteComConclusoesDaSemanaCorrente() throws Exception {
        clock.definirHoje(LocalDate.of(2026, 10, 18)); // domingo, último dia da semana
        repository.saveAll(List.of(
                concluida("Antes do prazo", LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 12)),
                concluida("No dia do prazo", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)),
                concluida("Atrasada", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 18)),
                concluida("Semana passada", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11)),
                new Tarefa("Pendente", null, LocalDate.of(2026, 10, 1), Prioridade.ALTA)));

        mockMvc.perform(get("/api/produtividade/score-semanal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-12"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-18"))
                .andExpect(jsonPath("$.totalConcluidas").value(3))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(2))
                .andExpect(jsonPath("$.percentualNoPrazo").value(66.67));
    }
}
