package com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TarefaTest {

    private static Tarefa novaTarefa() {
        return new Tarefa("Pagar conta", "Luz", LocalDate.of(2026, 10, 20), Prioridade.ALTA);
    }

    @Test
    void novaTarefaComecaPendenteSemDataDeConclusao() {
        Tarefa tarefa = novaTarefa();

        assertThat(tarefa.getId()).isNotNull();
        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void concluirTarefaDefineDataDeConclusaoComoHoje() {
        Tarefa tarefa = novaTarefa();

        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 18));

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(tarefa.getDataConclusao()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void editarTarefaJaConcluidaPreservaDataDeConclusao() {
        Tarefa tarefa = novaTarefa();
        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 15));

        tarefa.alterarDados("Pagar conta de luz", "Luz", LocalDate.of(2026, 10, 20), Prioridade.ALTA);
        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 18));

        assertThat(tarefa.getTitulo()).isEqualTo("Pagar conta de luz");
        assertThat(tarefa.getDataConclusao()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void reabrirTarefaApagaDataDeConclusao() {
        Tarefa tarefa = novaTarefa();
        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 15));

        tarefa.alterarStatus(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 18));

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void concluirNovamenteAposReabrirUsaANovaData() {
        Tarefa tarefa = novaTarefa();
        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 15));
        tarefa.alterarStatus(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 16));

        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 18));

        assertThat(tarefa.getDataConclusao()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void manterPendenteContinuaSemDataDeConclusao() {
        Tarefa tarefa = novaTarefa();

        tarefa.alterarStatus(StatusTarefa.PENDENTE, LocalDate.of(2026, 10, 18));

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }
}
