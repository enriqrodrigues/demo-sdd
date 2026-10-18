package com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.demosdd.todo.RelogioAjustavel;
import com.demosdd.todo.RelogioDeTesteConfiguration;
import com.demosdd.todo.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RelogioDeTesteConfiguration.class})
class TarefaApiIntegrationTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 18);

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    RelogioAjustavel relogio;

    @Autowired
    TarefaRepository repository;

    @BeforeEach
    void prepararCenario() {
        repository.deleteAll();
        relogio.definirHoje(HOJE);
    }

    private ResultActions criar(String corpo) throws Exception {
        return mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String criarTarefa(String titulo, String vencimento, String prioridade) throws Exception {
        String corpo = criar("""
                {"titulo":"%s","dataVencimento":"%s","prioridade":"%s"}
                """.formatted(titulo, vencimento, prioridade))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }

    private ResultActions atualizar(String id, String corpo) throws Exception {
        return mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions alterarStatus(String id, String titulo, String status) throws Exception {
        return atualizar(id, """
                {"titulo":"%s","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"%s"}
                """.formatted(titulo, status));
    }

    // --- criação ---

    @Test
    void criacaoComDadosValidos() throws Exception {
        String corpo = criar("""
                {"titulo":"Pagar conta","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.titulo").value("Pagar conta"))
                .andExpect(jsonPath("$.descricao").value("Luz"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-20"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").isEmpty())
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"))
                .andReturn().getResponse().getContentAsString();

        String id = json.readTree(corpo).get("id").asText();
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isOk());
        criar("""
                {"titulo":"Outra","dataVencimento":"2026-10-20","prioridade":"BAIXA"}
                """).andExpect(header().string("Location", matchesPattern("http://localhost/api/tarefas/[0-9a-f-]{36}")));
    }

    @Test
    void criacaoSemDescricaoGuardaDescricaoNula() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.descricao").isEmpty());
    }

    @Test
    void criacaoIgnoraCamposControladosPeloSistema() throws Exception {
        String idEnviado = "22222222-2222-2222-2222-222222222222";

        criar("""
                {"id":"%s","titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA",
                 "status":"CONCLUIDA","dataConclusao":"2026-10-18","situacaoPrazo":"ATRASADA"}
                """.formatted(idEnviado))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(idEnviado)))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").isEmpty())
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"));

        assertThat(repository.existsById(UUID.fromString(idEnviado))).isFalse();
    }

    @Test
    void criacaoComVencimentoNoPassadoEhAceita() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-01","prioridade":"MEDIA"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacaoPrazo").value("ATRASADA"));
    }

    @Test
    void tituloNoLimiteEhArmazenadoSemOsEspacos() throws Exception {
        String titulo100 = "t".repeat(100);

        String id = criarTarefa("   " + titulo100 + "   ", "2026-10-20", "ALTA");

        assertThat(repository.findById(UUID.fromString(id)).orElseThrow().getTitulo()).isEqualTo(titulo100);
    }

    // --- listagem ---

    @Test
    void listagemOrdenaPorVencimentoEPrioridade() throws Exception {
        criarTarefa("A", "2026-10-20", "BAIXA");
        criarTarefa("B", "2026-10-19", "MEDIA");
        criarTarefa("C", "2026-10-20", "ALTA");

        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo", contains("B", "C", "A")))
                .andExpect(jsonPath("$[*].situacaoPrazo", contains("NO_PRAZO", "NO_PRAZO", "NO_PRAZO")));
    }

    @Test
    void listagemSemTarefasRetornaListaVazia() throws Exception {
        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- consulta ---

    @Test
    void consultaDeTarefaExistente() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-18", "ALTA");

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.situacaoPrazo").value("PARA_HOJE"));
    }

    @Test
    void consultaDeTarefaInexistenteGera404() throws Exception {
        mvc.perform(get("/api/tarefas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- atualização ---

    @Test
    void atualizacaoValidaSubstituiOsCampos() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");

        atualizar(id, """
                {"titulo":"Pagar conta de luz","descricao":"Vence no dia 25","dataVencimento":"2026-10-25",
                 "prioridade":"BAIXA","status":"PENDENTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Pagar conta de luz"))
                .andExpect(jsonPath("$.descricao").value("Vence no dia 25"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-25"))
                .andExpect(jsonPath("$.prioridade").value("BAIXA"))
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.titulo").value("Pagar conta de luz"));
    }

    @Test
    void atualizacaoDeTarefaInexistenteGera404() throws Exception {
        alterarStatus(UUID.randomUUID().toString(), "Pagar conta", "PENDENTE")
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizacaoIgnoraIdEDataDeConclusaoEnviados() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");

        atualizar(id, """
                {"id":"22222222-2222-2222-2222-222222222222","titulo":"Pagar conta","dataVencimento":"2026-10-20",
                 "prioridade":"ALTA","status":"PENDENTE","dataConclusao":"2026-10-01"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.dataConclusao").isEmpty());
    }

    // --- conclusão e reabertura ---

    @Test
    void concluirTarefaDefineADataDeConclusao() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");

        alterarStatus(id, "Pagar conta", "CONCLUIDA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-18"))
                .andExpect(jsonPath("$.situacaoPrazo").value("CONCLUIDA"));
    }

    @Test
    void editarTarefaJaConcluidaPreservaADataDeConclusao() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");
        relogio.definirHoje(LocalDate.of(2026, 10, 15));
        alterarStatus(id, "Pagar conta", "CONCLUIDA");

        relogio.definirHoje(LocalDate.of(2026, 10, 18));
        alterarStatus(id, "Pagar a conta de luz", "CONCLUIDA")
                .andExpect(jsonPath("$.titulo").value("Pagar a conta de luz"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-15"));
    }

    @Test
    void reabrirTarefaApagaADataDeConclusao() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");
        alterarStatus(id, "Pagar conta", "CONCLUIDA");

        alterarStatus(id, "Pagar conta", "PENDENTE")
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").isEmpty())
                .andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"));
    }

    @Test
    void concluirNovamenteAposReabrirUsaANovaData() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");
        relogio.definirHoje(LocalDate.of(2026, 10, 15));
        alterarStatus(id, "Pagar conta", "CONCLUIDA");
        alterarStatus(id, "Pagar conta", "PENDENTE");

        relogio.definirHoje(LocalDate.of(2026, 10, 18));
        alterarStatus(id, "Pagar conta", "CONCLUIDA")
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-18"));
    }

    // --- exclusão ---

    @Test
    void exclusaoDeTarefaExistente() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");

        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/tarefas")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void exclusaoDeTarefaInexistenteGera404() throws Exception {
        mvc.perform(delete("/api/tarefas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- situação de prazo ao longo do tempo ---

    @Test
    void situacaoMudaComAPassagemDoTempo() throws Exception {
        String id = criarTarefa("Pagar conta", "2026-10-20", "ALTA");

        relogio.definirHoje(LocalDate.of(2026, 10, 19));
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(jsonPath("$.situacaoPrazo").value("NO_PRAZO"));

        relogio.definirHoje(LocalDate.of(2026, 10, 20));
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(jsonPath("$.situacaoPrazo").value("PARA_HOJE"));

        relogio.definirHoje(LocalDate.of(2026, 10, 21));
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(jsonPath("$.situacaoPrazo").value("ATRASADA"));
        mvc.perform(get("/api/tarefas")).andExpect(jsonPath("$[0].situacaoPrazo").value("ATRASADA"));
    }
}
