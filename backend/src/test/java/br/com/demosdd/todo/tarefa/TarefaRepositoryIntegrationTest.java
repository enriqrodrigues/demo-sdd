package br.com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.demosdd.todo.IntegracaoBase;

class TarefaRepositoryIntegrationTest extends IntegracaoBase {

    @Autowired
    private TarefaRepository repository;

    @Test
    void persisteERecuperaTodosOsCampos() {
        Tarefa tarefa = new Tarefa("Estudar", "Capítulo 3 inteiro", LocalDate.of(2026, 10, 20), Prioridade.MEDIA);
        tarefa.concluir(HOJE);
        repository.save(tarefa);

        Tarefa lida = repository.findById(tarefa.getId()).orElseThrow();

        assertThat(lida.getTitulo()).isEqualTo("Estudar");
        assertThat(lida.getDescricao()).isEqualTo("Capítulo 3 inteiro");
        assertThat(lida.getDataVencimento()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(lida.getPrioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(lida.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(lida.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void listaPendentesPorVencimentoETituloDepoisConcluidas() {
        Tarefa concluida = new Tarefa("Concluída antiga", null, LocalDate.of(2026, 10, 1), Prioridade.BAIXA);
        concluida.concluir(LocalDate.of(2026, 10, 2));
        Tarefa futura = new Tarefa("Futura", null, LocalDate.of(2026, 10, 20), Prioridade.ALTA);
        Tarefa hojeB = new Tarefa("B hoje", null, HOJE, Prioridade.MEDIA);
        Tarefa hojeA = new Tarefa("A hoje", null, HOJE, Prioridade.MEDIA);
        Tarefa atrasada = new Tarefa("Atrasada", null, LocalDate.of(2026, 10, 10), Prioridade.ALTA);
        repository.saveAll(List.of(concluida, futura, hojeB, hojeA, atrasada));

        assertThat(repository.listarOrdenado())
                .extracting(Tarefa::getTitulo)
                .containsExactly("Atrasada", "A hoje", "B hoje", "Futura", "Concluída antiga");
    }

    @Test
    void buscaConcluidasNoIntervaloInclusivo() {
        Tarefa domingoAnterior = concluidaEm("Domingo anterior", LocalDate.of(2026, 10, 11));
        Tarefa segunda = concluidaEm("Segunda", LocalDate.of(2026, 10, 12));
        Tarefa domingo = concluidaEm("Domingo", LocalDate.of(2026, 10, 18));
        Tarefa segundaSeguinte = concluidaEm("Segunda seguinte", LocalDate.of(2026, 10, 19));
        Tarefa pendente = new Tarefa("Pendente", null, HOJE, Prioridade.BAIXA);
        repository.saveAll(List.of(domingoAnterior, segunda, domingo, segundaSeguinte, pendente));

        List<Tarefa> encontradas = repository.findByStatusAndDataConclusaoBetween(
                StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));

        assertThat(encontradas).extracting(Tarefa::getTitulo).containsExactlyInAnyOrder("Segunda", "Domingo");
    }

    private Tarefa concluidaEm(String titulo, LocalDate dataConclusao) {
        Tarefa tarefa = new Tarefa(titulo, null, dataConclusao, Prioridade.MEDIA);
        tarefa.concluir(dataConclusao);
        return tarefa;
    }
}
