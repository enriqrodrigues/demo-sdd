package br.com.demo.todo.tarefa;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.demo.todo.comum.DataIso;
import br.com.demo.todo.comum.DataIsoValidator;
import br.com.demo.todo.comum.ValorEnum;

/**
 * Corpo de criação de tarefa. O título chega sem os espaços das extremidades e a descrição
 * vazia vira {@code null}. Data e prioridade chegam como texto para que seus erros sejam
 * agregados aos demais. Campos calculados pelo servidor são aceitos e descartados; qualquer
 * outra propriedade desconhecida é rejeitada.
 */
@JsonIgnoreProperties({"id", "status", "concluidaEm", "situacaoPrazo", "version"})
public record NovaTarefaRequisicao(
        @NotBlank(message = "Informe o título.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "Informe a data de vencimento.")
        @DataIso
        String dataVencimento,

        @NotNull(message = "Informe a prioridade.")
        @ValorEnum(value = Prioridade.class, message = "A prioridade deve ser BAIXA, MEDIA ou ALTA.")
        String prioridade) {

    public NovaTarefaRequisicao {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null || descricao.isEmpty() ? null : descricao;
    }

    public LocalDate dataVencimentoConvertida() {
        return DataIsoValidator.converter(dataVencimento);
    }

    public Prioridade prioridadeConvertida() {
        return Prioridade.valueOf(prioridade);
    }
}
