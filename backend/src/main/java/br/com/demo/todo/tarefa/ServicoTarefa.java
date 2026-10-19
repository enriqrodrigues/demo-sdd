package br.com.demo.todo.tarefa;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoTarefa {

    private final TarefaRepository repositorio;

    public ServicoTarefa(TarefaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Tarefa criar(NovaTarefaRequisicao requisicao) {
        Tarefa tarefa = Tarefa.nova(
                requisicao.titulo(),
                requisicao.descricao(),
                requisicao.dataVencimentoConvertida(),
                Prioridade.valueOf(requisicao.prioridade()));
        return repositorio.saveAndFlush(tarefa);
    }

    @Transactional(readOnly = true)
    public Tarefa consultar(UUID id) {
        return repositorio.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }
}
