package br.com.demosdd.registration;

import java.util.UUID;

/**
 * Publicado na mesma transação do cadastro. O tratamento é síncrono: se o
 * e-mail de ativação falhar, o cadastro é desfeito.
 */
public record UserRegistered(UUID userId, String email, String name) {
}
