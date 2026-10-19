package br.com.demo.todo.comum;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Única fonte de "agora" da aplicação ({@link Clock}) e fuso em que instantes
 * viram datas locais ({@code app.fuso-horario}).
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracaoTempo {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId fusoHorario(@Value("${app.fuso-horario:America/Sao_Paulo}") String fuso) {
        return ZoneId.of(fuso);
    }
}
