package com.demosdd.todo.tarefa;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TarefaService {

    private static final Map<Prioridade, Integer> ORDEM_PRIORIDADE =
            Map.of(Prioridade.ALTA, 0, Prioridade.MEDIA, 1, Prioridade.BAIXA, 2);

    /** Vencimento crescente, depois ALTA, MEDIA, BAIXA e, por fim, o id, para a ordem ser estável. */
    static final Comparator<Tarefa> ORDEM_LISTAGEM = Comparator
            .comparing(Tarefa::getDataVencimento)
            .thenComparing(tarefa -> ORDEM_PRIORIDADE.get(tarefa.getPrioridade()))
            .thenComparing(Tarefa::getId);

    private final TarefaRepository repository;
    private final Clock clock;

    public TarefaService(TarefaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public TarefaResponse criar(CriarTarefaRequest request) {
        Tarefa tarefa = new Tarefa(request.titulo(), request.descricao(), request.dataVencimento(),
                request.prioridade());
        return paraResposta(repository.save(tarefa));
    }

    @Transactional(readOnly = true)
    public List<TarefaResponse> listar() {
        LocalDate hoje = hoje();
        return repository.findAll().stream()
                .sorted(ORDEM_LISTAGEM)
                .map(tarefa -> TarefaResponse.de(tarefa, hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public TarefaResponse buscar(UUID id) {
        return paraResposta(buscarEntidade(id));
    }

    public TarefaResponse atualizar(UUID id, AtualizarTarefaRequest request) {
        Tarefa tarefa = buscarEntidade(id);
        tarefa.alterarDados(request.titulo(), request.descricao(), request.dataVencimento(),
                request.prioridade());
        tarefa.alterarStatus(request.status(), hoje());
        return paraResposta(tarefa);
    }

    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
    }

    private Tarefa buscarEntidade(UUID id) {
        return repository.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }

    private TarefaResponse paraResposta(Tarefa tarefa) {
        return TarefaResponse.de(tarefa, hoje());
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
