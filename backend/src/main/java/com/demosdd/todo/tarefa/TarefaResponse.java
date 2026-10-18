package com.demosdd.todo.tarefa;

import java.time.LocalDate;
import java.util.UUID;

import com.demosdd.todo.prazo.CalculadoraSituacaoPrazo;
import com.demosdd.todo.prazo.SituacaoPrazo;

public record TarefaResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate dataVencimento,
        Prioridade prioridade,
        StatusTarefa status,
        LocalDate dataConclusao,
        SituacaoPrazo situacaoPrazo) {

    public static TarefaResponse de(Tarefa tarefa, LocalDate hoje) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getDataVencimento(),
                tarefa.getPrioridade(),
                tarefa.getStatus(),
                tarefa.getDataConclusao(),
                CalculadoraSituacaoPrazo.calcular(tarefa.getStatus(), tarefa.getDataVencimento(), hoje));
    }
}
