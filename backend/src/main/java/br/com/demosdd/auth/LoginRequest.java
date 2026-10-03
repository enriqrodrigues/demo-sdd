package br.com.demosdd.auth;

import jakarta.validation.constraints.NotBlank;

import br.com.demosdd.shared.validation.InputNormalizer;

/**
 * Credenciais do login (RF06). O e-mail é comparado sem os espaços nas pontas e
 * em minúsculas, como foi gravado no cadastro; a senha não é alterada (apenas
 * uma senha só com espaços é tratada como vazia).
 */
record LoginRequest(
        @NotBlank(message = REQUIRED)
        String email,

        @NotBlank(message = REQUIRED)
        String password) {

    static final String REQUIRED = "Campo obrigatório";

    LoginRequest {
        email = InputNormalizer.email(email);
        password = password == null || password.isBlank() ? null : password;
    }
}
