package br.com.demosdd.activation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import br.com.demosdd.registration.UserRegistered;
import br.com.demosdd.shared.web.ApiException;

import jakarta.mail.MessagingException;

/**
 * Emite o link e envia o e-mail de ativação para cada novo cadastro.
 * Listener síncrono, na transação do cadastro: se o envio falhar, a exceção
 * desfaz o cadastro (spec "Cadastro só é concluído com o e-mail enviado").
 */
@Component
class UserRegisteredListener {

    static final String EMAIL_UNAVAILABLE = "EMAIL_UNAVAILABLE";

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);

    private final ActivationService activationService;
    private final ActivationEmailSender emailSender;

    UserRegisteredListener(ActivationService activationService, ActivationEmailSender emailSender) {
        this.activationService = activationService;
        this.emailSender = emailSender;
    }

    @EventListener
    void on(UserRegistered event) {
        String token = activationService.issue(event.userId());
        try {
            emailSender.send(event.email(), event.name(), token);
        } catch (MailException | MessagingException ex) {
            log.warn("Falha ao enviar e-mail de ativação para {}: {}", event.email(), ex.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, EMAIL_UNAVAILABLE,
                    "Não foi possível concluir o cadastro",
                    "Não foi possível enviar o e-mail de ativação. Tente novamente em alguns minutos.");
        }
    }
}
