package br.com.demo.todo.produtividade;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;

/**
 * Spec score-produtividade: Consulta do score semanal, Tarefas finalizadas na semana e Conclusão
 * no prazo › Vencimento editado após a conclusão. Consultas feitas na quarta-feira 2026-10-21
 * (semana de 2026-10-19 a 2026-10-25), fuso America/Sao_Paulo (UTC-3).
 */
class ScoreSemanalIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @Test
    void estruturaDaResposta() throws Exception {
        consultarScore()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-19"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-25"))
                .andExpect(jsonPath("$.concluidas").value(0))
                .andExpect(jsonPath("$.noPrazo").value(0))
                .andExpect(jsonPath("$.percentual").value(nullValue()))
                .andExpect(jsonPath("$.percentual").hasJsonPath());
    }

    @Test
    void conclusaoNoDomingoDaSemanaAnterior() throws Exception {
        concluirEm(criar("2026-10-25"), "2026-10-19T02:59:00Z"); // 2026-10-18 23:59 no fuso

        consultarScore().andExpect(jsonPath("$.concluidas").value(0));
    }

    @Test
    void conclusaoNaSegundaFeiraAMeiaNoite() throws Exception {
        concluirEm(criar("2026-10-25"), "2026-10-19T03:00:00Z"); // 2026-10-19 00:00 no fuso

        consultarScore().andExpect(jsonPath("$.concluidas").value(1));
    }

    @Test
    void limiteDaSemanaAvaliadoNoFusoConfigurado() throws Exception {
        concluirEm(criar("2026-10-25"), "2026-10-19T02:00:00Z"); // já segunda em UTC, domingo 23:00 no fuso

        consultarScore().andExpect(jsonPath("$.concluidas").value(0));
    }

    @Test
    void conclusaoNoDomingoDaSemanaCorrenteContaEASeguinteNao() throws Exception {
        concluirEm(criar("2026-10-30"), "2026-10-26T02:59:00Z"); // 2026-10-25 23:59 no fuso
        concluirEm(criar("2026-10-30"), "2026-10-26T03:00:00Z"); // 2026-10-26 00:00 no fuso

        relogio.ajustar(Instant.parse("2026-10-25T12:00:00Z"));
        consultarScore().andExpect(jsonPath("$.concluidas").value(1));
    }

    @Test
    void pendentesNaoContam() throws Exception {
        criar("2026-10-01"); // atrasada
        criar("2026-10-30");
        concluirEm(criar("2026-10-25"), "2026-10-20T15:00:00Z");

        consultarScore()
                .andExpect(jsonPath("$.concluidas").value(1))
                .andExpect(jsonPath("$.noPrazo").value(1))
                .andExpect(jsonPath("$.percentual").value(100.0));
    }

    @Test
    void tarefaReabertaSaiDoScore() throws Exception {
        String id = criar("2026-10-25");
        concluirEm(id, "2026-10-20T15:00:00Z");
        consultarScore().andExpect(jsonPath("$.concluidas").value(1));
        mvc.perform(post("/api/tarefas/{id}/reabrir", id)).andExpect(status().isOk());

        consultarScore()
                .andExpect(jsonPath("$.concluidas").value(0))
                .andExpect(jsonPath("$.noPrazo").value(0));
    }

    @Test
    void tarefaExcluidaSaiDoScore() throws Exception {
        String id = criar("2026-10-25");
        concluirEm(id, "2026-10-20T15:00:00Z");
        consultarScore().andExpect(jsonPath("$.concluidas").value(1));
        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());

        consultarScore()
                .andExpect(jsonPath("$.concluidas").value(0))
                .andExpect(jsonPath("$.noPrazo").value(0));
    }

    @Test
    void contaNoPrazoEPercentualArredondado() throws Exception {
        concluirEm(criar("2026-10-23"), "2026-10-21T12:00:00Z");  // antes do vencimento
        concluirEm(criar("2026-10-21"), "2026-10-22T02:59:00Z");  // 23:59 do dia do vencimento no fuso
        concluirEm(criar("2026-10-20"), "2026-10-21T12:00:00Z");  // depois do vencimento

        consultarScore()
                .andExpect(jsonPath("$.concluidas").value(3))
                .andExpect(jsonPath("$.noPrazo").value(2))
                .andExpect(jsonPath("$.percentual").value(66.67));
    }

    @Test
    void vencimentoEditadoAposAConclusao() throws Exception {
        String id = criar("2026-10-20");
        concluirEm(id, "2026-10-21T12:00:00Z");
        consultarScore().andExpect(jsonPath("$.noPrazo").value(0));

        mvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Tarefa","dataVencimento":"2026-10-22","prioridade":"MEDIA","version":1}
                                """))
                .andExpect(status().isOk());

        consultarScore()
                .andExpect(jsonPath("$.concluidas").value(1))
                .andExpect(jsonPath("$.noPrazo").value(1));
    }

    private ResultActions consultarScore() throws Exception {
        return mvc.perform(get("/api/produtividade/semana"));
    }

    private String criar(String vencimento) throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Tarefa","dataVencimento":"%s","prioridade":"MEDIA"}
                                """.formatted(vencimento)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }

    /** Conclui com o relógio no instante dado e volta ao instante padrão das consultas. */
    private void concluirEm(String id, String instante) throws Exception {
        relogio.ajustar(Instant.parse(instante));
        mvc.perform(post("/api/tarefas/{id}/concluir", id)).andExpect(status().isOk());
        relogio.ajustar(INSTANTE_PADRAO);
    }
}
