package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735"})
    void aceitaCpfComDigitosVerificadoresCorretos(String cpf) {
        assertThat(Cpf.isValido(cpf)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "52998224724",    // DV errado
            "11111111111",    // sequência repetida
            "5299822472",     // 10 dígitos
            "529982247250",   // 12 dígitos
            "529.982.247-25", // com máscara
            "5299822472a"
    })
    void rejeitaCpfInvalido(String cpf) {
        assertThat(Cpf.isValido(cpf)).isFalse();
    }
}
