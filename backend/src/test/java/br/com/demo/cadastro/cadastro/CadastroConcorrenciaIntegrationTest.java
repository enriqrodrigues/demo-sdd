package br.com.demo.cadastro.cadastro;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Corrida real no banco: outra transacao insere (sem commit) o mesmo e-mail/CPF; o POST passa pelas verificacoes
 * existsBy... (a linha nao esta visivel), bloqueia no INSERT ate o commit e recebe a violacao da constraint unica.
 */
class CadastroConcorrenciaIntegrationTest extends IntegrationTest {

    @Autowired
    DataSource dataSource;

    @Test
    void corridaPeloMesmoEmailRetorna409NoCampoEmail() throws Exception {
        String email = DadosTeste.emailUnico();

        corrida(email, DadosTeste.cpfValido(), email, DadosTeste.cpfValido())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
                .andExpect(jsonPath("$.erros[0].campo").value("email"));
    }

    @Test
    void corridaPeloMesmoCpfRetorna409NoCampoCpf() throws Exception {
        String cpf = DadosTeste.cpfValido();

        corrida(DadosTeste.emailUnico(), cpf, DadosTeste.emailUnico(), cpf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CPF_JA_CADASTRADO"))
                .andExpect(jsonPath("$.erros[0].campo").value("cpf"));
    }

    private ResultActions corrida(String emailExistente, String cpfExistente, String emailNovo, String cpfNovo)
            throws Exception {
        UUID id = UUID.randomUUID();
        try (Connection outra = dataSource.getConnection()) {
            outra.setAutoCommit(false);
            try {
                inserir(outra, id, emailExistente, cpfExistente);
                CompletableFuture<ResultActions> resposta = CompletableFuture.supplyAsync(() -> {
                    try {
                        return mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                                .content(DadosTeste.cadastroJson(emailNovo, cpfNovo)));
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
                // so confirma depois que o INSERT do POST esta de fato bloqueado esperando o nosso commit
                await().atMost(Duration.ofSeconds(15)).until(() -> inserirBloqueado(), bloqueado -> bloqueado);
                outra.commit();
                return resposta.get(15, TimeUnit.SECONDS);
            } finally {
                outra.rollback();
            }
        } finally {
            jdbc.update("delete from usuario where id = ?", id);
        }
    }

    private boolean inserirBloqueado() {
        Integer total = jdbc.queryForObject("""
                select count(*) from pg_stat_activity
                where wait_event_type = 'Lock' and query ilike 'insert into usuario%'
                """, Integer.class);
        return total != null && total > 0;
    }

    private static void inserir(Connection c, UUID id, String email, String cpf) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("""
                insert into usuario (id, nome, cpf, email, data_nascimento, senha_hash, telefone, cep, logradouro,
                                     numero, bairro, cidade, uf, status, criado_em, atualizado_em)
                values (?, 'Concorrente', ?, ?, date '1990-05-20', 'hash', '11987654321', '01310100', 'Rua', '1',
                        'Centro', 'Cidade', 'SP', 'PENDENTE_ATIVACAO', now(), now())
                """)) {
            ps.setObject(1, id);
            ps.setString(2, cpf);
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }
}
