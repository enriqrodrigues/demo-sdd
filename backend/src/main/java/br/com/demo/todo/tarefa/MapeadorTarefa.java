package br.com.demo.todo.tarefa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import br.com.demo.todo.comum.DataCorrente;
import br.com.demo.todo.prazo.CalculadoraPrazo;

/** Converte a entidade na resposta da API, calculando a situação de prazo para a data corrente. */
@Component
public class MapeadorTarefa {

    private final DataCorrente dataCorrente;
    private final ZoneId fuso;

    public MapeadorTarefa(DataCorrente dataCorrente, ZoneId fuso) {
        this.dataCorrente = dataCorrente;
        this.fuso = fuso;
    }

    public TarefaResposta paraResposta(Tarefa tarefa) {
        return paraResposta(tarefa, dataCorrente.hoje());
    }

    /** Recebe {@code hoje} para que todos os itens de uma mesma resposta usem a mesma data. */
    public TarefaResposta paraResposta(Tarefa tarefa, LocalDate hoje) {
        OffsetDateTime concluidaEm = tarefa.getConcluidaEm() == null
                ? null
                : tarefa.getConcluidaEm().atZone(fuso).toOffsetDateTime();
        return new TarefaResposta(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getDataVencimento(),
                tarefa.getPrioridade(),
                tarefa.getStatus(),
                concluidaEm,
                CalculadoraPrazo.calcular(tarefa.getStatus(), tarefa.getDataVencimento(), hoje),
                tarefa.getVersion());
    }
}
