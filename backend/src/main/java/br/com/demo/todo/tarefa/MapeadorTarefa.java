package br.com.demo.todo.tarefa;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import br.com.demo.todo.comum.DataCorrente;
import br.com.demo.todo.prazo.CalculadoraPrazo;

/** Converte a entidade na resposta da API, calculando a situação de prazo para a data corrente. */
@Component
public class MapeadorTarefa {

    private final DataCorrente dataCorrente;

    public MapeadorTarefa(DataCorrente dataCorrente) {
        this.dataCorrente = dataCorrente;
    }

    public TarefaResposta paraResposta(Tarefa tarefa) {
        return paraResposta(tarefa, dataCorrente.hoje());
    }

    /** Recebe {@code hoje} para que todos os itens de uma mesma resposta usem a mesma data. */
    public TarefaResposta paraResposta(Tarefa tarefa, LocalDate hoje) {
        OffsetDateTime concluidaEm = tarefa.getConcluidaEm() == null
                ? null
                : tarefa.getConcluidaEm().atZone(dataCorrente.fuso()).toOffsetDateTime();
        return new TarefaResposta(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getDataVencimento(),
                tarefa.getPrioridade(),
                tarefa.getStatus(),
                concluidaEm,
                CalculadoraPrazo.calcular(tarefa.getStatus() == StatusTarefa.CONCLUIDA,
                        tarefa.getDataVencimento(), hoje),
                tarefa.getVersion());
    }
}
