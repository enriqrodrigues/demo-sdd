package br.com.demosdd.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @Test
    void aceitaCpfValido() {
        assertThat(validator.isValid("52998224725", null)).isTrue();
        assertThat(validator.isValid("11144477735", null)).isTrue();
    }

    @Test
    void recusaDigitoVerificadorIncorreto() {
        assertThat(validator.isValid("52998224726", null)).isFalse();
        assertThat(validator.isValid("52998224715", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
    void recusaDigitosRepetidos(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"5299822472", "529982247250", "5299822472a", ""})
    void recusaTamanhoDiferenteDe11OuNaoNumerico(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @Test
    void nuloFicaParaOutraValidacao() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
