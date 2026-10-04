package br.com.demosdd.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demosdd.registration.TestPayloads;
import br.com.demosdd.support.IntegrationTest;

/**
 * Perfil do usuário (spec user-profile): consulta, edição de telefone e
 * endereço, validação, imutabilidade dos dados críticos (RN01) e isolamento.
 */
class ProfileApiTest extends IntegrationTest {

    private static final String MARIA = "maria@exemplo.com";
    private static final String JOAO = "joao@exemplo.com";

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession loggedIn(String email, String cpf) throws Exception {
        postJson("/api/registrations", TestPayloads.registration(email, cpf)).andExpect(status().isCreated());
        markActive(email);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login").session(session).with(xsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"Segura@123\"}"))
                .andExpect(status().isOk());
        return session;
    }

    private MockHttpSession maria() throws Exception {
        return loggedIn(MARIA, "529.982.247-25");
    }

    private ResultActions getProfile(MockHttpSession session) throws Exception {
        return mockMvc.perform(session == null ? get("/api/profile") : get("/api/profile").session(session));
    }

    private ResultActions putProfile(MockHttpSession session, Map<String, Object> body) throws Exception {
        var request = put("/api/profile").with(xsrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        return mockMvc.perform(session == null ? request : request.session(session));
    }

    /** Alteração válida, com máscaras, a partir dos dados do cadastro. */
    private static Map<String, Object> contact() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", "(21) 3456-7890");
        body.put("cep", "20040-020");
        body.put("street", "Rua da Assembleia");
        body.put("number", "10");
        body.put("complement", "Sala 501");
        body.put("district", "Centro");
        body.put("city", "Rio de Janeiro");
        body.put("state", "rj");
        return body;
    }

    private Map<String, Object> storedRow(String email) {
        return jdbc.queryForMap("SELECT name, cpf, email, birth_date, phone, cep, street, number, complement,"
                + " district, city, state FROM users WHERE email = ?", email);
    }

    // --- Consulta ---

    @Test
    void getDevolveTodosOsDadosDoUsuarioLogado() throws Exception {
        MockHttpSession session = maria();

        getProfile(session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.email").value(MARIA))
                .andExpect(jsonPath("$.birthDate").value("1990-05-20"))
                .andExpect(jsonPath("$.phone").value("11987654321"))
                .andExpect(jsonPath("$.cep").value("01310100"))
                .andExpect(jsonPath("$.street").value("Avenida Paulista"))
                .andExpect(jsonPath("$.number").value("1000"))
                .andExpect(jsonPath("$.complement").isEmpty())
                .andExpect(jsonPath("$.district").value("Bela Vista"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getSemSessaoEhRecusadoComoNaoAutenticado() throws Exception {
        getProfile(null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // --- Edição de telefone e endereço ---

    @Test
    void alteracaoValidaGravaOsValoresNormalizadosEDevolveOPerfilAtualizado() throws Exception {
        MockHttpSession session = maria();

        putProfile(session, contact())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.phone").value("2134567890"))
                .andExpect(jsonPath("$.cep").value("20040020"))
                .andExpect(jsonPath("$.complement").value("Sala 501"))
                .andExpect(jsonPath("$.state").value("RJ"));

        Map<String, Object> row = storedRow(MARIA);
        assertThat(row.get("phone")).isEqualTo("2134567890");
        assertThat(row.get("cep")).isEqualTo("20040020");
        assertThat(row.get("street")).isEqualTo("Rua da Assembleia");
        assertThat(row.get("number")).isEqualTo("10");
        assertThat(row.get("complement")).isEqualTo("Sala 501");
        assertThat(row.get("district")).isEqualTo("Centro");
        assertThat(row.get("city")).isEqualTo("Rio de Janeiro");
        assertThat(row.get("state")).isEqualTo("RJ");

        getProfile(session)
                .andExpect(jsonPath("$.phone").value("2134567890"))
                .andExpect(jsonPath("$.cep").value("20040020"));
    }

    @Test
    void celularComOnzeDigitosEhAceito() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> body = contact();
        body.put("phone", "(21) 99876-5432");

        putProfile(session, body).andExpect(status().isOk());

        assertThat(storedRow(MARIA).get("phone")).isEqualTo("21998765432");
    }

    @Test
    void complementoApagadoEhRemovido() throws Exception {
        MockHttpSession session = maria();
        putProfile(session, contact()).andExpect(status().isOk());

        Map<String, Object> body = contact();
        body.put("complement", "   ");
        putProfile(session, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complement").isEmpty());

        assertThat(storedRow(MARIA).get("complement")).isNull();
    }

    @Test
    void telefoneInvalidoEhRecusadoSemGravarNada() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> before = storedRow(MARIA);
        Map<String, Object> body = contact();
        body.put("phone", "(21) 3456-789");

        putProfile(session, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("phone"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("O telefone deve ter 10 dígitos (fixo) ou 11 dígitos (celular), incluindo o DDD"));

        assertThat(storedRow(MARIA)).isEqualTo(before);
    }

    @Test
    void cidadeVaziaEhRecusada() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> before = storedRow(MARIA);
        Map<String, Object> body = contact();
        body.put("city", "  ");

        putProfile(session, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("city"))
                .andExpect(jsonPath("$.errors[0].message").value("Campo obrigatório"));

        assertThat(storedRow(MARIA)).isEqualTo(before);
    }

    @Test
    void cepEUfInvalidosETamanhoMaximoSaoRecusados() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> body = contact();
        body.put("cep", "2004-020");
        body.put("state", "XX");
        body.put("number", "1".repeat(21));

        putProfile(session, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("cep", "state", "number")));
    }

    // --- RN01: dados críticos não podem ser alterados ---

    @Test
    void novoEmailEhRecusadoSemGravarNada() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> before = storedRow(MARIA);
        Map<String, Object> body = contact();
        body.put("email", "outro@exemplo.com");

        putProfile(session, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("Este campo não pode ser alterado"));

        assertThat(storedRow(MARIA)).isEqualTo(before);
    }

    @Test
    void nomeCpfEDataDeNascimentoSaoApontadosComoNaoAlteraveis() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> before = storedRow(MARIA);
        Map<String, Object> body = contact();
        body.put("name", "Maria Souza");
        body.put("cpf", 11144477735L);
        body.put("birthDate", "1991-01-01");

        putProfile(session, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "cpf", "birthDate")))
                .andExpect(jsonPath("$.errors[*].message", containsInAnyOrder("Este campo não pode ser alterado",
                        "Este campo não pode ser alterado", "Este campo não pode ser alterado")));

        assertThat(storedRow(MARIA)).isEqualTo(before);
    }

    @Test
    void camposCriticosEnviadosComoNuloNaoSaoTentativaDeAlteracao() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> body = contact();
        body.put("name", null);
        body.put("email", null);

        putProfile(session, body).andExpect(status().isOk());
    }

    // --- Isolamento entre usuários ---

    @Test
    void cadaUsuarioVeEAlteraSoOsProprioDados() throws Exception {
        MockHttpSession maria = maria();
        MockHttpSession joao = loggedIn(JOAO, "111.444.777-35");
        Map<String, Object> joaoBefore = storedRow(JOAO);

        getProfile(maria).andExpect(jsonPath("$.email").value(MARIA));
        getProfile(joao).andExpect(jsonPath("$.email").value(JOAO));

        putProfile(maria, contact()).andExpect(status().isOk());

        assertThat(storedRow(MARIA).get("phone")).isEqualTo("2134567890");
        assertThat(storedRow(JOAO)).isEqualTo(joaoBefore);
        getProfile(joao).andExpect(jsonPath("$.phone").value("11987654321"));
    }

    // --- Proteções da sessão ---

    @Test
    void putSemSessaoEhRecusadoComoNaoAutenticado() throws Exception {
        maria();
        Map<String, Object> before = storedRow(MARIA);

        putProfile(null, contact())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        assertThat(storedRow(MARIA)).isEqualTo(before);
    }

    @Test
    void putSemTokenAntiCsrfEhRecusado() throws Exception {
        MockHttpSession session = maria();
        Map<String, Object> before = storedRow(MARIA);

        mockMvc.perform(put("/api/profile").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contact())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        assertThat(storedRow(MARIA)).isEqualTo(before);
    }
}
