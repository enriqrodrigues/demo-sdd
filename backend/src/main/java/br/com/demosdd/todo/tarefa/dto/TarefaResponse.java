package br.com.demosdd.todo.tarefa.dto;

import java.time.LocalDate;
import java.util.UUID;

import br.com.demosdd.todo.tarefa.CalculadoraSituacao;
import br.com.demosdd.todo.tarefa.Prioridade;
import br.com.demosdd.todo.tarefa.Situacao;
import br.com.demosdd.todo.tarefa.StatusTarefa;
import br.com.demosdd.todo.tarefa.Tarefa;

public record TarefaResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate dataVencimento,
        Prioridade prioridade,
        StatusTarefa status,
        LocalDate dataConclusao,
        Situacao situacao) {

    public static TarefaResponse de(Tarefa tarefa, LocalDate hoje) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getDataVencimento(),
                tarefa.getPrioridade(),
                tarefa.getStatus(),
                tarefa.getDataConclusao(),
                CalculadoraSituacao.calcular(tarefa.getStatus(), tarefa.getDataVencimento(), hoje));
    }
}
