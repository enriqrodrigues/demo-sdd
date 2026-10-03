package br.com.demosdd.activation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ActivationTokensTest {

    @Test
    void geraTokensDistintosEUrlSafe() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = ActivationTokens.generate();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            generated.add(token);
        }
        assertThat(generated).hasSize(1000);
    }

    @Test
    void hashEhDeterministicoESha256Hex() {
        String token = ActivationTokens.generate();

        assertThat(ActivationTokens.hash(token)).isEqualTo(ActivationTokens.hash(token));
        assertThat(ActivationTokens.hash(token)).hasSize(64).matches("[0-9a-f]+");
        assertThat(ActivationTokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hashNaoContemOTokenPuro() {
        String token = ActivationTokens.generate();

        assertThat(ActivationTokens.hash(token)).isNotEqualTo(token).doesNotContain(token);
    }
}
