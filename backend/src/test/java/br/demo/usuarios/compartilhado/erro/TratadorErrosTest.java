package br.demo.usuarios.compartilhado.erro;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("AD-11: envelope de erro do TratadorErros")
class TratadorErrosTest {

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        var escritor = new EscritorErro(JsonMapper.builder().build());
        mvc = MockMvcBuilders.standaloneSetup(new ControladorTeste())
                .setControllerAdvice(new TratadorErros(escritor))
                .build();
    }

    @Test
    @DisplayName("AD-11, AD-16: ErroNegocio responde com seu status, código, mensagem e campos")
    void erroNegocioComCampos() throws Exception {
        mvc.perform(get("/teste/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("EMAIL_DUPLICADO"))
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"))
                .andExpect(jsonPath("$.campos", hasSize(1)))
                .andExpect(jsonPath("$.campos[0].campo").value("email"))
                .andExpect(jsonPath("$.campos[0].codigo").value("EMAIL_DUPLICADO"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("E-mail já cadastrado"));
    }

    @Test
    @DisplayName("AD-11: campos nulo vira [] e continua presente no envelope")
    void camposSemprePresente() throws Exception {
        mvc.perform(get("/teste/sem-campos"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CPF_DUPLICADO"))
                .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado"))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    @DisplayName("AD-11: MethodArgumentNotValidException vira 400 VALIDACAO com um campo por FieldError")
    void validacaoTraduzFieldErrors() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\" \",\"endereco\":{\"cep\":\"123\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.mensagem").value("Verifique os campos destacados."))
                .andExpect(jsonPath("$.campos", hasSize(3)))
                .andExpect(jsonPath("$.campos[*].campo",
                        containsInAnyOrder("nome", "endereco.cep", "endereco.uf")))
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')].codigo").value("CAMPO_OBRIGATORIO"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'nome')].mensagem").value("Campo obrigatório."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'endereco.cep')].codigo").value("CEP_INVALIDO"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'endereco.cep')].mensagem").value("CEP inválido."))
                .andExpect(jsonPath("$.campos[?(@.campo == 'endereco.uf')].codigo").value("CAMPO_OBRIGATORIO"));
    }

    @Test
    @DisplayName("AD-11: constraint com mensagem fora do catálogo é bug e vira 500 ERRO_INTERNO")
    void mensagemForaDoCatalogo() throws Exception {
        mvc.perform(post("/teste/validacao-sem-codigo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"longo demais\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    @DisplayName("AD-11: erro de validação sem campo (constraint de classe) é bug e vira 500 ERRO_INTERNO")
    void erroGlobalDeValidacao() throws Exception {
        mvc.perform(post("/teste/validacao-global").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":\"x\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    @DisplayName("AD-11: JSON malformado vira 400 REQUISICAO_INVALIDA")
    void jsonMalformado() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Requisição inválida."))
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    @DisplayName("AD-11: NoResourceFoundException vira 404 NAO_ENCONTRADO")
    void recursoInexistente() throws Exception {
        mvc.perform(get("/teste/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Recurso não encontrado."))
                .andExpect(jsonPath("$.campos", empty()));
    }

    @Test
    @DisplayName("AD-11: exceção inesperada vira 500 ERRO_INTERNO sem stack trace nem detalhe interno")
    void excecaoInesperada() throws Exception {
        mvc.perform(get("/teste/falha"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.campos", empty()))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("detalhe interno"))))
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("at br.demo"))));
    }

    record Endereco(@Pattern(regexp = "\\d{8}", message = "CEP_INVALIDO") String cep,
                    @NotNull(message = "CAMPO_OBRIGATORIO") String uf) {
    }

    record Formulario(@NotBlank String nome, @NotNull @Valid Endereco endereco) {
    }

    record FormularioSemCodigo(@Size(max = 3) String apelido) {
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = SempreInvalidoValidador.class)
    public @interface SempreInvalido {
        String message() default "CAMPO_OBRIGATORIO";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class SempreInvalidoValidador implements ConstraintValidator<SempreInvalido, Object> {
        @Override
        public boolean isValid(Object valor, ConstraintValidatorContext contexto) {
            return false;
        }
    }

    @SempreInvalido
    record FormularioGlobal(String valor) {
    }

    @RestController
    static class ControladorTeste {

        @GetMapping("/teste/duplicado")
        void duplicado() {
            throw new ErroNegocio("EMAIL_DUPLICADO", 409, "E-mail já cadastrado",
                    List.of(new CampoErro("email", "EMAIL_DUPLICADO", "E-mail já cadastrado")));
        }

        @GetMapping("/teste/sem-campos")
        void semCampos() {
            throw new ErroNegocio(CodigoErro.CPF_DUPLICADO, null);
        }

        @PostMapping("/teste/validacao")
        void validacao(@Valid @RequestBody Formulario formulario) {
        }

        @PostMapping("/teste/validacao-sem-codigo")
        void validacaoSemCodigo(@Valid @RequestBody FormularioSemCodigo formulario) {
        }

        @PostMapping("/teste/validacao-global")
        void validacaoGlobal(@Valid @RequestBody FormularioGlobal formulario) {
        }

        @GetMapping("/teste/inexistente")
        void inexistente() throws Exception {
            throw new NoResourceFoundException(HttpMethod.GET, "/teste/inexistente", "teste/inexistente");
        }

        @GetMapping("/teste/falha")
        void falha() {
            throw new RuntimeException("detalhe interno");
        }
    }
}
