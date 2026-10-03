package br.com.demosdd.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.demosdd.support.IntegrationTest;

/** Cenários da spec user-registration exercitados pela API, com banco real. */
class RegistrationApiTest extends IntegrationTest {

    private static final String EMAIL = "maria@exemplo.com";
    private static final String CPF = "529.982.247-25";
    private static final String OTHER_CPF = "111.444.777-35";

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Gravação do usuário como pendente ---

    @Test
    void cadastroValidoGravaUsuarioPendenteComDadosNormalizados() throws Exception {
        postJson("/api/registrations", TestPayloads.validRegistration())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM users WHERE email = ?", EMAIL);
        assertThat(row.get("status")).isEqualTo("PENDENTE");
        assertThat(row.get("cpf")).isEqualTo("52998224725");
        assertThat(row.get("phone")).isEqualTo("11987654321");
        assertThat(row.get("cep")).isEqualTo("01310100");
        assertThat(row.get("state")).isEqualTo("SP");
        assertThat(row.get("complement")).isNull();
    }

    @Test
    void senhaEhArmazenadaSomenteComoHash() throws Exception {
        postJson("/api/registrations", TestPayloads.validRegistration()).andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, EMAIL);
        assertThat(hash).isNotEqualTo("Segura@123");
        assertThat(passwordEncoder.matches("Segura@123", hash)).isTrue();
    }

    @Test
    void emailEhNormalizadoParaMinusculas() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(" Maria@Exemplo.com ", CPF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL));

        assertThat(statusOf(EMAIL)).isEqualTo("PENDENTE");
    }

    @Test
    void telefoneFixoEhAceito() throws Exception {
        String body = TestPayloads.validRegistration().replace("(11) 98765-4321", "(11) 3456-7890");

        postJson("/api/registrations", body).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT phone FROM users", String.class)).isEqualTo("1134567890");
    }

    // --- Validação definitiva no servidor ---

    @Test
    void envioDiretoComDadosInvalidosNaoGravaNada() throws Exception {
        String body = TestPayloads.validRegistration()
                .replace(CPF, "529.982.247-26")
                .replace("(11) 98765-4321", "(11) 8765-432");

        postJson("/api/registrations", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("cpf", "phone")));

        assertThat(countUsers()).isZero();
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    // --- Unicidade de e-mail e CPF ---

    @Test
    void recusaEmailDeContaAtiva() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF)).andExpect(status().isCreated());
        markActive(EMAIL);

        postJson("/api/registrations", TestPayloads.registration(EMAIL, OTHER_CPF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("E-mail já cadastrado"));
    }

    @Test
    void recusaCpfDeContaAtiva() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF)).andExpect(status().isCreated());
        markActive(EMAIL);

        postJson("/api/registrations", TestPayloads.registration("joao@exemplo.com", CPF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.errors[0].field").value("cpf"))
                .andExpect(jsonPath("$.errors[0].message").value("CPF já cadastrado"));
    }

    @Test
    void recusaEmailComCaixaDiferente() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF)).andExpect(status().isCreated());
        markActive(EMAIL);

        postJson("/api/registrations", TestPayloads.registration("MARIA@exemplo.com", OTHER_CPF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void recusaCadastroPendenteComLinkValido() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF)).andExpect(status().isCreated());
        clock.advance(Duration.ofHours(23));

        postJson("/api/registrations", TestPayloads.registration(EMAIL, OTHER_CPF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_ACTIVATION"))
                .andExpect(jsonPath("$.detail").value(
                        "Já existe um cadastro aguardando ativação. Verifique seu e-mail para ativar a conta."));

        assertThat(countUsers()).isEqualTo(1);
    }

    // --- Substituição de cadastro pendente expirado ---

    @Test
    void recadastroAposExpiracaoSubstituiOPendente() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF)).andExpect(status().isCreated());
        String oldId = jdbc.queryForObject("SELECT id::text FROM users WHERE email = ?", String.class, EMAIL);
        clock.advance(Duration.ofHours(25));

        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF)).andExpect(status().isCreated());

        assertThat(countUsers()).isEqualTo(1);
        String newId = jdbc.queryForObject("SELECT id::text FROM users WHERE email = ?", String.class, EMAIL);
        assertThat(newId).isNotEqualTo(oldId);
        assertThat(statusOf(EMAIL)).isEqualTo("PENDENTE");
        assertThat(greenMail.getReceivedMessages()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM activation_tokens", Integer.class)).isEqualTo(1);
    }

    @Test
    void conflitoMistoRecusaEMantemPendenteExpirado() throws Exception {
        // Pendente expirado dono do e-mail
        postJson("/api/registrations", TestPayloads.registration(EMAIL, OTHER_CPF)).andExpect(status().isCreated());
        // Ativo dono do CPF
        postJson("/api/registrations", TestPayloads.registration("joao@exemplo.com", CPF))
                .andExpect(status().isCreated());
        markActive("joao@exemplo.com");
        clock.advance(Duration.ofHours(25));

        postJson("/api/registrations", TestPayloads.registration(EMAIL, CPF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("cpf")));

        assertThat(countUsers()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT cpf FROM users WHERE email = ?", String.class, EMAIL))
                .isEqualTo("11144477735");
    }

    // --- Cadastro só é concluído com o e-mail enviado ---

    @Test
    void falhaNoEnvioDoEmailDesfazOCadastro() throws Exception {
        greenMail.stop();

        postJson("/api/registrations", TestPayloads.validRegistration())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EMAIL_UNAVAILABLE"));

        assertThat(countUsers()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM activation_tokens", Integer.class)).isZero();
    }
}
