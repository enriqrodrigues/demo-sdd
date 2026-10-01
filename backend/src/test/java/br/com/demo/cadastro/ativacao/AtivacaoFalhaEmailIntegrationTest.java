package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AtivacaoFalhaEmailIntegrationTest extends IntegrationTest {

    @MockitoBean
    EmailAtivacao emailAtivacao;

    @Test
    void falhaNoEnvioNaoDesfazOCadastroEDeixaOEventoPendente() throws Exception {
        doThrow(new MailSendException("SMTP indisponível"))
                .when(emailAtivacao).enviar(anyString(), anyString(), anyString());
        String email = DadosTeste.emailUnico();

        String id = cadastrar(email, DadosTeste.cpfValido());

        verify(emailAtivacao, timeout(5000)).enviar(eq(email), anyString(), anyString());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(statusDoUsuario(id)).isEqualTo("PENDENTE_ATIVACAO");
            assertThat(jdbc.queryForObject(
                    "select count(*) from token_ativacao where usuario_id = ?::uuid", Integer.class, id)).isZero();
            assertThat(jdbc.queryForObject(
                    "select count(*) from event_publication where completion_date is null and serialized_event like ?",
                    Integer.class, "%" + id + "%")).isEqualTo(1);
        });
    }
}
