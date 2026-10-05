package br.demo.usuarios.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.internet.MimeMessage;
import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@DisplayName("NFR-4: base de testes de integração")
class FumacaIntegracaoTest extends TesteIntegracao {

    @Autowired
    ApplicationContext contexto;

    @Autowired
    DataSource dataSource;

    @Autowired
    JavaMailSender enviador;

    @Autowired
    Clock clock;

    @Test
    @DisplayName("NFR-4: o contexto da aplicação sobe")
    void contextoSobe() {
        assertThat(contexto).isNotNull();
    }

    @Test
    @DisplayName("NFR-4: o datasource aponta para o PostgreSQL 18")
    void datasourceNoPostgres18() throws Exception {
        try (Connection conexao = dataSource.getConnection()) {
            var metadados = conexao.getMetaData();
            assertThat(metadados.getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(metadados.getDatabaseMajorVersion()).isEqualTo(18);
        }
    }

    @Test
    @DisplayName("NFR-4: o GreenMail recebe a mensagem enviada pelo JavaMailSender")
    void greenMailRecebeMensagem() throws Exception {
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom("nao-responda@demo.br");
        mensagem.setTo("ana@exemplo.com");
        mensagem.setSubject("Teste de fumaça");
        mensagem.setText("Olá");

        enviador.send(mensagem);

        MimeMessage[] recebidas = GREEN_MAIL.getReceivedMessages();
        assertThat(recebidas).hasSize(1);
        assertThat(recebidas[0].getSubject()).isEqualTo("Teste de fumaça");
        assertThat(recebidas[0].getAllRecipients()[0].toString()).isEqualTo("ana@exemplo.com");
    }

    @Test
    @DisplayName("NFR-4: o Clock injetado é o RelogioAjustavel e avança sob controle do teste")
    void clockAvanca() {
        assertThat(clock).isSameAs(relogio);
        var antes = clock.instant();
        var noFuso = clock.withZone(ZoneId.of("America/Sao_Paulo"));

        relogio.avancar(Duration.ofHours(25));

        assertThat(clock.instant()).isEqualTo(antes.plus(Duration.ofHours(25)));
        assertThat(noFuso.instant()).isEqualTo(antes.plus(Duration.ofHours(25)));
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
