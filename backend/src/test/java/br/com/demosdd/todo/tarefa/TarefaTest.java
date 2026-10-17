package br.com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TarefaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 16);

    private Tarefa novaTarefa() {
        return new Tarefa("Pagar boleto", "Conta de luz", HOJE.plusDays(2), Prioridade.ALTA);
    }

    @Test
    void novaTarefaComecaPendenteSemDataConclusao() {
        Tarefa tarefa = novaTarefa();

        assertThat(tarefa.getId()).isNotNull();
        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void concluirGravaDataDeConclusao() {
        Tarefa tarefa = novaTarefa();

        tarefa.concluir(HOJE);

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void concluirTarefaJaConcluidaMantemDataOriginal() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.concluir(HOJE.plusDays(3));

        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void reabrirLimpaDataDeConclusao() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.reabrir();

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void alterarStatusParaConcluidaConclui() {
        Tarefa tarefa = novaTarefa();

        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, HOJE);

        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void alterarStatusParaPendenteReabre() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.alterarStatus(StatusTarefa.PENDENTE, HOJE);

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void atualizarDadosNaoMexeNoStatus() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.atualizarDados("Outro título", null, HOJE.plusDays(5), Prioridade.BAIXA);

        assertThat(tarefa.getTitulo()).isEqualTo("Outro título");
        assertThat(tarefa.getDescricao()).isNull();
        assertThat(tarefa.getDataVencimento()).isEqualTo(HOJE.plusDays(5));
        assertThat(tarefa.getPrioridade()).isEqualTo(Prioridade.BAIXA);
        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }
}
