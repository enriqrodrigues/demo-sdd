package br.com.demosdd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import br.com.demosdd.registration.TestPayloads;
import br.com.demosdd.support.IntegrationTest;

/**
 * Cookie de sessão emitido pelo Tomcat real (spec "Proteção do identificador de
 * sessão"): o MockMvc não passa pelo container e não emite o JSESSIONID.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCookieHttpTest extends IntegrationTest {

    private static final String EMAIL = "maria@exemplo.com";

    @LocalServerPort
    private int port;

    private CookieManager cookies;
    private HttpClient http;

    @BeforeEach
    void createActiveUserAndClient() throws Exception {
        postJson("/api/registrations", TestPayloads.registration(EMAIL, "529.982.247-25"))
                .andExpect(status().isCreated());
        markActive(EMAIL);
        cookies = new CookieManager();
        http = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    /** Login como a interface faz: busca o token anti-CSRF e o envia no cabeçalho. */
    private HttpResponse<String> login() throws Exception {
        if (cookie("XSRF-TOKEN").isEmpty()) {
            http.send(HttpRequest.newBuilder(url("/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.discarding());
        }
        HttpRequest request = HttpRequest.newBuilder(url("/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", cookie("XSRF-TOKEN").orElseThrow())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\": \"" + EMAIL + "\", \"password\": \"Segura@123\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Optional<String> cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst();
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @Test
    void cookieDeSessaoEhHttpOnly() throws Exception {
        HttpResponse<String> response = login();

        assertThat(response.statusCode()).isEqualTo(200);
        String sessionCookie = response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith("JSESSIONID="))
                .findFirst().orElseThrow();
        assertThat(sessionCookie).containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Lax");
    }

    @Test
    void novoLoginEmiteIdDeSessaoDiferente() throws Exception {
        assertThat(login().statusCode()).isEqualTo(200);
        String first = cookie("JSESSIONID").orElseThrow();

        assertThat(login().statusCode()).isEqualTo(200);

        assertThat(cookie("JSESSIONID").orElseThrow()).isNotEqualTo(first);
    }
}
