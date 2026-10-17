package br.com.demosdd.todo.produtividade;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.todo.produtividade.CalculadoraScore.Conclusao;
import br.com.demosdd.todo.produtividade.dto.ScoreSemanalResponse;
import br.com.demosdd.todo.tarefa.StatusTarefa;
import br.com.demosdd.todo.tarefa.TarefaRepository;

@Service
public class ProdutividadeService {

    private final TarefaRepository repository;
    private final Clock clock;

    public ProdutividadeService(TarefaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScoreSemanalResponse scoreSemanal() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = CalculadoraScore.inicioSemana(hoje);
        LocalDate fim = CalculadoraScore.fimSemana(hoje);
        List<Conclusao> conclusoes = repository
                .findByStatusAndDataConclusaoBetween(StatusTarefa.CONCLUIDA, inicio, fim).stream()
                .map(tarefa -> new Conclusao(tarefa.getDataConclusao(), tarefa.getDataVencimento()))
                .toList();
        long noPrazo = CalculadoraScore.contarNoPrazo(conclusoes);
        return new ScoreSemanalResponse(inicio, fim, conclusoes.size(), noPrazo,
                CalculadoraScore.percentual(noPrazo, conclusoes.size()));
    }
}
