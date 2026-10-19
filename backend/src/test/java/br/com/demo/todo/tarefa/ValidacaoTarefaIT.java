package br.com.demo.todo.tarefa;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.TesteIntegracao;

/**
 * Spec gestao-tarefas: Validação do título, da descrição e da data de vencimento, e
 * Criar tarefa › Vencimento no passado é aceito. A data corrente dos testes é 2026-10-21.
 */
class ValidacaoTarefaIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    // --- Validação do título

    @Test
    void tituloAusente() throws Exception {
        criar(semCampo("titulo")).andExpect(status().isBadRequest()).andExpect(erroNoCampo("titulo"));
    }

    @Test
    void tituloSoComEspacos() throws Exception {
        criar(com("titulo", "   ")).andExpect(status().isBadRequest()).andExpect(erroNoCampo("titulo"));
    }

    @Test
    void tituloCom100Caracteres() throws Exception {
        criar(com("titulo", "a".repeat(100))).andExpect(status().isCreated());
    }

    @Test
    void tituloCom101Caracteres() throws Exception {
        criar(com("titulo", "a".repeat(101))).andExpect(status().isBadRequest()).andExpect(erroNoCampo("titulo"));
    }

    @Test
    void espacosNasExtremidadesSaoRemovidos() throws Exception {
        criar(com("titulo", "  Revisar PR  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Revisar PR"));
    }

    // --- Validação da descrição

    @Test
    void descricaoCom2000Caracteres() throws Exception {
        criar(com("descricao", "d".repeat(2000))).andExpect(status().isCreated());
    }

    @Test
    void descricaoCom2001Caracteres() throws Exception {
        criar(com("descricao", "d".repeat(2001)))
                .andExpect(status().isBadRequest())
                .andExpect(erroNoCampo("descricao"));
    }

    @Test
    void descricaoVaziaViraNula() throws Exception {
        criar(com("descricao", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void descricaoSoComEspacosViraNula() throws Exception {
        criar(com("descricao", "   "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    // --- Validação da data de vencimento

    @Test
    void anoZeroNaoExisteNoCalendario() throws Exception {
        criar(com("dataVencimento", "0000-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(erroNoCampo("dataVencimento"));
    }

    @Test
    void vencimentoAusente() throws Exception {
        criar(semCampo("dataVencimento"))
                .andExpect(status().isBadRequest())
                .andExpect(erroNoCampo("dataVencimento"));
    }

    @Test
    void vencimentoVazioEhTratadoComoAusente() throws Exception {
        criar(com("dataVencimento", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'dataVencimento')].mensagem",
                        contains("Informe a data de vencimento.")));
    }

    @Test
    void vencimentoEmFormatoBrasileiro() throws Exception {
        criar(com("dataVencimento", "25/10/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(erroNoCampo("dataVencimento"));
    }

    @Test
    void vencimentoComHorario() throws Exception {
        criar(com("dataVencimento", "2026-10-25T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(erroNoCampo("dataVencimento"));
    }

    @Test
    void dataInexistente() throws Exception {
        criar(com("dataVencimento", "2026-02-30"))
                .andExpect(status().isBadRequest())
                .andExpect(erroNoCampo("dataVencimento"));
    }

    @Test
    void vencimentoNoPassadoEhAceito() throws Exception {
        criar(com("dataVencimento", "2026-10-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-01"));
    }

    // --- Agregação

    @Test
    void dataInvalidaNaoEscondeOutrosErros() throws Exception {
        Map<String, Object> corpo = semCampo("titulo");
        corpo.put("dataVencimento", "2026-02-30");

        criar(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros", hasSize(2)))
                .andExpect(erroNoCampo("titulo"))
                .andExpect(erroNoCampo("dataVencimento"));
    }

    @Test
    void erroDeCampoTrazMensagem() throws Exception {
        criar(semCampo("titulo"))
                .andExpect(jsonPath("$.erros[0].mensagem").isNotEmpty())
                .andExpect(jsonPath("$.erros[*].mensagem", not(hasItem(nullValue()))));
    }

    private static Map<String, Object> valido() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("titulo", "Pagar conta");
        corpo.put("dataVencimento", "2026-10-25");
        corpo.put("prioridade", "ALTA");
        return corpo;
    }

    private static Map<String, Object> com(String campo, Object valor) {
        Map<String, Object> corpo = valido();
        corpo.put(campo, valor);
        return corpo;
    }

    private static Map<String, Object> semCampo(String campo) {
        Map<String, Object> corpo = valido();
        corpo.remove(campo);
        return corpo;
    }

    private ResultActions criar(Map<String, Object> corpo) throws Exception {
        return mvc.perform(post("/api/tarefas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo)));
    }

    private static org.springframework.test.web.servlet.ResultMatcher erroNoCampo(String campo) {
        return jsonPath("$.erros[*].campo", hasItem(campo));
    }
}
