package br.com.demosdd.todo.tarefa;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import br.com.demosdd.todo.IntegracaoBase;

class TarefaAtualizacaoIntegrationTest extends IntegracaoBase {

    private static final String PENDENTE = """
            {"titulo":"Pagar boleto","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA"}""";

    private ResultActions atualizar(String id, String corpo) throws Exception {
        return mockMvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void atualizaTodosOsCampos() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"Pagar fatura","descricao":"Cartão","dataVencimento":"2026-10-14","prioridade":"BAIXA","status":"PENDENTE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pagar fatura"))
                .andExpect(jsonPath("$.descricao").value("Cartão"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-14"))
                .andExpect(jsonPath("$.prioridade").value("BAIXA"))
                .andExpect(jsonPath("$.situacao").value("PARA_HOJE"));
    }

    @Test
    void concluirGravaDataDeHoje() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"Pagar boleto","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-14"))
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
    }

    @Test
    void reabrirLimpaDataConclusao() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""");

        atualizar(id, """
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").value(nullValue()))
                .andExpect(jsonPath("$.situacao").value("NO_PRAZO"));
    }

    @Test
    void editarConcluidaMantemDataConclusaoOriginal() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""");
        clock.definirHoje(LocalDate.of(2026, 10, 16));

        atualizar(id, """
                {"titulo":"Feita (corrigido)","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Feita (corrigido)"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-14"));
    }

    @Test
    void exigeStatusNoPut() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"Pagar boleto","dataVencimento":"2026-10-20","prioridade":"ALTA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("status"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("é obrigatório"));
    }

    @Test
    void validaCamposNoPut() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"));
    }

    @Test
    void retorna404AoAtualizarInexistente() throws Exception {
        atualizar("6f1c2a7e-0000-4000-8000-000000000000", """
                {"titulo":"X","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}""")
                .andExpect(status().isNotFound());
    }
}
