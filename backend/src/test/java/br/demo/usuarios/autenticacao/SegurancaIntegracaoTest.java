package br.demo.usuarios.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.demo.usuarios.compartilhado.TesteIntegracao;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("AD-7, AD-15: cadeia de segurança da API")
class SegurancaIntegracaoTest extends TesteIntegracao {

    @Autowired
    MockMvc mvc;

    @Autowired
    Environment ambiente;

    @Autowired
    ApplicationContext contexto;

    @Test
    @DisplayName("AD-7, AD-15: GET /api/auth/sessao sem Sessão responde {autenticado:false}, entrega XSRF-TOKEN e não cria Sessão")
    void sessaoAnonima() throws Exception {
        var resultado = mvc.perform(get("/api/auth/sessao"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"autenticado\":false}", JsonCompareMode.STRICT))
                .andReturn();

        Cookie xsrf = resultado.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.getValue()).isNotBlank();
        assertThat(xsrf.isHttpOnly()).isFalse();
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("AD-7: POST sem X-XSRF-TOKEN responde 403 CSRF_INVALIDO no envelope")
    void postSemCsrf() throws Exception {
        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"))
                .andExpect(jsonPath("$.mensagem").value("Sua sessão de navegação expirou. Recarregue a página."))
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    @DisplayName("AD-7: POST com cookie do bootstrap e X-XSRF-TOKEN divergente responde 403 CSRF_INVALIDO")
    void postComCsrfDivergente() throws Exception {
        Cookie xsrf = mvc.perform(get("/api/auth/sessao")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();

        mvc.perform(post("/api/cadastro")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue() + "-divergente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"))
                .andExpect(jsonPath("$.mensagem").value("Sua sessão de navegação expirou. Recarregue a página."))
                .andExpect(jsonPath("$.campos", empty()));
    }

    @ParameterizedTest(name = "AD-7, AD-15: POST {0} com CSRF válido passa pela segurança (sem controller ainda: 404)")
    @ValueSource(strings = {"/api/cadastro", "/api/ativacao", "/api/auth/login", "/api/auth/logout"})
    @DisplayName("AD-7, AD-15: POST público com cookie e header do bootstrap passa pela segurança")
    void postComCsrfValido(String rota) throws Exception {
        Cookie xsrf = mvc.perform(get("/api/auth/sessao")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();

        var resultado = mvc.perform(post(rota)
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Recurso não encontrado."))
                .andExpect(jsonPath("$.campos", empty()))
                .andReturn();
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("AD-15: rota protegida sem Sessão responde 401 NAO_AUTENTICADO no envelope e não cria Sessão")
    void rotaProtegidaSemSessao() throws Exception {
        var resultado = mvc.perform(get("/api/qualquer"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensagem").value("Sua sessão expirou. Faça login novamente."))
                .andExpect(jsonPath("$.campos", empty()))
                .andReturn();
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("AD-15: as rotas públicas valem só para o método declarado")
    void rotasPublicasPorMetodo() throws Exception {
        mvc.perform(get("/api/cadastro"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("AD-7: o cookie de Sessão JSESSIONID é HttpOnly e SameSite=Lax")
    void atributosDoCookieDeSessao() {
        assertThat(ambiente.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
        assertThat(ambiente.getProperty("server.servlet.session.cookie.same-site")).isEqualToIgnoringCase("lax");
    }

    @Test
    @DisplayName("AD-15: o Boot não cria usuário em memória com senha gerada")
    void semUsuarioGerado() {
        assertThat(contexto.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }
}
