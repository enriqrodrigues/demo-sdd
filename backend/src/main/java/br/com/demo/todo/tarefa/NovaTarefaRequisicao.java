package br.com.demo.todo.tarefa;

import java.time.LocalDate;

/** Corpo de criação de tarefa. */
public record NovaTarefaRequisicao(String titulo, String descricao, LocalDate dataVencimento, String prioridade) {
}
