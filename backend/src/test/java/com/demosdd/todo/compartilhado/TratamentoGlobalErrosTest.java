package com.demosdd.todo.compartilhado;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.demosdd.todo.tarefa.AtualizarTarefaRequest;
import com.demosdd.todo.tarefa.TarefaController;
import com.demosdd.todo.tarefa.TarefaNaoEncontradaException;
import com.demosdd.todo.tarefa.TarefaService;

@WebMvcTest(TarefaController.class)
class TratamentoGlobalErrosTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TarefaService service;

    private ResultActions postar(String corpo) throws Exception {
        return mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static void problemDetail(ResultActions resposta, int status) throws Exception {
        resposta.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    void variosCamposInvalidosGeramUmaEntradaPorCampo() throws Exception {
        ResultActions resposta = postar("""
                {"titulo":"","dataVencimento":"2026-10-20"}
                """);

        problemDetail(resposta, 400);
        resposta.andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].campo", containsInAnyOrder("titulo", "prioridade")))
                .andExpect(jsonPath("$.errors[*].mensagem", containsInAnyOrder(
                        "O título é obrigatório.", "A prioridade é obrigatória.")));
    }

    @Test
    void corpoJsonMalformadoGera400SemErrosPorCampo() throws Exception {
        ResultActions resposta = postar("{\"titulo\": \"Pagar conta\",");

        problemDetail(resposta, 400);
        resposta.andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void corpoAusenteGera400() throws Exception {
        problemDetail(mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON)), 400);
    }

    @Test
    void dataInvalidaIdentificaOCampo() throws Exception {
        ResultActions resposta = postar("""
                {"titulo":"Pagar conta","dataVencimento":"20/10/2026","prioridade":"ALTA"}
                """);

        problemDetail(resposta, 400);
        resposta.andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].campo").value("dataVencimento"));
    }

    @Test
    void enumInvalidoIdentificaOCampoEListaOsValoresAceitos() throws Exception {
        ResultActions resposta = postar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"URGENTE"}
                """);

        problemDetail(resposta, 400);
        resposta.andExpect(jsonPath("$.errors[0].campo").value("prioridade"))
                .andExpect(jsonPath("$.errors[0].mensagem").value(containsString("BAIXA, MEDIA, ALTA")));
    }

    @Test
    void enumInformadoComoNumeroEhRejeitado() throws Exception {
        ResultActions resposta = postar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":1}
                """);

        problemDetail(resposta, 400);
        resposta.andExpect(jsonPath("$.errors[0].campo").value("prioridade"));
    }

    @Test
    void identificadorEmFormatoInvalidoGera400() throws Exception {
        problemDetail(mvc.perform(get("/api/tarefas/abc")), 400);
    }

    @Test
    void tarefaInexistenteGera404() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.buscar(id)).willThrow(new TarefaNaoEncontradaException(id));

        ResultActions resposta = mvc.perform(get("/api/tarefas/{id}", id));

        problemDetail(resposta, 404);
        resposta.andExpect(jsonPath("$.title").value("Tarefa não encontrada"));
    }

    @Test
    void atualizacaoDeTarefaInexistenteGera404() throws Exception {
        UUID id = UUID.randomUUID();
        given(service.atualizar(any(UUID.class), any(AtualizarTarefaRequest.class)))
                .willThrow(new TarefaNaoEncontradaException(id));

        problemDetail(mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}
                """)), 404);
    }

    @Test
    void erroInesperadoGera500SemExporDetalhesInternos() throws Exception {
        given(service.listar()).willThrow(new IllegalStateException("segredo interno da conexão"));

        ResultActions resposta = mvc.perform(get("/api/tarefas"));

        problemDetail(resposta, 500);
        resposta.andExpect(content().string(not(containsString("segredo"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at com."))));
    }
}
