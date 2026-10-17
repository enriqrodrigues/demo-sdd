package br.com.demosdd.todo.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class TratadorGlobalErrosTest {

    @Test
    void erroInesperadoNaoExpoeDetalhesInternos() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/tarefas"));

        ResponseEntity<ProblemDetail> resposta = new TratadorGlobalErros()
                .tratarErroInesperado(new IllegalStateException("senha do banco"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        ProblemDetail corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.getTitle()).isEqualTo("Erro interno");
        assertThat(corpo.getDetail()).isEqualTo("Ocorreu um erro inesperado.");
        assertThat(corpo.getInstance()).hasToString("/api/tarefas");
    }
}
