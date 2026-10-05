package br.demo.usuarios.compartilhado;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base única dos testes de integração do backend (NFR-4): PostgreSQL 18 em Testcontainers,
 * GreenMail em porta dinâmica e {@link java.time.Clock} controlável.
 *
 * <p>O Postgres e o GreenMail sobem uma única vez por JVM e não são parados entre as classes de
 * teste, porque o contexto do Spring fica em cache e continua apontando para as mesmas portas.
 */
@SpringBootTest
@Import(TesteIntegracao.ConfiguracaoTeste.class)
public abstract class TesteIntegracao {

    protected static final Instant INSTANTE_INICIAL = Instant.parse("2026-01-15T12:00:00Z");

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

    protected static final GreenMail GREEN_MAIL = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

    static {
        POSTGRES.start();
        GREEN_MAIL.start();
    }

    @DynamicPropertySource
    static void propriedadesSmtp(DynamicPropertyRegistry registro) {
        registro.add("spring.mail.host", () -> GREEN_MAIL.getSmtp().getBindTo());
        registro.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
        registro.add("spring.mail.username", () -> "");
        registro.add("spring.mail.password", () -> "");
        registro.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registro.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        registro.add("spring.mail.properties.mail.smtp.starttls.required", () -> "false");
        registro.add("app.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired
    protected RelogioAjustavel relogio;

    @BeforeEach
    void reiniciarEstado() throws Exception {
        relogio.definir(INSTANTE_INICIAL);
        GREEN_MAIL.purgeEmailFromAllMailboxes();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracaoTeste {

        @Bean
        @Primary
        RelogioAjustavel relogioAjustavel() {
            return new RelogioAjustavel(INSTANTE_INICIAL);
        }
    }
}
