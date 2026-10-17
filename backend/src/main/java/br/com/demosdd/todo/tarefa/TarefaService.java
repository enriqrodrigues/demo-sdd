package br.com.demosdd.todo.tarefa;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.todo.comum.RecursoNaoEncontradoException;
import br.com.demosdd.todo.tarefa.dto.TarefaRequest;

@Service
@Transactional
public class TarefaService {

    private final TarefaRepository repository;
    private final Clock clock;

    public TarefaService(TarefaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar() {
        return repository.listarOrdenado();
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada: " + id));
    }

    public Tarefa criar(TarefaRequest dados) {
        Tarefa tarefa = new Tarefa(dados.titulo(), dados.descricao(), dados.dataVencimento(), dados.prioridade());
        if (dados.status() == StatusTarefa.CONCLUIDA) {
            tarefa.concluir(hoje());
        }
        return repository.save(tarefa);
    }

    public void excluir(UUID id) {
        repository.delete(buscar(id));
    }
}
