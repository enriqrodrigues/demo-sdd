package br.com.demo.cadastro.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class SpaControllerTest extends IntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/cadastro", "/cadastro/concluido", "/ativar", "/login", "/perfil"})
    void rotasDaSpaSaoEncaminhadasParaOIndex(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void rotasDaApiNaoSaoEncaminhadas() throws Exception {
        mvc.perform(get("/api/inexistente")).andExpect(status().isUnauthorized());
    }

    @Test
    void arquivosComExtensaoNaoSaoEncaminhados() throws Exception {
        mvc.perform(get("/assets/nao-existe.js")).andExpect(status().isNotFound());
    }
}
