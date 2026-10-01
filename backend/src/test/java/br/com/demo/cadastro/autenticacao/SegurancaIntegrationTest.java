package br.com.demo.cadastro.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

// csrf() de outras classes troca o repositório de token do CsrfFilter compartilhado; contexto limpo garante o cookie XSRF-TOKEN real.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SegurancaIntegrationTest extends IntegrationTest {

    @Test
    void apiProtegidaSemSessaoRetorna401EmProblemJson() throws Exception {
        mvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void endpointCsrfEmiteCookieLegivelPeloJavascript() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();

        String cookie = resultado.getResponse().getCookie("XSRF-TOKEN").getValue();
        String token = JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
        assertThat(token).isEqualTo(cookie);
    }

    @Test
    void mutacaoSemTokenCsrfEhRecusada() throws Exception {
        mvc.perform(post("/api/ativacao").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
    }

    @Test
    void recursosForaDaApiNaoExigemAutenticacao() throws Exception {
        mvc.perform(get("/arquivo-inexistente.js")).andExpect(status().isNotFound());
    }
}
