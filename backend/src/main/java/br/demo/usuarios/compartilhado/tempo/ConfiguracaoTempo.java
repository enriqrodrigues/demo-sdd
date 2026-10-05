package br.demo.usuarios.compartilhado.tempo;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único {@link Clock} de produção, em UTC (AD-14). Nenhuma outra classe lê o relógio do sistema.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoTempo {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
