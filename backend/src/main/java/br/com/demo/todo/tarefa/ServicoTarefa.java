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
    public Tarefa criar(TarefaRequisicao requisicao) {
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
        return buscar(id);
    }

    /**
     * Edita só se o cliente conhece a versão atual; a corrida entre esta checagem e o commit é
     * coberta pelo {@code @Version} da entidade.
     */
    @Transactional
    public Tarefa editar(UUID id, TarefaRequisicao requisicao) {
        Tarefa tarefa = buscar(id);
        if (!tarefa.getVersion().equals(requisicao.version())) {
            throw new ConflitoVersaoException();
        }
        tarefa.editar(
                requisicao.titulo(),
                requisicao.descricao(),
                requisicao.dataVencimentoConvertida(),
                requisicao.prioridadeConvertida());
        return repositorio.saveAndFlush(tarefa);
    }

    private Tarefa buscar(UUID id) {
        return repositorio.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }
}
