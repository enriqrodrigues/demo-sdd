package br.com.demo.cadastro.ativacao;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class EmailAtivacao {

    private final JavaMailSender mailSender;
    private final String remetente;
    private final long horasValidade;

    EmailAtivacao(JavaMailSender mailSender,
                  @Value("${spring.mail.username}") String remetente,
                  @Value("${app.ativacao.expiracao}") Duration validade) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.horasValidade = validade.toHours();
    }

    void enviar(String para, String nome, String link) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper ajudante = new MimeMessageHelper(mensagem, true, "UTF-8");
            ajudante.setFrom(remetente);
            ajudante.setTo(para);
            ajudante.setSubject("Ative sua conta");
            ajudante.setText(texto(nome, link), html(nome, link));
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            throw new MailPreparationException("Falha ao montar o e-mail de ativação", e);
        }
    }

    private String texto(String nome, String link) {
        return """
                Olá, %s!

                Recebemos o seu cadastro. Para ativar a sua conta, acesse o link abaixo:

                %s

                Este link é válido por %d horas.
                """.formatted(nome, link, horasValidade);
    }

    private String html(String nome, String link) {
        return """
                <p>Olá, %s!</p>
                <p>Recebemos o seu cadastro. Para ativar a sua conta, clique no link abaixo:</p>
                <p><a href="%s">Ativar minha conta</a></p>
                <p>Este link é válido por %d horas.</p>
                """.formatted(HtmlUtils.htmlEscape(nome), HtmlUtils.htmlEscape(link), horasValidade);
    }
}
