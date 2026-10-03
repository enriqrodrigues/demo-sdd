package br.com.demosdd.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.servlet.server.Session.SessionTrackingMode;

import br.com.demosdd.support.IntegrationTest;

/**
 * Configuração da sessão (spec "Expiração da sessão por inatividade", design D6).
 * A passagem real do tempo não é simulada: o Tomcat usa o relógio do sistema.
 */
class SessionConfigurationTest extends IntegrationTest {

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void sessaoExpiraApos30MinutosDeInatividade() {
        assertThat(serverProperties.getServlet().getSession().getTimeout()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void cookieDeSessaoEhHttpOnlyLaxERastreadoSoPorCookie() {
        var session = serverProperties.getServlet().getSession();
        assertThat(session.getCookie().getHttpOnly()).isTrue();
        assertThat(session.getCookie().getSameSite()).isEqualTo(SameSite.LAX);
        assertThat(session.getCookie().getSecure()).isFalse();
        assertThat(session.getTrackingModes()).containsExactly(SessionTrackingMode.COOKIE);
    }
}
