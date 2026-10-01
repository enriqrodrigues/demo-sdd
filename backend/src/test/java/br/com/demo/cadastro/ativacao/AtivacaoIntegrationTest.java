package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.EmailsTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AtivacaoIntegrationTest extends IntegrationTest {

    private ResultActions ativar(String corpo) throws Exception {
        return mvc.perform(post("/api/ativacao").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions ativarToken(String token) throws Exception {
        return ativar("{\"token\":\"%s\"}".formatted(token));
    }

    @Test
    void cadastroEnviaEmailDeAtivacaoParaOEnderecoCadastrado() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());

        MimeMessage mensagem = EmailsTeste.aguardarMensagem(greenMail, email);

        assertThat(mensagem.getSubject()).isEqualTo("Ative sua conta");
        assertThat(mensagem.getFrom()[0].toString()).isEqualTo("cadastro@teste.local");
        assertThat(EmailsTeste.conteudo(mensagem))
                .contains("Olá, Maria da Silva!")
                .contains("http://localhost:8080/ativar?token=")
                .contains("24 horas");
    }

    @Test
    void linkDoEmailAtivaAContaUmaUnicaVez() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = cadastrar(email, DadosTeste.cpfValido());
        String token = EmailsTeste.extrairToken(EmailsTeste.aguardarMensagem(greenMail, email));

        String hashGravado = jdbc.queryForObject(
                "select token_hash from token_ativacao where usuario_id = ?::uuid", String.class, id);
        assertThat(hashGravado).isEqualTo(GeradorToken.hash(token)).isNotEqualTo(token);

        ativarToken(token).andExpect(status().isNoContent());
        assertThat(statusDoUsuario(id)).isEqualTo("ATIVO");

        ativarToken(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_JA_UTILIZADO"));
    }

    @Test
    void tokenExpiradoNaoAtivaAConta() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = cadastrar(email, DadosTeste.cpfValido());
        String token = "expirado-" + UUID.randomUUID();
        jdbc.update("""
                insert into token_ativacao (id, usuario_id, token_hash, expira_em, criado_em)
                values (gen_random_uuid(), ?::uuid, ?, now() - interval '1 minute', now() - interval '25 hours')
                """, id, GeradorToken.hash(token));

        ativarToken(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
        assertThat(statusDoUsuario(id)).isEqualTo("PENDENTE_ATIVACAO");
    }

    @Test
    void tokenDesconhecidoVazioOuAusenteEhInvalido() throws Exception {
        ativarToken("nao-existe").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        ativarToken("").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        ativar("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void nomeEhEscapadoNoHtmlDoEmail() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido(), "<b>Ana</b>");

        String conteudo = EmailsTeste.conteudo(EmailsTeste.aguardarMensagem(greenMail, email));

        assertThat(conteudo).contains("<p>Olá, &lt;b&gt;Ana&lt;/b&gt;!</p>").doesNotContain("<p>Olá, <b>Ana</b>");
    }
}
