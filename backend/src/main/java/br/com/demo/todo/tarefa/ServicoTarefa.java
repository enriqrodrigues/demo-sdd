package br.com.demo.todo.tarefa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demo.todo.prazo.SituacaoPrazo;

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
                requisicao.prioridadeConvertida());
        return repositorio.saveAndFlush(tarefa);
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar(StatusTarefa status, SituacaoPrazo situacaoPrazo, LocalDate hoje) {
        return repositorio.findAll(ConsultasTarefa.ordenadaParaListagem()
                .and(ConsultasTarefa.comStatus(status))
                .and(ConsultasTarefa.comSituacaoPrazo(situacaoPrazo, hoje)));
    }

    @Transactional(readOnly = true)
    public Tarefa consultar(UUID id) {
        return repositorio.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }
}
