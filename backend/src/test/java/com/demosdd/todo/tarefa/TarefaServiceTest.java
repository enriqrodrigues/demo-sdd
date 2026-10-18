package com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.demosdd.todo.prazo.SituacaoPrazo;

/** A situação de prazo da resposta vem do {@link Clock} injetado, no fuso de São Paulo. */
class TarefaServiceTest {

    private final TarefaRepository repository = mock(TarefaRepository.class);

    private TarefaService serviceEm(String instante) {
        Clock clock = Clock.fixed(Instant.parse(instante), ZoneId.of("America/Sao_Paulo"));
        return new TarefaService(repository, clock);
    }

    @Test
    void situacaoPrazoDaRespostaUsaADataDoClock() {
        given(repository.save(any(Tarefa.class))).willAnswer(invocacao -> invocacao.getArgument(0));
        CriarTarefaRequest request = new CriarTarefaRequest("Pagar conta", null, LocalDate.of(2026, 10, 17),
                Prioridade.ALTA);

        // 2026-10-18T01:00Z ainda é 2026-10-17 em São Paulo
        assertThat(serviceEm("2026-10-18T01:00:00Z").criar(request).situacaoPrazo())
                .isEqualTo(SituacaoPrazo.PARA_HOJE);
        assertThat(serviceEm("2026-10-18T12:00:00Z").criar(request).situacaoPrazo())
                .isEqualTo(SituacaoPrazo.ATRASADA);
    }
}
