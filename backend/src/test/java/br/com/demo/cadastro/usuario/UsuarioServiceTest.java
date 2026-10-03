package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class UsuarioServiceTest extends IntegrationTest {

    @Autowired
    UsuarioService servico;

    @Test
    void cadastraComoPendenteComSenhaCriptografadaEEmailNormalizado() {
        String email = DadosTeste.emailUnico();

        Usuario usuario = servico.cadastrar(
                DadosTeste.novoUsuario("  " + email.toUpperCase() + " ", DadosTeste.cpfValido()));

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getEmail()).isEqualTo(email);
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.PENDENTE_ATIVACAO);
        String hash = jdbc.queryForObject("select senha_hash from usuario where id = ?", String.class, usuario.getId());
        assertThat(hash).startsWith("$2").isNotEqualTo(DadosTeste.SENHA);
        assertThat(servico.senhaConfere(usuario, DadosTeste.SENHA)).isTrue();
        assertThat(servico.senhaConfere(usuario, "Outra@123")).isFalse();
        assertThat(servico.senhaConfere(usuario, "Aa1!" + "é".repeat(40))).isFalse();
    }

    @Test
    void rejeitaEmailDuplicadoIgnorandoMaiusculas() {
        String email = DadosTeste.emailUnico();
        servico.cadastrar(DadosTeste.novoUsuario(email, DadosTeste.cpfValido()));

        assertThatThrownBy(() -> servico.cadastrar(DadosTeste.novoUsuario(email.toUpperCase(), DadosTeste.cpfValido())))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("EMAIL_JA_CADASTRADO");
                    assertThat(e.getCampo()).isEqualTo("email");
                });
    }

    @Test
    void rejeitaCpfDuplicado() {
        String cpf = DadosTeste.cpfValido();
        servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), cpf));

        assertThatThrownBy(() -> servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), cpf)))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("CPF_JA_CADASTRADO");
                    assertThat(e.getCampo()).isEqualTo("cpf");
                });
    }

    @Test
    void ativaUsuario() {
        Usuario usuario = servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), DadosTeste.cpfValido()));

        servico.ativar(usuario.getId());

        assertThat(servico.buscarPorId(usuario.getId())).get()
                .extracting(Usuario::getStatus).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    void atualizaSomenteContatoEPreservaDadosImutaveis() {
        Usuario original = servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), DadosTeste.cpfValido()));
        var novoEndereco = new EnderecoDados("20040002", "Rua da Assembleia", "S/N", "  ", "Centro", "Rio de Janeiro", "RJ");

        Usuario atualizado = servico.atualizarContato(original.getId(), "2133334444", novoEndereco);

        assertThat(atualizado.getTelefone()).isEqualTo("2133334444");
        assertThat(EnderecoDados.de(atualizado.getEndereco())).isEqualTo(
                new EnderecoDados("20040002", "Rua da Assembleia", "S/N", null, "Centro", "Rio de Janeiro", "RJ"));
        assertThat(atualizado.getNome()).isEqualTo(original.getNome());
        assertThat(atualizado.getCpf()).isEqualTo(original.getCpf());
        assertThat(atualizado.getEmail()).isEqualTo(original.getEmail());
        assertThat(atualizado.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(atualizado.getAtualizadoEm()).isAfterOrEqualTo(original.getAtualizadoEm());
    }

    @Test
    void buscaPorEmailIgnoraMaiusculasEEspacos() {
        String email = DadosTeste.emailUnico();
        servico.cadastrar(DadosTeste.novoUsuario(email, DadosTeste.cpfValido()));

        assertThat(servico.buscarPorEmail(" " + email.toUpperCase() + " ")).isPresent();
        assertThat(servico.buscarPorEmail(null)).isEmpty();
    }
}
