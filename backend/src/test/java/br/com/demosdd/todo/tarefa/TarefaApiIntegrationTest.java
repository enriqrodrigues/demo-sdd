package br.com.demosdd.todo.tarefa;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import br.com.demosdd.todo.IntegracaoBase;

class TarefaApiIntegrationTest extends IntegracaoBase {

    private ResultActions postar(String corpo) throws Exception {
        return mockMvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void criaTarefaPendentePorPadrao() throws Exception {
        postar("""
                {"titulo":"Pagar boleto","descricao":"Conta de luz","dataVencimento":"2026-10-14","prioridade":"ALTA"}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/tarefas/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Pagar boleto"))
                .andExpect(jsonPath("$.descricao").value("Conta de luz"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-14"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").value(nullValue()))
                .andExpect(jsonPath("$.situacao").value("PARA_HOJE"));
    }

    @Test
    void criaTarefaJaConcluidaComDataDeHoje() throws Exception {
        postar("""
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"BAIXA","status":"CONCLUIDA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-14"))
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
    }

    @Test
    void ignoraCamposSomenteLeituraEnviadosPeloCliente() throws Exception {
        postar("""
                {"id":"00000000-0000-0000-0000-000000000001","titulo":"Futura","dataVencimento":"2026-10-20",
                 "prioridade":"MEDIA","dataConclusao":"2020-01-01","situacao":"ATRASADA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not("00000000-0000-0000-0000-000000000001")))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").value(nullValue()))
                .andExpect(jsonPath("$.situacao").value("NO_PRAZO"));
    }

    @Test
    void normalizaTituloEDescricao() throws Exception {
        postar("""
                {"titulo":"  Estudar  ","descricao":"   ","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Estudar"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void aceitaTituloCom100CaracteresAcentuados() throws Exception {
        String titulo = "á".repeat(100);
        postar("""
                {"titulo":"%s","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""".formatted(titulo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value(titulo));
    }

    @Test
    void rejeitaTituloCom101Caracteres() throws Exception {
        postar("""
                {"titulo":"%s","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""".formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos."))
                .andExpect(jsonPath("$.instance").value("/api/tarefas"))
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("deve ter no máximo 100 caracteres"));
    }

    @Test
    void rejeitaTituloSoComEspacos() throws Exception {
        postar("""
                {"titulo":"   ","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("é obrigatório"));
    }

    @Test
    void listaErrosDeTodosOsCamposObrigatorios() throws Exception {
        postar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("titulo", "dataVencimento", "prioridade")));
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        postar("{\"titulo\":")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou malformado."));
    }

    @Test
    void rejeitaPrioridadeInvalida() throws Exception {
        postar("""
                {"titulo":"X","dataVencimento":"2026-10-20","prioridade":"URGENTE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o campo 'prioridade'."));
    }

    @Test
    void rejeitaDataInexistente() throws Exception {
        postar("""
                {"titulo":"X","dataVencimento":"2026-02-30","prioridade":"MEDIA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o campo 'dataVencimento'."));
    }

    @Test
    void buscaTarefaPorId() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Ler","dataVencimento":"2026-10-10","prioridade":"BAIXA"}""");

        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.situacao").value("ATRASADA"));
    }

    @Test
    void retorna404ParaTarefaInexistente() throws Exception {
        mockMvc.perform(get("/api/tarefas/{id}", "6f1c2a7e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Tarefa não encontrada: 6f1c2a7e-0000-4000-8000-000000000000"));
    }

    @Test
    void retorna400ParaIdInvalido() throws Exception {
        mockMvc.perform(get("/api/tarefas/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("Valor inválido para 'id': abc"));
    }

    @Test
    void listaTarefasNaOrdemComSituacaoCalculada() throws Exception {
        criarTarefa("""
                {"titulo":"Concluída","dataVencimento":"2026-10-01","prioridade":"BAIXA","status":"CONCLUIDA"}""");
        criarTarefa("""
                {"titulo":"No prazo","dataVencimento":"2026-10-20","prioridade":"ALTA"}""");
        criarTarefa("""
                {"titulo":"Hoje","dataVencimento":"2026-10-14","prioridade":"MEDIA"}""");
        criarTarefa("""
                {"titulo":"Atrasada","dataVencimento":"2026-10-10","prioridade":"ALTA"}""");

        mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Atrasada"))
                .andExpect(jsonPath("$[0].situacao").value("ATRASADA"))
                .andExpect(jsonPath("$[1].situacao").value("PARA_HOJE"))
                .andExpect(jsonPath("$[2].situacao").value("NO_PRAZO"))
                .andExpect(jsonPath("$[3].situacao").value("CONCLUIDA"));
    }

    @Test
    void calculaHojeNoFusoDeSaoPaulo() throws Exception {
        // 01:30 UTC do dia 15 = 22:30 do dia 14 em São Paulo
        clock.definirInstante(Instant.parse("2026-10-15T01:30:00Z"));
        String id = criarTarefa("""
                {"titulo":"Vence hoje","dataVencimento":"2026-10-14","prioridade":"MEDIA"}""");

        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.situacao").value("PARA_HOJE"));
    }

    @Test
    void excluiTarefa() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Descartável","dataVencimento":"2026-10-20","prioridade":"BAIXA"}""");

        mockMvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void retorna404AoExcluirInexistente() throws Exception {
        mockMvc.perform(delete("/api/tarefas/{id}", "6f1c2a7e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void liberaCorsParaOFrontend() throws Exception {
        mockMvc.perform(options("/api/tarefas")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void recusaCorsDeOutraOrigem() throws Exception {
        mockMvc.perform(options("/api/tarefas")
                        .header("Origin", "http://outro.exemplo")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
