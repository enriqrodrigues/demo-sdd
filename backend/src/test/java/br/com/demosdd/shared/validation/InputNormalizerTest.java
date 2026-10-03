package br.com.demosdd.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InputNormalizerTest {

    @Test
    void textoSoComEspacosViraNulo() {
        assertThat(InputNormalizer.trimToNull("   ")).isNull();
        assertThat(InputNormalizer.trimToNull("")).isNull();
        assertThat(InputNormalizer.trimToNull(null)).isNull();
        assertThat(InputNormalizer.trimToNull("  Maria  ")).isEqualTo("Maria");
    }

    @Test
    void emailFicaSemEspacosEEmMinusculas() {
        assertThat(InputNormalizer.email(" Maria@Exemplo.com ")).isEqualTo("maria@exemplo.com");
    }

    @Test
    void cpfComMascaraFicaSomenteComDigitos() {
        assertThat(InputNormalizer.cpf("529.982.247-25")).isEqualTo("52998224725");
        assertThat(InputNormalizer.cpf("52998224725")).isEqualTo("52998224725");
    }

    @Test
    void cpfComLetrasMantemAsLetrasParaFalharNaValidacao() {
        assertThat(InputNormalizer.cpf("529.982.247-2a")).isEqualTo("5299822472a");
    }

    @Test
    void telefoneComMascaraFicaSomenteComDigitos() {
        assertThat(InputNormalizer.phone("(11) 98765-4321")).isEqualTo("11987654321");
        assertThat(InputNormalizer.phone("(11) 3456-7890")).isEqualTo("1134567890");
    }

    @Test
    void cepComHifenFicaSomenteComDigitos() {
        assertThat(InputNormalizer.cep("01310-100")).isEqualTo("01310100");
    }

    @Test
    void ufFicaEmMaiusculas() {
        assertThat(InputNormalizer.uf(" sp ")).isEqualTo("SP");
    }
}
