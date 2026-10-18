package com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.demosdd.todo.prazo.SituacaoPrazo;

@WebMvcTest(TarefaController.class)
class TarefaControllerValidacaoTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TITULO_100 = "a".repeat(100);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TarefaService service;

    private ResultActions criar(String corpo) throws Exception {
        return mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions atualizar(String corpo) throws Exception {
        return mvc.perform(put("/api/tarefas/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static void erroNoCampo(ResultActions resposta, String campo) throws Exception {
        resposta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].campo").value(campo))
                .andExpect(jsonPath("$.errors[0].mensagem").isString());
    }

    private static TarefaResponse resposta(String titulo) {
        return new TarefaResponse(ID, titulo, null, LocalDate.of(2026, 10, 20), Prioridade.ALTA,
                StatusTarefa.PENDENTE, null, SituacaoPrazo.NO_PRAZO);
    }

    // --- título ---

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\"}",
            "{\"titulo\":\"\",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\"}",
            "{\"titulo\":\"    \",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\"}"
    })
    void criacaoComTituloAusenteOuEmBrancoEhRejeitada(String corpo) throws Exception {
        erroNoCampo(criar(corpo), "titulo");
        then(service).should(never()).criar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\",\"status\":\"PENDENTE\"}",
            "{\"titulo\":\"\",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\",\"status\":\"PENDENTE\"}",
            "{\"titulo\":\"   \",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\",\"status\":\"PENDENTE\"}"
    })
    void atualizacaoComTituloAusenteOuEmBrancoEhRejeitada(String corpo) throws Exception {
        erroNoCampo(atualizar(corpo), "titulo");
        then(service).should(never()).atualizar(any(), any());
    }

    @Test
    void tituloAcimaDoLimiteEhRejeitado() throws Exception {
        String titulo = "  " + "a".repeat(101) + "  ";

        erroNoCampo(criar("{\"titulo\":\"" + titulo + "\",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\"}"),
                "titulo");
    }

    @Test
    void tituloNoLimiteComEspacosNasExtremidadesEhAceitoSemOsEspacos() throws Exception {
        given(service.criar(any())).willReturn(resposta(TITULO_100));

        criar("{\"titulo\":\"   " + TITULO_100 + "  \",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\"}")
                .andExpect(status().isCreated());

        ArgumentCaptor<CriarTarefaRequest> captor = ArgumentCaptor.forClass(CriarTarefaRequest.class);
        then(service).should().criar(captor.capture());
        assertThat(captor.getValue().titulo()).isEqualTo(TITULO_100);
    }

    @Test
    void atualizacaoTambemRemoveEspacosDoTitulo() throws Exception {
        given(service.atualizar(eq(ID), any())).willReturn(resposta("Pagar conta"));

        atualizar("""
                {"titulo":"  Pagar conta ","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}
                """).andExpect(status().isOk());

        ArgumentCaptor<AtualizarTarefaRequest> captor = ArgumentCaptor.forClass(AtualizarTarefaRequest.class);
        then(service).should().atualizar(eq(ID), captor.capture());
        assertThat(captor.getValue().titulo()).isEqualTo("Pagar conta");
    }

    // --- vencimento ---

    @Test
    void criacaoSemVencimentoEhRejeitada() throws Exception {
        erroNoCampo(criar("{\"titulo\":\"Pagar conta\",\"prioridade\":\"ALTA\"}"), "dataVencimento");
    }

    @Test
    void atualizacaoSemVencimentoEhRejeitada() throws Exception {
        erroNoCampo(atualizar("{\"titulo\":\"Pagar conta\",\"prioridade\":\"ALTA\",\"status\":\"PENDENTE\"}"),
                "dataVencimento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"20/10/2026", "2026-02-30"})
    void vencimentoEmFormatoInvalidoEhRejeitado(String data) throws Exception {
        erroNoCampo(criar("{\"titulo\":\"Pagar conta\",\"dataVencimento\":\"" + data + "\",\"prioridade\":\"ALTA\"}"),
                "dataVencimento");
    }

    // --- prioridade ---

    @Test
    void criacaoSemPrioridadeEhRejeitada() throws Exception {
        erroNoCampo(criar("{\"titulo\":\"Pagar conta\",\"dataVencimento\":\"2026-10-20\"}"), "prioridade");
    }

    @Test
    void atualizacaoSemPrioridadeEhRejeitada() throws Exception {
        erroNoCampo(atualizar("{\"titulo\":\"Pagar conta\",\"dataVencimento\":\"2026-10-20\",\"status\":\"PENDENTE\"}"),
                "prioridade");
    }

    @Test
    void prioridadeForaDoEnumEhRejeitada() throws Exception {
        erroNoCampo(criar("{\"titulo\":\"Pagar conta\",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"URGENTE\"}"),
                "prioridade");
    }

    // --- status ---

    @Test
    void atualizacaoSemStatusEhRejeitada() throws Exception {
        erroNoCampo(atualizar("{\"titulo\":\"Pagar conta\",\"dataVencimento\":\"2026-10-20\",\"prioridade\":\"ALTA\"}"),
                "status");
    }

    @Test
    void atualizacaoComStatusInvalidoEhRejeitada() throws Exception {
        erroNoCampo(atualizar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"ATRASADA"}
                """), "status");
    }

    // --- campos do sistema e resposta ---

    @Test
    void criacaoIgnoraCamposControladosPeloSistemaERespondeComLocation() throws Exception {
        given(service.criar(any())).willReturn(resposta("Pagar conta"));

        criar("""
                {"id":"22222222-2222-2222-2222-222222222222","titulo":"Pagar conta","descricao":"Luz",
                 "dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA",
                 "dataConclusao":"2026-10-18","situacaoPrazo":"ATRASADA"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tarefas/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"));

        then(service).should().criar(new CriarTarefaRequest("Pagar conta", "Luz", LocalDate.of(2026, 10, 20),
                Prioridade.ALTA));
    }

    @Test
    void atualizacaoIgnoraCamposControladosPeloSistema() throws Exception {
        given(service.atualizar(eq(ID), any())).willReturn(resposta("Pagar conta"));

        atualizar("""
                {"id":"22222222-2222-2222-2222-222222222222","titulo":"Pagar conta",
                 "dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE",
                 "dataConclusao":"2026-10-18","situacaoPrazo":"ATRASADA"}
                """).andExpect(status().isOk());

        then(service).should().atualizar(ID, new AtualizarTarefaRequest("Pagar conta", null,
                LocalDate.of(2026, 10, 20), Prioridade.ALTA, StatusTarefa.PENDENTE));
    }
}
