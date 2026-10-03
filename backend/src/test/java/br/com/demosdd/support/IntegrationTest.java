package br.com.demosdd.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * Base dos testes de integração: contexto Spring completo, Postgres via
 * Testcontainers, SMTP falso (GreenMail na porta 3025) e relógio controlável.
 * Cada teste começa com o banco vazio e o relógio no instante atual.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInfrastructureConfig.class)
public abstract class IntegrationTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/ativar\\?token=([A-Za-z0-9_-]+)");

    protected static final String XSRF_COOKIE = "XSRF-TOKEN";
    protected static final String XSRF_HEADER = "X-XSRF-TOKEN";

    @RegisterExtension
    protected static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication())
            .withPerMethodLifecycle(true);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetState() {
        jdbc.execute("TRUNCATE TABLE users CASCADE");
        clock.setInstant(Instant.now());
    }

    /** POST em JSON com um token anti-CSRF válido, como faz a interface. */
    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(xsrfToken()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /**
     * Token anti-CSRF no formato da interface: cookie {@code XSRF-TOKEN} e o mesmo
     * valor no cabeçalho {@code X-XSRF-TOKEN}. Diferente do {@code csrf()} do
     * spring-security-test, passa pelo repositório de tokens real e não o troca
     * no contexto compartilhado entre os testes.
     */
    protected static RequestPostProcessor xsrfToken() {
        return xsrfToken(UUID.randomUUID().toString());
    }

    protected static RequestPostProcessor xsrfToken(String token) {
        return request -> {
            Cookie[] current = request.getCookies() == null ? new Cookie[0] : request.getCookies();
            Cookie[] cookies = Arrays.copyOf(current, current.length + 1);
            cookies[current.length] = new Cookie(XSRF_COOKIE, token);
            request.setCookies(cookies);
            request.addHeader(XSRF_HEADER, token);
            return request;
        };
    }

    protected ResultActions activate(String token) throws Exception {
        return postJson("/api/activations", "{\"token\": \"" + token + "\"}");
    }

    protected int countUsers() {
        return jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
    }

    protected String statusOf(String email) {
        return jdbc.queryForObject("SELECT status FROM users WHERE email = ?", String.class, email);
    }

    protected void markActive(String email) {
        jdbc.update("UPDATE users SET status = 'ATIVO', activated_at = now() WHERE email = ?", email);
    }

    /** Último e-mail recebido pelo GreenMail. */
    protected MimeMessage lastEmail() {
        MimeMessage[] messages = greenMail.getReceivedMessages();
        if (messages.length == 0) {
            throw new AssertionError("Nenhum e-mail foi enviado");
        }
        return messages[messages.length - 1];
    }

    /** Texto (parte text/plain) decodificado do e-mail. */
    protected static String plainText(MimeMessage message) throws MessagingException, IOException {
        String text = findPart(message, "text/plain");
        if (text == null) {
            throw new AssertionError("E-mail sem parte text/plain");
        }
        return text;
    }

    /** HTML (parte text/html) decodificado do e-mail. */
    protected static String html(MimeMessage message) throws MessagingException, IOException {
        String html = findPart(message, "text/html");
        if (html == null) {
            throw new AssertionError("E-mail sem parte text/html");
        }
        return html;
    }

    /** Token do link de ativação contido no último e-mail. */
    protected String lastActivationToken() throws MessagingException, IOException {
        Matcher matcher = TOKEN_IN_LINK.matcher(plainText(lastEmail()));
        if (!matcher.find()) {
            throw new AssertionError("Link de ativação não encontrado no e-mail");
        }
        return matcher.group(1);
    }

    private static String findPart(Part part, String mimeType) throws MessagingException, IOException {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = findPart(multipart.getBodyPart(i), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
