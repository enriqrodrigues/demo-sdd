package br.com.demo.cadastro.shared;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorFalso())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void validacaoRetornaUmErroPorCampoPriorizandoObrigatorio() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","interno":{"valor":""}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros.length()").value(2))
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')].mensagem").value(contains("Campo obrigatório")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'interno.valor')].mensagem")
                        .value(contains("Campo obrigatório")));
    }

    @Test
    void corpoMalformadoRetornaValidacao() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido"));
    }

    @Test
    void negocioComCampoIncluiErroDoCampo() throws Exception {
        mvc.perform(post("/teste/negocio-com-campo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"))
                .andExpect(jsonPath("$.detail").value("Já existe"))
                .andExpect(jsonPath("$.erros[0].campo").value("email"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Já existe"));
    }

    @Test
    void negocioSemCampoNaoTemListaDeErros() throws Exception {
        mvc.perform(post("/teste/negocio-sem-campo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NEGADO"))
                .andExpect(jsonPath("$.erros").doesNotExist());
    }

    @RestController
    static class ControladorFalso {

        record Interno(@NotBlank(message = Mensagens.OBRIGATORIO) String valor) {}

        record Entrada(
                @NotBlank(message = Mensagens.OBRIGATORIO) @Size(min = 3, message = "Muito curto") String nome,
                @NotNull(message = Mensagens.OBRIGATORIO) @Valid Interno interno) {}

        @PostMapping("/teste/validacao")
        void validar(@Valid @RequestBody Entrada entrada) {}

        @PostMapping("/teste/negocio-com-campo")
        void negocioComCampo() {
            throw new NegocioException(HttpStatus.CONFLICT, "DUPLICADO", "Já existe", "email");
        }

        @PostMapping("/teste/negocio-sem-campo")
        void negocioSemCampo() {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "NEGADO", "Negado");
        }
    }
}
