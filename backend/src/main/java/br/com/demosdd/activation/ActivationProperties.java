package br.com.demosdd.activation;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do link de ativação ({@code app.*} no application.yml).
 *
 * @param baseUrl   URL pública da aplicação, usada para montar o link do e-mail
 * @param mail      remetente do e-mail
 * @param activation validade do link (RN02)
 */
@ConfigurationProperties(prefix = "app")
record ActivationProperties(String baseUrl, Mail mail, Activation activation) {

    record Mail(String from) {
    }

    record Activation(Duration tokenTtl) {
    }
}
