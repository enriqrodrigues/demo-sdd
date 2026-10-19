package br.com.demo.todo.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfiguracaoTempoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracaoTempo.class);

    @Test
    void fusoPadraoEhAmericaSaoPaulo() {
        contexto.run(ctx -> assertThat(ctx.getBean(ZoneId.class)).isEqualTo(ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    void fusoPodeSerSobrescritoPorPropriedade() {
        contexto.withPropertyValues("app.fuso-horario=UTC")
                .run(ctx -> assertThat(ctx.getBean(ZoneId.class)).isEqualTo(ZoneId.of("UTC")));
    }

    @Test
    void expoeUmClockDoSistema() {
        contexto.run(ctx -> assertThat(ctx.getBean(Clock.class)).isEqualTo(Clock.systemUTC()));
    }
}
