package br.com.demo.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelogioIT extends TesteIntegracao {

    @Autowired
    private Clock clock;

    @Test
    void contextoDeTesteUsaORelogioMutavel() {
        relogio.avancar(Duration.ofHours(5));

        assertThat(clock.instant()).isEqualTo(INSTANTE_PADRAO.plus(Duration.ofHours(5)));
    }

    @Test
    void relogioVoltaAoInstantePadraoEntreTestes() {
        assertThat(clock.instant()).isEqualTo(INSTANTE_PADRAO);
    }
}
