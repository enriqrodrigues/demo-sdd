package br.com.demosdd.shared.web;

/** Erro de um campo da requisição, exposto em {@code errors[]}. */
public record FieldErrorResponse(String field, String message) {
}
