package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.demo.cadastro.shared.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class TraducaoViolacaoTest {

    private static DataIntegrityViolationException violacao(String constraint) {
        return new DataIntegrityViolationException("falha",
                new RuntimeException("duplicate key value violates unique constraint \"" + constraint + "\""));
    }

    @Test
    void violacaoDeEmailViraConflitoNoCampoEmail() {
        NegocioException e = UsuarioService.traduzirViolacao(violacao("uk_usuario_email"));
        assertThat(e.getCodigo()).isEqualTo("EMAIL_JA_CADASTRADO");
        assertThat(e.getCampo()).isEqualTo("email");
    }

    @Test
    void violacaoDeCpfViraConflitoNoCampoCpf() {
        NegocioException e = UsuarioService.traduzirViolacao(violacao("uk_usuario_cpf"));
        assertThat(e.getCodigo()).isEqualTo("CPF_JA_CADASTRADO");
        assertThat(e.getCampo()).isEqualTo("cpf");
    }
}
