package br.com.demosdd.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void aceitaSenhaForte() {
        assertThat(PasswordPolicy.unmetCriteria("Segura@123")).isEmpty();
    }

    @Test
    void recusaSenhaSemCaractereEspecial() {
        assertThat(PasswordPolicy.unmetCriteria("Segura1234"))
                .containsExactly("A senha deve conter ao menos um caractere especial");
    }

    @Test
    void recusaSenhaCurta() {
        assertThat(PasswordPolicy.unmetCriteria("Se@1"))
                .containsExactly("A senha deve ter ao menos 8 caracteres");
    }

    @Test
    void recusaSenhaLonga() {
        String senha = "Aa1@" + "x".repeat(61);
        assertThat(PasswordPolicy.unmetCriteria(senha))
                .containsExactly("A senha deve ter no máximo 64 caracteres");
    }

    @Test
    void aceitaSenhaComExatamente64Caracteres() {
        String senha = "Aa1@" + "x".repeat(60);
        assertThat(PasswordPolicy.unmetCriteria(senha)).isEmpty();
    }

    @Test
    void indicaCadaCriterioNaoAtendido() {
        assertThat(PasswordPolicy.unmetCriteria("abcdefgh")).containsExactly(
                "A senha deve conter ao menos uma letra maiúscula",
                "A senha deve conter ao menos um dígito",
                "A senha deve conter ao menos um caractere especial");
        assertThat(PasswordPolicy.unmetCriteria("ABCDEF1@"))
                .containsExactly("A senha deve conter ao menos uma letra minúscula");
    }

    @Test
    void espacoContaComoCaractereEspecial() {
        assertThat(PasswordPolicy.unmetCriteria("Segura 123")).isEmpty();
    }
}
