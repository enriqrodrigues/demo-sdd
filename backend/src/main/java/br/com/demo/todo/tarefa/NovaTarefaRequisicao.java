package br.com.demo.todo.tarefa;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.demo.todo.comum.DataIso;
import br.com.demo.todo.comum.DataIsoValidator;

/**
 * Corpo de criação de tarefa. O título chega sem os espaços das extremidades e a descrição
 * vazia vira {@code null}; a data chega como texto para que seus erros sejam agregados aos demais.
 */
public record NovaTarefaRequisicao(
        @NotBlank(message = "Informe o título.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "Informe a data de vencimento.")
        @DataIso
        String dataVencimento,

        String prioridade) {

    public NovaTarefaRequisicao {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null || descricao.isEmpty() ? null : descricao;
    }

    public LocalDate dataVencimentoConvertida() {
        return DataIsoValidator.converter(dataVencimento);
    }
}
