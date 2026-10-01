package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.demo.cadastro.shared.NegocioException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TokenAtivacaoTest {

    private static final Instant CRIACAO = Instant.parse("2026-09-30T12:00:00Z");
    private static final Duration VALIDADE = Duration.ofHours(24);

    private TokenAtivacao novoToken() {
        return new TokenAtivacao(UUID.randomUUID(), "hash", CRIACAO, VALIDADE);
    }

    @Test
    void podeSerUsadoDentroDaValidade() {
        assertThatCode(() -> novoToken().usar(CRIACAO.plus(VALIDADE).minusSeconds(1))).doesNotThrowAnyException();
    }

    @Test
    void expiraExatamenteAoFimDaValidade() {
        assertThatThrownBy(() -> novoToken().usar(CRIACAO.plus(VALIDADE)))
                .isInstanceOf(NegocioException.class).hasFieldOrPropertyWithValue("codigo", "TOKEN_EXPIRADO");
    }

    @Test
    void naoPodeSerUsadoDuasVezes() {
        TokenAtivacao token = novoToken();
        token.usar(CRIACAO.plusSeconds(60));

        assertThatThrownBy(() -> token.usar(CRIACAO.plusSeconds(120)))
                .isInstanceOf(NegocioException.class).hasFieldOrPropertyWithValue("codigo", "TOKEN_JA_UTILIZADO");
    }
}
