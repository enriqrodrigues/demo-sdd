package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SenhaTest {

    @ParameterizedTest
    @ValueSource(strings = {"Senha@123", "Abcdef1!", "Çãoção9#X"})
    void aceitaSenhaForte(String senha) {
        assertThat(Senha.isForte(senha)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Sen@1", "senha@123", "SENHA@123", "Senha@abc", "Senha1234"})
    void rejeitaSenhaFraca(String senha) {
        assertThat(Senha.isForte(senha)).isFalse();
    }

    @Test
    void limiteEhContadoEmBytesUtf8() {
        assertThat(Senha.excedeLimite("Aa1!" + "x".repeat(68))).isFalse(); // 72 bytes
        assertThat(Senha.excedeLimite("Aa1!" + "x".repeat(69))).isTrue();  // 73 bytes
        assertThat(Senha.excedeLimite("Aa1!" + "é".repeat(35))).isTrue();  // 74 bytes, 39 caracteres
    }
}
