package br.com.demo.cadastro.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class PerfilIntegrationTest extends IntegrationTest {

    private static final String ATUALIZACAO = """
            {"telefone":"2133334444",
             "endereco":{"cep":"20040002","logradouro":"Rua da Assembleia","numero":"S/N","complemento":"",
                         "bairro":"Centro","cidade":"Rio de Janeiro","uf":"RJ"}}
            """;

    private static RequestPostProcessor como(String id) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(id, null, List.of()));
    }

    private String usuarioAtivo(String email) throws Exception {
        String id = cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);
        return id;
    }

    @Test
    void retornaPerfilCompletoSemSenha() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = usuarioAtivo(email);

        mvc.perform(get("/api/perfil").with(como(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.cpf").isString())
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"))
                .andExpect(jsonPath("$.telefone").value("11987654321"))
                .andExpect(jsonPath("$.endereco.cep").value("01310100"))
                .andExpect(jsonPath("$.endereco.complemento").value("Apto 12"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void atualizaTelefoneEEndereco() throws Exception {
        String id = usuarioAtivo(DadosTeste.emailUnico());

        mvc.perform(put("/api/perfil").with(como(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ATUALIZACAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("2133334444"))
                .andExpect(jsonPath("$.endereco.cidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.endereco.complemento").value(org.hamcrest.Matchers.nullValue()));

        Map<String, Object> linha = jdbc.queryForMap(
                "select telefone, cep, complemento from usuario where id = ?::uuid", id);
        assertThat(linha).containsEntry("telefone", "2133334444").containsEntry("cep", "20040002");
        assertThat(linha.get("complemento")).isNull();
    }

    @Test
    void ignoraCamposImutaveisEnviadosNoCorpo() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = usuarioAtivo(email);
        Map<String, Object> antes = jdbc.queryForMap(
                "select nome, cpf, email, data_nascimento from usuario where id = ?::uuid", id);
        String corpo = ATUALIZACAO.replaceFirst("\\{",
                "{\"nome\":\"Outro Nome\",\"cpf\":\"52998224725\",\"email\":\"outro@teste.local\","
                        + "\"dataNascimento\":\"2000-01-01\",");

        mvc.perform(put("/api/perfil").with(como(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForMap("select nome, cpf, email, data_nascimento from usuario where id = ?::uuid", id))
                .isEqualTo(antes);
    }

    @Test
    void validaTelefoneEEnderecoObrigatorio() throws Exception {
        String id = usuarioAtivo(DadosTeste.emailUnico());

        mvc.perform(put("/api/perfil").with(como(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'telefone')].mensagem")
                        .value(org.hamcrest.Matchers.contains("Telefone inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'endereco')].mensagem")
                        .value(org.hamcrest.Matchers.contains("Campo obrigatório")));
    }

    @Test
    void exigeAutenticacaoECsrf() throws Exception {
        String id = usuarioAtivo(DadosTeste.emailUnico());

        mvc.perform(put("/api/perfil").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(ATUALIZACAO))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil").with(como(id)).contentType(MediaType.APPLICATION_JSON).content(ATUALIZACAO))
                .andExpect(status().isForbidden());
    }

    @Test
    void fluxoCompletoComSessaoReal() throws Exception {
        String email = DadosTeste.emailUnico();
        usuarioAtivo(email);
        MockHttpSession sessao = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, DadosTeste.SENHA)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/perfil").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
