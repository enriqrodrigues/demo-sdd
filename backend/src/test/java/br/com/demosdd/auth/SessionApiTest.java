package br.com.demosdd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import br.com.demosdd.registration.TestPayloads;
import br.com.demosdd.support.IntegrationTest;

/**
 * Sessão do usuário (spec authentication): dados do usuário autenticado,
 * recursos internos, logout, expiração e renovação do id de sessão.
 */
class SessionApiTest extends IntegrationTest {

    private static final String EMAIL = "maria@exemplo.com";

    private MockHttpSession loggedIn() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, "529.982.247-25"))
                .andExpect(status().isCreated());
        markActive(EMAIL);
        MockHttpSession session = new MockHttpSession();
        login(session).andExpect(status().isOk());
        return session;
    }

    private ResultActions login(MockHttpSession session) throws Exception {
        return mockMvc.perform(post("/api/auth/login").session(session).with(xsrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + EMAIL + "\", \"password\": \"Segura@123\"}"));
    }

    private ResultActions me(MockHttpSession session) throws Exception {
        return mockMvc.perform(session == null ? get("/api/auth/me") : get("/api/auth/me").session(session));
    }

    // --- Dados do usuário autenticado ---

    @Test
    void meComSessaoDevolveNomeEEmailDoBanco() throws Exception {
        MockHttpSession session = loggedIn();
        jdbc.update("UPDATE users SET name = 'Maria Souza' WHERE email = ?", EMAIL);

        me(session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    // --- Recursos internos exigem autenticação ---

    @Test
    void meSemSessaoEhRecusadoComoNaoAutenticado() throws Exception {
        me(null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void recursosPublicosContinuamAcessiveisSemSessao() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, "529.982.247-25"))
                .andExpect(status().isCreated());
        activate(lastActivationToken()).andExpect(status().isOk());
        postJson("/api/auth/login", "{\"email\": \"" + EMAIL + "\", \"password\": \"Segura@123\"}")
                .andExpect(status().isOk());
    }

    // --- Expiração da sessão por inatividade ---

    @Test
    void sessaoExpiradaEhTratadaComoNaoAutenticada() throws Exception {
        MockHttpSession session = loggedIn();
        me(session).andExpect(status().isOk());

        session.invalidate(); // o que o container faz após 30 minutos sem requisições

        me(session)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // --- Logout ---

    @Test
    void depoisDoLogoutASessaoNaoAcessaMaisRecursosInternos() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(post("/api/auth/logout").session(session).with(xsrfToken()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        me(session).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSemTokenAntiCsrfEhRecusado() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        me(session).andExpect(status().isOk());
    }

    // --- Proteção do identificador de sessão ---

    @Test
    void idDeSessaoMudaNoLogin() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, "529.982.247-25"))
                .andExpect(status().isCreated());
        markActive(EMAIL);
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        login(session).andExpect(status().isOk());

        assertThat(session.getId()).isNotEqualTo(before);
    }
}
