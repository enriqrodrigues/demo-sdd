package br.com.demosdd.activation;

import java.time.Duration;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/** Monta e envia o e-mail com o link de ativação (RF04). */
@Component
class ActivationEmailSender {

    static final String SUBJECT = "Ative sua conta";

    private final JavaMailSender mailSender;
    private final ActivationProperties properties;

    ActivationEmailSender(JavaMailSender mailSender, ActivationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /**
     * Envia de forma síncrona. Falhas de SMTP chegam como
     * {@link org.springframework.mail.MailException}; endereço inválido como
     * {@link MessagingException}.
     */
    void send(String to, String name, String token) throws MessagingException {
        String link = activationLink(token);
        String validity = describe(properties.activation().tokenTtl());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(properties.mail().from());
        helper.setTo(to);
        helper.setSubject(SUBJECT);
        helper.setText(plainText(name, link, validity), html(name, link, validity));
        mailSender.send(message);
    }

    String activationLink(String token) {
        return properties.baseUrl() + "/ativar?token=" + token;
    }

    private static String plainText(String name, String link, String validity) {
        return """
                Olá, %s!

                Recebemos o seu cadastro. Para ativar a sua conta, acesse o link abaixo:

                %s

                Este link expira em %s e só pode ser usado uma vez.

                Se você não fez este cadastro, ignore este e-mail.
                """.formatted(name, link, validity);
    }

    private static String html(String name, String link, String validity) {
        return """
                <p>Olá, %s!</p>
                <p>Recebemos o seu cadastro. Para ativar a sua conta, clique no link abaixo:</p>
                <p><a href="%s">Ativar minha conta</a></p>
                <p>Este link expira em %s e só pode ser usado uma vez.</p>
                <p>Se você não fez este cadastro, ignore este e-mail.</p>
                """.formatted(HtmlUtils.htmlEscape(name), link, validity);
    }

    private static String describe(Duration ttl) {
        long hours = ttl.toHours();
        return hours == 1 ? "1 hora" : hours + " horas";
    }
}
