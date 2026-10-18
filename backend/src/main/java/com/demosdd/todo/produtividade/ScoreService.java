package com.demosdd.todo.produtividade;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demosdd.todo.tarefa.TarefaRepository;

@Service
public class ScoreService {

    private final TarefaRepository repository;
    private final Clock clock;

    public ScoreService(TarefaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScoreProdutividade scoreDaSemanaCorrente() {
        Semana semana = Semana.contendo(LocalDate.now(clock));
        long total = repository.contarConcluidasEntre(semana.inicio(), semana.fim());
        long noPrazo = repository.contarConcluidasNoPrazoEntre(semana.inicio(), semana.fim());
        return CalculadoraScore.calcular(semana, noPrazo, total);
    }
}
