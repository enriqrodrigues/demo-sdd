package br.com.demo.cadastro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@RecordApplicationEvents
class CadastroIntegrationTest extends IntegrationTest {

    @Autowired
    ApplicationEvents eventos;

    private ResultActions postar(String json) throws Exception {
        return mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cadastraUsuarioPendenteEPublicaEvento() throws Exception {
        String email = DadosTeste.emailUnico();

        postar(DadosTeste.cadastroJson(email.toUpperCase(), DadosTeste.cpfValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("PENDENTE_ATIVACAO"));

        Map<String, Object> linha = jdbc.queryForMap("select status, senha_hash from usuario where email = ?", email);
        assertThat(linha.get("status")).isEqualTo("PENDENTE_ATIVACAO");
        assertThat((String) linha.get("senha_hash")).startsWith("$2").isNotEqualTo(DadosTeste.SENHA);
        assertThat(eventos.stream(UsuarioCadastrado.class))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.email()).isEqualTo(email);
                    assertThat(e.nome()).isEqualTo("Maria da Silva");
                });
    }

    @Test
    void complementoEhOpcional() throws Exception {
        String json = DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())
                .replace("\"complemento\":\"Apto 12\",", "");
        postar(json).andExpect(status().isCreated());
    }

    @Test
    void corpoVazioListaTodosOsCamposObrigatorios() throws Exception {
        postar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[*].campo").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "nome", "cpf", "email", "dataNascimento", "senha", "telefone", "endereco")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'cpf')].mensagem").value(contains("Campo obrigatório")));
    }

    @Test
    void formatosInvalidosRetornamMensagensDaSpec() throws Exception {
        String json = """
                {"nome":"Maria","cpf":"12345678900","email":"invalido","dataNascimento":"2999-01-01",
                 "senha":"fraca","telefone":"123",
                 "endereco":{"cep":"1","logradouro":"Rua","numero":"1","bairro":"Centro","cidade":"Cidade","uf":"XX"}}
                """;
        postar(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'cpf')].mensagem").value(contains("CPF inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'email')].mensagem").value(contains("E-mail inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'dataNascimento')].mensagem")
                        .value(contains("Data de nascimento inválida")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'senha')].mensagem").value(contains(
                        "A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'telefone')].mensagem").value(contains("Telefone inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'endereco.cep')].mensagem").value(contains("CEP inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'endereco.uf')].mensagem").value(contains("UF inválida")));
    }

    @Test
    void dataImpossivelOuJsonMalformadoRetorna400() throws Exception {
        String json = DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())
                .replace("1990-05-20", "2020-13-45");
        postar(json).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        postar("{\"nome\":").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void senhaAcimaDe72BytesRetorna400() throws Exception {
        String json = DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())
                .replace(DadosTeste.SENHA, "Aa1!" + "é".repeat(40));
        postar(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'senha')].mensagem").value(contains("A senha é longa demais")));
    }

    @Test
    void emailDuplicadoComOutraCaixaRetorna409NoCampoEmail() throws Exception {
        String email = DadosTeste.emailUnico();
        postar(DadosTeste.cadastroJson(email, DadosTeste.cpfValido())).andExpect(status().isCreated());

        postar(DadosTeste.cadastroJson(email.toUpperCase(), DadosTeste.cpfValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
                .andExpect(jsonPath("$.erros[0].campo").value("email"));
    }

    @Test
    void cpfDuplicadoRetorna409NoCampoCpf() throws Exception {
        String cpf = DadosTeste.cpfValido();
        postar(DadosTeste.cadastroJson(DadosTeste.emailUnico(), cpf)).andExpect(status().isCreated());

        postar(DadosTeste.cadastroJson(DadosTeste.emailUnico(), cpf))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CPF_JA_CADASTRADO"))
                .andExpect(jsonPath("$.erros[0].campo").value("cpf"));
    }

    @Test
    void semTokenCsrfRetorna403() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())))
                .andExpect(status().isForbidden());
    }

    /**
     * csrf() troca de forma permanente o repositorio de token do filtro compartilhado por um baseado em sessao.
     * Aqui restauramos o repositorio de cookie real, para validar o fluxo de producao independente da ordem dos testes.
     */
    private static RequestPostProcessor repositorioDeCookieReal() {
        return request -> {
            WebTestUtils.setCsrfTokenRepository(request, CookieCsrfTokenRepository.withHttpOnlyFalse());
            return request;
        };
    }

    @Test
    void aceitaTokenCsrfObtidoEmApiCsrf() throws Exception {
        Cookie cookie = mvc.perform(get("/api/csrf").with(repositorioDeCookieReal()))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/usuarios").with(repositorioDeCookieReal())
                        .cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())))
                .andExpect(status().isCreated());
    }
}
