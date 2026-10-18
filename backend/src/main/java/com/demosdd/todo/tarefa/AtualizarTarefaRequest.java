package com.demosdd.todo.tarefa;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do {@code PUT /api/tarefas/{id}}. Não declara {@code id}, {@code dataConclusao}
 * nem {@code situacaoPrazo}: se o cliente os enviar, são ignorados.
 */
public record AtualizarTarefaRequest(
        @NotBlank(message = MensagensValidacao.TITULO_OBRIGATORIO)
        @Size(max = 100, message = MensagensValidacao.TITULO_TAMANHO)
        String titulo,
        String descricao,
        @NotNull(message = MensagensValidacao.VENCIMENTO_OBRIGATORIO)
        LocalDate dataVencimento,
        @NotNull(message = MensagensValidacao.PRIORIDADE_OBRIGATORIA)
        Prioridade prioridade,
        @NotNull(message = MensagensValidacao.STATUS_OBRIGATORIO)
        StatusTarefa status) {

    public AtualizarTarefaRequest {
        // o trim acontece antes da validação, então as regras valem para o título já aparado
        titulo = titulo == null ? null : titulo.strip();
    }
}
