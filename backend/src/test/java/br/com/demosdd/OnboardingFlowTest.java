package br.com.demosdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import br.com.demosdd.registration.TestPayloads;
import br.com.demosdd.support.IntegrationTest;

/** Fluxo completo: cadastro, e-mail, link e ativação (RF01 a RF05). */
class OnboardingFlowTest extends IntegrationTest {

    @Test
    void cadastroEmailEAtivacaoDePontaAPonta() throws Exception {
        postJson("/api/registrations", TestPayloads.validRegistration()).andExpect(status().isCreated());
        assertThat(statusOf("maria@exemplo.com")).isEqualTo("PENDENTE");

        String token = lastActivationToken();
        activate(token).andExpect(status().isOk());

        assertThat(statusOf("maria@exemplo.com")).isEqualTo("ATIVO");
    }
}
