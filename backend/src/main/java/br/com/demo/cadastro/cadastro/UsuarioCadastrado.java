package br.com.demo.cadastro.cadastro;

import java.util.UUID;

public record UsuarioCadastrado(UUID usuarioId, String nome, String email) {}
