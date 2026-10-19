package br.com.demo.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import br.com.demo.todo.TesteIntegracao;

/**
 * Spec gestao-tarefas: Validação da prioridade, Campos desconhecidos rejeitados, campos somente
 * leitura e status ignorados na criação, e Formato padronizado de erro (vários campos, JSON malformado).
 */
class DesserializacaoTarefaIT extends TesteIntegracao {

    // --- Validação da prioridade

    @Test
    void prioridadeAusente() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25"}
                """).andExpect(status().isBadRequest()).andExpect(erroNoCampo("prioridade"));
    }

    @Test
    void prioridadeNumerica() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":0}
                """).andExpect(status().isBadRequest()).andExpect(erroNoCampo("prioridade"));
    }

    @Test
    void prioridadeNumericaEmTexto() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"2"}
                """).andExpect(status().isBadRequest()).andExpect(erroNoCampo("prioridade"));
    }

    @Test
    void prioridadeEmMinusculas() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"alta"}
                """).andExpect(status().isBadRequest()).andExpect(erroNoCampo("prioridade"));
    }

    // --- Campos desconhecidos rejeitados

    @Test
    void propriedadeDesconhecida() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","responsavel":"ana"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(erroNoCampo("responsavel"));

        assertThat(jdbc.queryForObject("select count(*) from tarefa", Integer.class)).isZero();
    }

    // --- Criar tarefa: campos do servidor ignorados

    @Test
    void statusEnviadoNaCriacaoEhIgnorado() throws Exception {
        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA","status":"CONCLUIDA"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()));
    }

    @Test
    void camposSomenteLeituraEnviadosNaCriacaoSaoIgnorados() throws Exception {
        String idEnviado = "11111111-1111-4111-8111-111111111111";

        criar("""
                {"titulo":"Pagar conta","dataVencimento":"2026-10-20","prioridade":"ALTA",
                 "id":"%s","concluidaEm":"2026-10-01T10:00:00Z","situacaoPrazo":"NO_PRAZO","version":7}
                """.formatted(idEnviado))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(idEnviado)))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()))
                .andExpect(jsonPath("$.situacaoPrazo").value("ATRASADA"))
                .andExpect(jsonPath("$.version").value(0));
    }

    // --- Formato padronizado de erro

    @Test
    void variosCamposInvalidos() throws Exception {
        criar("""
                {"dataVencimento":"2026-10-25","prioridade":"URGENTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros", hasSize(2)))
                .andExpect(erroNoCampo("titulo"))
                .andExpect(erroNoCampo("prioridade"));
    }

    @Test
    void jsonMalformado() throws Exception {
        criar("{\"titulo\": \"Pagar conta\", ")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    private ResultActions criar(String corpo) throws Exception {
        return mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static ResultMatcher erroNoCampo(String campo) {
        return jsonPath("$.erros[*].campo", hasItem(campo));
    }
}
