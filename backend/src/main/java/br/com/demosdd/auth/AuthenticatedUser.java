package br.com.demosdd.auth;

import java.io.Serializable;
import java.util.UUID;

/**
 * Principal guardado na sessão após o login (design D2). Os dados exibidos na
 * interface vêm sempre do banco, pelo {@code id}.
 */
public record AuthenticatedUser(UUID id, String name, String email) implements Serializable {
}
