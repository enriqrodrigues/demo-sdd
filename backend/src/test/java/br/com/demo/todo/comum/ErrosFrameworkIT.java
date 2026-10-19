package br.com.demo.todo.comum;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.demo.todo.TesteIntegracao;

/** Design D7: erros do framework em Problem Details com textos em português e sem ecoar a entrada. */
class ErrosFrameworkIT extends TesteIntegracao {

    @Test
    void rotaInexistente() throws Exception {
        mvc.perform(get("/api/nada"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(content().string(not(containsString("static resource"))));
    }

    @Test
    void metodoNaoSuportado() throws Exception {
        mvc.perform(patch("/api/tarefas"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Método não permitido"));
    }

    @Test
    void tipoDeConteudoNaoSuportado() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Tipo de conteúdo não suportado"));
    }

    @Test
    void identificadorMalformadoNaoEcoaOValor() throws Exception {
        // O "instance" do Problem Details identifica a requisição (o caminho) por definição da RFC 9457;
        // o que não pode repetir a entrada são as mensagens.
        mvc.perform(get("/api/tarefas/{id}", "valor-ecoado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("id"))
                .andExpect(jsonPath("$.erros[0].mensagem", not(containsString("valor-ecoado"))))
                .andExpect(jsonPath("$.detail", not(containsString("valor-ecoado"))));
    }

    @Test
    void filtroInvalidoNaoEcoaOValor() throws Exception {
        mvc.perform(get("/api/tarefas").param("status", "valor-ecoado"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("valor-ecoado"))));
    }
}
