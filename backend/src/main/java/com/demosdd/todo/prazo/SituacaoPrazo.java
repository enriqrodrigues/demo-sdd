package com.demosdd.todo.prazo;

/** Situação do prazo de uma tarefa. É sempre calculada, nunca persistida. */
public enum SituacaoPrazo {
    ATRASADA,
    PARA_HOJE,
    NO_PRAZO,
    CONCLUIDA
}
