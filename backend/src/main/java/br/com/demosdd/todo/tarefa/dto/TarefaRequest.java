package br.com.demosdd.todo.tarefa.dto;

import java.time.LocalDate;

import br.com.demosdd.todo.tarefa.Prioridade;
import br.com.demosdd.todo.tarefa.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada de criação e atualização. Campos somente leitura (id, dataConclusao, situacao)
 * não existem aqui e são ignorados se enviados.
 */
public record TarefaRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String titulo,

        String descricao,

        @NotNull(message = "é obrigatória")
        LocalDate dataVencimento,

        @NotNull(message = "é obrigatória")
        Prioridade prioridade,

        @NotNull(groups = TarefaRequest.Atualizacao.class, message = "é obrigatório")
        StatusTarefa status) {

    /** Grupo de validação do PUT, em que o status é obrigatório. */
    public interface Atualizacao {
    }

    public TarefaRequest {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao;
    }
}
