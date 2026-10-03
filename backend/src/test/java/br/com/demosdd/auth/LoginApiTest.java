package br.com.demosdd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.ResultActions;

import br.com.demosdd.registration.TestPayloads;
import br.com.demosdd.support.IntegrationTest;

/** Cenários de login da spec authentication (RF06, RN04), com banco real. */
class LoginApiTest extends IntegrationTest {

    private static final String EMAIL = "maria@exemplo.com";
    private static final String PASSWORD = "Segura@123";
    private static final String INVALID_MESSAGE = "E-mail ou senha inválidos";

    private void registerPending() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, "529.982.247-25"))
                .andExpect(status().isCreated());
    }

    private void registerActive() throws Exception {
        registerPending();
        markActive(EMAIL);
    }

    private ResultActions login(MockHttpSession session, String email, String password) throws Exception {
        String body = """
                {"email": %s, "password": %s}
                """.formatted(json(email), json(password));
        return mockMvc.perform(post("/api/auth/login").session(session).with(xsrfToken())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static boolean isAuthenticated(MockHttpSession session) {
        Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return context instanceof SecurityContext securityContext && securityContext.getAuthentication() != null;
    }

    // --- Login com credenciais válidas ---

    @Test
    void loginDeContaAtivaIniciaSessaoEDevolveNomeEEmail() throws Exception {
        registerActive();
        MockHttpSession session = new MockHttpSession();

        login(session, EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.email").value(EMAIL));

        assertThat(isAuthenticated(session)).isTrue();
    }

    @Test
    void emailComCaixaDiferenteEEspacosAutentica() throws Exception {
        registerActive();
        MockHttpSession session = new MockHttpSession();

        login(session, " Maria@Exemplo.com ", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        assertThat(isAuthenticated(session)).isTrue();
    }

    // --- Recusa de credenciais inválidas ---

    @Test
    void senhaIncorretaEhRecusadaSemSessao() throws Exception {
        registerActive();
        MockHttpSession session = new MockHttpSession();

        login(session, EMAIL, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value(INVALID_MESSAGE));

        assertThat(isAuthenticated(session)).isFalse();
    }

    @Test
    void emailNaoCadastradoRecebeAMesmaMensagem() throws Exception {
        MockHttpSession session = new MockHttpSession();

        login(session, "ninguem@exemplo.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value(INVALID_MESSAGE));

        assertThat(isAuthenticated(session)).isFalse();
    }

    @Test
    void camposVaziosSaoObrigatoriosENaoAutenticam() throws Exception {
        registerActive();
        MockHttpSession session = new MockHttpSession();

        login(session, "  ", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("password")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("Campo obrigatório")));

        assertThat(isAuthenticated(session)).isFalse();
    }

    @Test
    void senhaVaziaEhObrigatoria() throws Exception {
        registerActive();

        login(new MockHttpSession(), EMAIL, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    // --- Conta pendente não acessa a plataforma (RN04) ---

    @Test
    void contaPendenteComSenhaCorretaEhRecusadaComOrientacao() throws Exception {
        registerPending();
        MockHttpSession session = new MockHttpSession();

        login(session, EMAIL, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING"))
                .andExpect(jsonPath("$.detail").value(
                        "Sua conta ainda não foi ativada. Use o link enviado para o seu e-mail para ativá-la."));

        assertThat(isAuthenticated(session)).isFalse();
    }

    @Test
    void contaPendenteComSenhaErradaRecebeMensagemGenerica() throws Exception {
        registerPending();
        MockHttpSession session = new MockHttpSession();

        login(session, EMAIL, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value(INVALID_MESSAGE));

        assertThat(isAuthenticated(session)).isFalse();
    }

    @Test
    void loginAposAtivacaoPeloLinkAutentica() throws Exception {
        registerPending();
        login(new MockHttpSession(), EMAIL, PASSWORD).andExpect(status().isForbidden());

        activate(lastActivationToken()).andExpect(status().isOk());

        MockHttpSession session = new MockHttpSession();
        login(session, EMAIL, PASSWORD).andExpect(status().isOk());
        assertThat(isAuthenticated(session)).isTrue();
    }
}
