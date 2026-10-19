package br.com.demo.todo.tarefa;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.demo.todo.ControladorFalhaTeste;
import br.com.demo.todo.TesteIntegracao;

/**
 * Spec gestao-tarefas: Editar tarefa; Concorrência otimista na edição (Versão ausente, Duas
 * edições simultâneas); Formato padronizado de erro › Conflito de versão.
 */
class EditarTarefaIT extends TesteIntegracao {

    @Autowired
    private ObjectMapper json;

    @Test
    void edicaoValida() throws Exception {
        String id = criar();

        editar(id, """
                {"titulo":"Pagar conta de luz","descricao":"até as 18h","dataVencimento":"2026-10-30",
                 "prioridade":"BAIXA","version":0}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Pagar conta de luz"))
                .andExpect(jsonPath("$.descricao").value("até as 18h"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-30"))
                .andExpect(jsonPath("$.prioridade").value("BAIXA"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.titulo").value("Pagar conta de luz"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void statusEnviadoNaEdicaoEhIgnorado() throws Exception {
        String id = criar();

        editar(id, """
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0,
                 "status":"CONCLUIDA","concluidaEm":"2026-10-21T10:00:00Z","id":"%s","situacaoPrazo":"CONCLUIDA"}
                """.formatted(UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()));
    }

    @Test
    void edicaoInvalida() throws Exception {
        String id = criar();

        editar(id, """
                {"titulo":"%s","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0}
                """.formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("titulo")));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.titulo").value("Pagar conta"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void edicaoDeTarefaInexistente() throws Exception {
        editar(UUID.randomUUID().toString(), """
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0}
                """)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void edicaoComPropriedadeDesconhecida() throws Exception {
        String id = criar();

        editar(id, """
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0,"responsavel":"ana"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("responsavel")));
    }

    @Test
    void tarefaConcluidaPodeSerEditadaSemPerderAConclusao() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into tarefa (id, titulo, data_vencimento, prioridade, status, concluida_em, version)
                values (?, 'Feita', date '2026-10-20', 'ALTA', 'CONCLUIDA', timestamptz '2026-10-20T15:00:00Z', 4)
                """, id);

        editar(id.toString(), """
                {"titulo":"Feita e revisada","dataVencimento":"2026-10-22","prioridade":"ALTA","version":4}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Feita e revisada"))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-20T12:00:00-03:00"))
                .andExpect(jsonPath("$.version").value(5));
    }

    // --- Concorrência otimista

    @Test
    void versaoAusente() throws Exception {
        String id = criar();

        editar(id, """
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("version")));
    }

    @Test
    void duasEdicoesSimultaneas() throws Exception {
        String id = criar();

        editar(id, """
                {"titulo":"Edição do cliente A","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0}
                """).andExpect(status().isOk());
        editar(id, """
                {"titulo":"Edição do cliente B","dataVencimento":"2026-10-25","prioridade":"ALTA","version":0}
                """)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.titulo").value("Edição do cliente A"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void corridaResidualDeVersaoNoCommitTambemViraConflito() throws Exception {
        mvc.perform(get(ControladorFalhaTeste.CAMINHO_CORRIDA_DE_VERSAO))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    private String criar() throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }

    private ResultActions editar(String id, String corpo) throws Exception {
        return mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
