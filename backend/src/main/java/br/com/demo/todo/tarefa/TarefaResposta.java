package br.com.demo.todo.tarefa;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.demo.todo.prazo.SituacaoPrazo;

/** Representação da tarefa na API; {@code situacaoPrazo} é calculada a cada leitura. */
public record TarefaResposta(
        UUID id,
        String titulo,
        String descricao,
        LocalDate dataVencimento,
        Prioridade prioridade,
        StatusTarefa status,
        OffsetDateTime concluidaEm,
        SituacaoPrazo situacaoPrazo,
        long version) {
}
