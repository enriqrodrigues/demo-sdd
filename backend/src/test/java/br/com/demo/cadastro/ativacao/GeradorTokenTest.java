package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeradorTokenTest {

    private final GeradorToken gerador = new GeradorToken();

    @Test
    void geraTokenBase64UrlDe32BytesSemPadding() {
        String token = gerador.gerar();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(gerador.gerar()).isNotEqualTo(token);
    }

    @Test
    void hashEhSha256EmHexadecimal() {
        assertThat(GeradorToken.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
