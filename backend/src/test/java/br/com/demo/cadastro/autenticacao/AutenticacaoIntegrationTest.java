package br.com.demo.cadastro.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class AutenticacaoIntegrationTest extends IntegrationTest {

    private ResultActions login(String email, String senha) throws Exception {
        return login(email, senha, new MockHttpSession());
    }

    private ResultActions login(String email, String senha, MockHttpSession sessao) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    @Test
    void usuarioAtivoEntraERecebeSessaoComSeuId() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);
        MockHttpSession sessaoAnterior = new MockHttpSession();

        MvcResult resultado = login(email.toUpperCase(), DadosTeste.SENHA, sessaoAnterior)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        assertThat(sessaoAnterior.isInvalid()).as("sessão anterior invalidada (fixação de sessão)").isTrue();
        HttpSession sessao = resultado.getRequest().getSession(false);
        SecurityContext contexto = (SecurityContext) sessao.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(contexto.getAuthentication().getName()).isEqualTo(id);
    }

    @Test
    void senhaErradaEEmailInexistenteRetornamOMesmo401() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);

        login(email, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
        login(DadosTeste.emailUnico(), DadosTeste.SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void usuarioPendenteComSenhaCorretaRecebe403SemSessao() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());

        MvcResult resultado = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, DadosTeste.SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CONTA_PENDENTE"))
                .andReturn();

        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    void usuarioPendenteComSenhaErradaRecebe401() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());

        login(email, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void senhaAcimaDe72BytesRetorna401EmVezDe500() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);

        login(email, "Aa1!" + "é".repeat(40)).andExpect(status().isUnauthorized());
    }

    @Test
    void corpoSemCamposRetorna400() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void logoutInvalidaASessao() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);
        MockHttpSession sessao = (MockHttpSession) login(email, DadosTeste.SENHA)
                .andReturn().getRequest().getSession(false);

        mvc.perform(post("/api/auth/logout").with(csrf()).session(sessao)).andExpect(status().isNoContent());

        assertThat(sessao.isInvalid()).isTrue();
    }

    @Test
    void logoutSemSessaoRetorna401() throws Exception {
        mvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isUnauthorized());
    }
}
