package br.com.demosdd.shared.validation;

import java.time.Clock;

import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Faz {@code @PastOrPresent} (data de nascimento não futura) usar o mesmo
 * {@link Clock} da aplicação, controlável nos testes.
 */
@Configuration
class ValidationClockConfig {

    @Bean
    ValidationConfigurationCustomizer clockProviderCustomizer(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
