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
 * Corpo de criação e de edição de tarefa. O título chega sem os espaços das extremidades e a
 * descrição vazia ou só com espaços vira {@code null}. Data e prioridade chegam como texto para que seus erros
 * sejam agregados aos demais. Campos calculados pelo servidor são aceitos e descartados; qualquer
 * outra propriedade desconhecida é rejeitada. {@code version} é obrigatória só na edição e
 * ignorada na criação.
 */
@JsonIgnoreProperties({"id", "status", "concluidaEm", "situacaoPrazo"})
public record TarefaRequisicao(
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
        String prioridade,

        @NotNull(groups = Edicao.class, message = "Informe a versão da tarefa que está sendo editada.")
        Long version) {

    /** Grupo de validação aplicado apenas na edição. */
    public interface Edicao {
    }

    public TarefaRequisicao {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao;
        // Um campo de data limpo no formulário chega como "": é vencimento ausente, não data malformada.
        dataVencimento = dataVencimento == null || dataVencimento.isBlank() ? null : dataVencimento;
    }

    public LocalDate dataVencimentoConvertida() {
        return DataIsoValidator.converter(dataVencimento);
    }

    public Prioridade prioridadeConvertida() {
        return Prioridade.valueOf(prioridade);
    }
}
