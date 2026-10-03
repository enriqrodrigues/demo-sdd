package br.com.demosdd.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import br.com.demosdd.support.IntegrationTest;

import jakarta.servlet.http.Cookie;

/** Emissão do token anti-CSRF para a interface (design D4). */
class CsrfApiTest extends IntegrationTest {

    @Test
    void endpointCsrfEmiteOCookieXsrfToken() throws Exception {
        Cookie cookie = mockMvc.perform(get("/api/auth/csrf"))                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
    }
}
