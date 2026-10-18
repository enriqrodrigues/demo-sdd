package com.demosdd.todo.tarefa;

import java.util.UUID;

import com.demosdd.todo.compartilhado.RecursoNaoEncontradoException;

public class TarefaNaoEncontradaException extends RecursoNaoEncontradoException {

    public TarefaNaoEncontradaException(UUID id) {
        super("Não existe tarefa com o id " + id + ".");
    }

    @Override
    public String getTitulo() {
        return "Tarefa não encontrada";
    }
}
