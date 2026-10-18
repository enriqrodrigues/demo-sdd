package com.demosdd.todo.compartilhado;

/** Base das exceções de domínio que viram {@code 404} no tratamento global de erros. */
public abstract class RecursoNaoEncontradoException extends RuntimeException {

    protected RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public abstract String getTitulo();
}
