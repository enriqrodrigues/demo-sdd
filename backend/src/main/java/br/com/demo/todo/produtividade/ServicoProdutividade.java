package br.com.demo.todo.produtividade;

import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demo.todo.comum.DataCorrente;
import br.com.demo.todo.tarefa.StatusTarefa;
import br.com.demo.todo.tarefa.Tarefa;
import br.com.demo.todo.tarefa.TarefaRepository;

/** Calcula o score sob demanda, refletindo reaberturas, exclusões e edições de vencimento. */
@Service
public class ServicoProdutividade {

    private final TarefaRepository tarefas;
    private final DataCorrente dataCorrente;

    public ServicoProdutividade(TarefaRepository tarefas, DataCorrente dataCorrente) {
        this.tarefas = tarefas;
        this.dataCorrente = dataCorrente;
    }

    @Transactional(readOnly = true)
    public ScoreSemanal semanaCorrente() {
        ZoneId fuso = dataCorrente.fuso();
        Semana semana = Semana.contendo(dataCorrente.hoje());
        List<Tarefa> concluidas = tarefas.findByStatusAndConcluidaEmGreaterThanEqualAndConcluidaEmLessThan(
                StatusTarefa.CONCLUIDA, semana.inicioEm(fuso), semana.fimExclusivoEm(fuso));
        int noPrazo = (int) concluidas.stream()
                .filter(t -> CalculadoraScore.concluidaNoPrazo(t.getConcluidaEm(), t.getDataVencimento(), fuso))
                .count();
        return new ScoreSemanal(semana.inicio(), semana.fim(), concluidas.size(), noPrazo,
                CalculadoraScore.percentual(noPrazo, concluidas.size()));
    }
}
