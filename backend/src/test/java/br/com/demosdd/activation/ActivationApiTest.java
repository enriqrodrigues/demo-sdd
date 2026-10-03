package br.com.demosdd.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.demosdd.registration.TestPayloads;
import br.com.demosdd.support.IntegrationTest;

import jakarta.mail.internet.MimeMessage;

/** Cenários da spec account-activation exercitados pela API, com banco real e SMTP falso. */
class ActivationApiTest extends IntegrationTest {

    private static final String EMAIL = "maria@exemplo.com";

    private String registerAndGetToken(String email, String cpf) throws Exception {
        postJson("/api/registrations", TestPayloads.registration(email, cpf)).andExpect(status().isCreated());
        return lastActivationToken();
    }

    // --- Envio do e-mail de ativação ---

    @Test
    void emailDeAtivacaoEhEnviadoAoEnderecoCadastrado() throws Exception {
        postJson("/api/registrations", TestPayloads.validRegistration()).andExpect(status().isCreated());

        MimeMessage email = lastEmail();
        assertThat(email.getAllRecipients()).hasSize(1);
        assertThat(email.getAllRecipients()[0].toString()).isEqualTo(EMAIL);
        assertThat(email.getSubject()).isEqualTo("Ative sua conta");
        String text = plainText(email);
        assertThat(text).contains("Olá, Maria da Silva!")
                .containsPattern("http://localhost:8080/ativar\\?token=[A-Za-z0-9_-]{43}")
                .contains("expira em 24 horas");
        assertThat(html(email)).contains("href=\"http://localhost:8080/ativar?token=");
    }

    // --- Link exclusivo e imprevisível ---

    @Test
    void tokenEhGravadoSomenteComoHashComValidadeDe24Horas() throws Exception {
        String token = registerAndGetToken(EMAIL, "529.982.247-25");

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM activation_tokens");
        assertThat(row.get("token_hash")).isEqualTo(ActivationTokens.hash(token)).isNotEqualTo(token);
        Duration validity = Duration.between(((Timestamp) row.get("created_at")).toInstant(),
                ((Timestamp) row.get("expires_at")).toInstant());
        assertThat(validity).isEqualTo(Duration.ofHours(24));
        assertThat(row.get("used_at")).isNull();
    }

    @Test
    void tokenDeUmUsuarioNaoAtivaOutro() throws Exception {
        String tokenMaria = registerAndGetToken(EMAIL, "529.982.247-25");
        String tokenJoao = registerAndGetToken("joao@exemplo.com", "111.444.777-35");

        assertThat(tokenMaria).isNotEqualTo(tokenJoao);

        activate(tokenMaria).andExpect(status().isOk());

        assertThat(statusOf(EMAIL)).isEqualTo("ATIVO");
        assertThat(statusOf("joao@exemplo.com")).isEqualTo("PENDENTE");
    }

    // --- Ativação da conta ---

    @Test
    void ativacaoComLinkValidoAtivaAConta() throws Exception {
        String token = registerAndGetToken(EMAIL, "529.982.247-25");

        activate(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        assertThat(statusOf(EMAIL)).isEqualTo("ATIVO");
        assertThat(jdbc.queryForObject("SELECT activated_at FROM users", Timestamp.class)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT used_at FROM activation_tokens", Timestamp.class)).isNotNull();
    }

    // --- Validade de 24 horas ---

    @Test
    void linkUsadoDentroDoPrazoAtivaAConta() throws Exception {
        String token = registerAndGetToken(EMAIL, "529.982.247-25");
        clock.advance(Duration.ofHours(23));

        activate(token).andExpect(status().isOk());

        assertThat(statusOf(EMAIL)).isEqualTo("ATIVO");
    }

    @Test
    void linkExpiradoEhRecusadoEContaPermanecePendente() throws Exception {
        String token = registerAndGetToken(EMAIL, "529.982.247-25");
        clock.advance(Duration.ofHours(24).plusMinutes(1));

        activate(token)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.detail").value(containsString("novo cadastro com os mesmos dados")));

        assertThat(statusOf(EMAIL)).isEqualTo("PENDENTE");
    }

    // --- Uso único do link ---

    @Test
    void linkReutilizadoEhRecusado() throws Exception {
        String token = registerAndGetToken(EMAIL, "529.982.247-25");
        activate(token).andExpect(status().isOk());

        activate(token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOKEN_ALREADY_USED"));

        assertThat(statusOf(EMAIL)).isEqualTo("ATIVO");
    }

    // --- Link inválido ---

    @Test
    void tokenDesconhecidoEhRecusado() throws Exception {
        registerAndGetToken(EMAIL, "529.982.247-25");

        activate("token-inexistente-ou-adulterado")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        assertThat(statusOf(EMAIL)).isEqualTo("PENDENTE");
    }

    @Test
    void tokenVazioEhRecusado() throws Exception {
        activate("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    // --- Ativação exige ação do usuário ---

    @Test
    void abrirOLinkNaoAlteraAContaNemOToken() throws Exception {
        String token = registerAndGetToken(EMAIL, "529.982.247-25");

        mockMvc.perform(get("/ativar").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<div id=\"root\">")));

        assertThat(statusOf(EMAIL)).isEqualTo("PENDENTE");
        assertThat(jdbc.queryForObject("SELECT used_at FROM activation_tokens", Timestamp.class)).isNull();
        activate(token).andExpect(status().isOk());
    }
}
