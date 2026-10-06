package br.demo.usuarios.compartilhado.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.demo.usuarios.compartilhado.TesteIntegracao;
import br.demo.usuarios.compartilhado.erro.CodigoCampo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tabela de casos compartilhada com o frontend ({@code contratos/validacao-casos.json}, AD-10): cada
 * entrada crua é normalizada pelo campo (AD-5) e validada pelo {@code Validator} do contexto, que deve
 * produzir zero violações ou exatamente uma, com o código esperado.
 */
@DisplayName("FR-2: regras de validação compartilhadas (contratos/validacao-casos.json)")
class ValidacaoCasosTest extends TesteIntegracao {

    /** 22h30 de 05/10/2026 em São Paulo, já 06/10 em UTC. */
    private static final Instant REFERENCIA = Instant.parse("2026-10-06T01:30:00Z");

    private static final File FIXTURE = new File("../contratos/validacao-casos.json");

    @Autowired
    Validator validator;

    @BeforeEach
    void fixarRelogio() {
        relogio.definir(REFERENCIA);
    }

    record Caso(String regra, String campo, JsonNode entrada, boolean valido, String codigo) {

        @Override
        public String toString() {
            String texto = entrada.toString();
            if (texto.length() > 40) {
                texto = texto.substring(0, 37) + "...(" + texto.length() + ")";
            }
            return regra + " | " + campo + " = " + texto + " -> " + (valido ? "válido" : codigo);
        }
    }

    static List<Caso> casos() {
        JsonNode raiz = JsonMapper.builder().build().readTree(FIXTURE);
        List<Caso> casos = new ArrayList<>();
        for (JsonNode no : raiz) {
            JsonNode codigo = no.get("codigo");
            casos.add(new Caso(no.get("regra").stringValue(), no.get("campo").stringValue(), no.get("entrada"),
                    no.get("valido").booleanValue(), codigo.isNull() ? null : codigo.stringValue()));
        }
        return casos;
    }

    @ParameterizedTest(name = "FR-2: {0}")
    @MethodSource("casos")
    @DisplayName("FR-2: cada caso do fixture produz exatamente o código esperado")
    void caso(Caso caso) {
        assertThat(caso.valido()).as("valido coerente com codigo").isEqualTo(caso.codigo() == null);

        Set<? extends ConstraintViolation<?>> violacoes = caso.entrada().isObject()
                ? validarSenhas(caso.entrada())
                : validarCampo(caso.campo(), texto(caso.entrada()));

        if (caso.valido()) {
            assertThat(violacoes).isEmpty();
        } else {
            assertThat(violacoes).hasSize(1);
            ConstraintViolation<?> violacao = violacoes.iterator().next();
            assertThat(violacao.getMessage()).isEqualTo(caso.codigo());
            assertThat(violacao.getPropertyPath().toString()).isEqualTo(propriedade(caso.campo()));
        }
    }

    @Test
    @DisplayName("FR-2: cada um dos 12 códigos de campo tem caso válido e inválido no fixture")
    void todosOsCodigosCobertos() {
        List<Caso> casos = casos();
        for (CodigoCampo codigo : CodigoCampo.values()) {
            assertThat(casos).as("caso válido de %s", codigo)
                    .anyMatch(c -> c.valido() && c.regra().equals(codigo.name()));
            assertThat(casos).as("caso inválido de %s", codigo)
                    .anyMatch(c -> !c.valido() && codigo.name().equals(c.codigo()));
        }
    }

    private Set<ConstraintViolation<Campos>> validarCampo(String campo, String entrada) {
        return validator.validateValue(Campos.class, propriedade(campo), normalizar(campo, entrada));
    }

    private Set<ConstraintViolation<Senhas>> validarSenhas(JsonNode entrada) {
        return validator.validate(new Senhas(texto(entrada.get("senha")), texto(entrada.get("confirmacaoSenha"))));
    }

    private static String texto(JsonNode no) {
        return no == null || no.isNull() ? null : no.stringValue();
    }

    private static String propriedade(String campo) {
        return campo.substring(campo.lastIndexOf('.') + 1);
    }

    /** A mesma normalização que os records de requisição aplicam no construtor compacto (AD-5). */
    private static String normalizar(String campo, String valor) {
        return switch (campo) {
            case "email" -> Normalizacao.email(valor);
            case "cpf", "telefone", "endereco.cep" -> Normalizacao.soDigitos(valor);
            case "endereco.complemento" -> Normalizacao.textoOpcional(valor);
            case "senha", "confirmacaoSenha" -> valor;
            case "nomeCompleto", "dataNascimento", "endereco.logradouro", "endereco.numero",
                    "endereco.bairro", "endereco.cidade", "endereco.uf" -> Normalizacao.texto(valor);
            default -> throw new IllegalArgumentException("Campo desconhecido no fixture: " + campo);
        };
    }

    /** Um campo de cada regra, com o nome do último segmento do caminho do fixture. */
    static class Campos {
        @Valida(RegraCampo.NOME_COMPLETO) String nomeCompleto;
        @Valida(RegraCampo.EMAIL) String email;
        @Valida(RegraCampo.CPF) String cpf;
        @Valida(RegraCampo.DATA_NASCIMENTO) String dataNascimento;
        @Valida(RegraCampo.TELEFONE) String telefone;
        @Valida(RegraCampo.CEP) String cep;
        @Valida(RegraCampo.LOGRADOURO) String logradouro;
        @Valida(RegraCampo.NUMERO) String numero;
        @Valida(RegraCampo.COMPLEMENTO) String complemento;
        @Valida(RegraCampo.BAIRRO) String bairro;
        @Valida(RegraCampo.CIDADE) String cidade;
        @Valida(RegraCampo.UF) String uf;
        @Valida(RegraCampo.SENHA) String senha;
        @Valida(RegraCampo.CONFIRMACAO_SENHA) String confirmacaoSenha;
    }

    /** Formato que os DTOs de cadastro vão usar: record com as constraints nos componentes. */
    @SenhasConferem
    record Senhas(@Valida(RegraCampo.SENHA) String senha,
                  @Valida(RegraCampo.CONFIRMACAO_SENHA) String confirmacaoSenha) implements ComConfirmacaoSenha {
    }
}
