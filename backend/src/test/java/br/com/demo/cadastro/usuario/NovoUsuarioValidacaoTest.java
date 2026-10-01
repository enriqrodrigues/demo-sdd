package br.com.demo.cadastro.usuario;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.demo.cadastro.shared.Mensagens;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NovoUsuarioValidacaoTest {

    private static Validator validator;

    @BeforeAll
    static void criarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static EnderecoDados enderecoValido() {
        return new EnderecoDados("01310100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP");
    }

    private static Map<String, Set<String>> mensagens(Object alvo) {
        return validator.validate(alvo).stream().collect(groupingBy(
                v -> v.getPropertyPath().toString(), mapping(ConstraintViolation::getMessage, toSet())));
    }

    @Test
    void dadosValidosSemComplementoNaoGeramViolacoes() {
        var usuario = new NovoUsuario("Maria da Silva", "52998224725", "maria@teste.local",
                LocalDate.of(1990, 5, 20), "Senha@123", "11987654321", enderecoValido());
        assertThat(validator.validate(usuario)).isEmpty();
    }

    @Test
    void camposAusentesSaoObrigatorios() {
        var usuario = new NovoUsuario(null, null, null, null, null, null, null);
        assertThat(mensagens(usuario))
                .containsEntry("nome", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("cpf", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("email", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("dataNascimento", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("senha", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("telefone", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("endereco", Set.of(Mensagens.OBRIGATORIO));
    }

    @Test
    void reportaCadaRegraComAMensagemDaSpec() {
        var usuario = new NovoUsuario("Maria", "12345678900", "invalido", LocalDate.now().plusDays(1),
                "fraca", "123", new EnderecoDados("1", "Rua", "1", null, "Centro", "Cidade", "XX"));
        assertThat(mensagens(usuario))
                .containsEntry("cpf", Set.of("CPF inválido"))
                .containsEntry("email", Set.of("E-mail inválido"))
                .containsEntry("dataNascimento", Set.of("Data de nascimento inválida"))
                .containsEntry("senha", Set.of(Senha.MENSAGEM_FRACA))
                .containsEntry("telefone", Set.of("Telefone inválido"))
                .containsEntry("endereco.cep", Set.of("CEP inválido"))
                .containsEntry("endereco.uf", Set.of("UF inválida"));
    }

    @Test
    void enderecoAusenteNosCamposObrigatorios() {
        var endereco = new EnderecoDados(null, null, null, null, null, null, null);
        assertThat(mensagens(endereco)).containsOnlyKeys("cep", "logradouro", "numero", "bairro", "cidade", "uf");
    }

    @Test
    void senhaAcimaDe72BytesTemMensagemPropria() {
        var usuario = new NovoUsuario("Maria da Silva", "52998224725", "maria@teste.local",
                LocalDate.of(1990, 5, 20), "Aa1!" + "é".repeat(40), "11987654321", enderecoValido());
        assertThat(mensagens(usuario)).containsEntry("senha", Set.of(Senha.MENSAGEM_LONGA));
    }
}
