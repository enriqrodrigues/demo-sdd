package br.com.demo.todo.comum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import br.com.demo.todo.TesteIntegracao;

class CorsIT extends TesteIntegracao {

    @Test
    void preflightDaOrigemDoViteEhAceito() throws Exception {
        mvc.perform(options("/api/tarefas")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void preflightDeOutraOrigemEhRecusado() throws Exception {
        mvc.perform(options("/api/tarefas")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
