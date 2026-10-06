package br.demo.usuarios.compartilhado.validacao;

import br.demo.usuarios.compartilhado.erro.CodigoCampo;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;

/**
 * Validador de {@link Valida}: aplica as regras de §4.2 do API-CONTRACT (AD-10) e emite no máximo um
 * {@link CodigoCampo}. Criado pela fábrica de validadores do Spring, que injeta o {@link Clock} e o
 * fuso {@code app.fuso}; "hoje" é a data nesse fuso (AD-14).
 *
 * <p>As expressões regulares são as mesmas de {@code frontend/src/validacao/regras.ts}: letra é
 * {@code \p{L}}, dígito de senha é {@code \p{N}}, {@code \d} é só ASCII e espaço em branco é
 * {@code \p{IsWhite_Space}}.
 */
public class ValidadorCampo implements ConstraintValidator<Valida, String> {

    private static final Pattern EM_BRANCO = Pattern.compile("\\p{IsWhite_Space}*");
    private static final Pattern NOME = Pattern.compile("[\\p{L}'-]+( [\\p{L}'-]+)+");
    private static final Pattern EMAIL =
            Pattern.compile("[^\\p{IsWhite_Space}@]+@[^\\p{IsWhite_Space}@]+\\.[^\\p{IsWhite_Space}@]+");
    private static final Pattern ONZE_DIGITOS = Pattern.compile("\\d{11}");
    private static final Pattern DATA_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TELEFONE = Pattern.compile("[1-9]{2}(\\d{8}|9\\d{8})");
    private static final Pattern CEP = Pattern.compile("\\d{8}");
    private static final Pattern MAIUSCULA = Pattern.compile("\\p{Lu}");
    private static final Pattern MINUSCULA = Pattern.compile("\\p{Ll}");
    private static final Pattern DIGITO = Pattern.compile("\\p{N}");
    private static final Pattern ESPECIAL = Pattern.compile("[^\\p{L}\\p{N}\\p{IsWhite_Space}]");

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP",
            "SE", "TO");

    private static final int NOME_MAXIMO = 150;
    private static final int EMAIL_MAXIMO = 254;
    private static final int SENHA_MINIMO = 8;
    private static final int SENHA_MAXIMO = 64;

    private final Clock clock;
    private final ZoneId fuso;
    private RegraCampo regra;

    public ValidadorCampo(Clock clock, @Value("${app.fuso}") ZoneId fuso) {
        this.clock = clock;
        this.fuso = fuso;
    }

    @Override
    public void initialize(Valida anotacao) {
        this.regra = anotacao.value();
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        CodigoCampo codigo = validar(regra, valor);
        if (codigo == null) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(codigo.name()).addConstraintViolation();
        return false;
    }

    /** Código da primeira regra violada, ou {@code null} quando o valor (já normalizado) é válido. */
    CodigoCampo validar(RegraCampo regra, String valor) {
        if (valor == null || EM_BRANCO.matcher(valor).matches()) {
            return regra == RegraCampo.COMPLEMENTO ? null : CodigoCampo.CAMPO_OBRIGATORIO;
        }
        return switch (regra) {
            case NOME_COMPLETO -> valor.length() <= NOME_MAXIMO && NOME.matcher(valor).matches()
                    ? null : CodigoCampo.NOME_INVALIDO;
            case EMAIL -> valor.length() <= EMAIL_MAXIMO && EMAIL.matcher(valor).matches()
                    ? null : CodigoCampo.EMAIL_INVALIDO;
            case CPF -> cpfValido(valor) ? null : CodigoCampo.CPF_INVALIDO;
            case DATA_NASCIMENTO -> dataNascimentoValida(valor) ? null : CodigoCampo.DATA_NASCIMENTO_INVALIDA;
            case TELEFONE -> TELEFONE.matcher(valor).matches() ? null : CodigoCampo.TELEFONE_INVALIDO;
            case CEP -> CEP.matcher(valor).matches() ? null : CodigoCampo.CEP_INVALIDO;
            case UF -> UFS.contains(valor) ? null : CodigoCampo.UF_INVALIDA;
            case LOGRADOURO, BAIRRO, CIDADE, NUMERO, COMPLEMENTO ->
                    valor.length() <= regra.tamanhoMaximo() ? null : CodigoCampo.TAMANHO_EXCEDIDO;
            case SENHA -> senha(valor);
            case CONFIRMACAO_SENHA -> null;
        };
    }

    private static boolean cpfValido(String cpf) {
        if (!ONZE_DIGITOS.matcher(cpf).matches() || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
                && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
    }

    /** Dígito verificador por módulo 11 sobre os {@code quantidade} primeiros dígitos. */
    private static int digitoVerificador(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private boolean dataNascimentoValida(String valor) {
        if (!DATA_ISO.matcher(valor).matches()) {
            return false;
        }
        LocalDate data;
        try {
            data = LocalDate.parse(valor, FORMATO_DATA);
        } catch (DateTimeParseException e) {
            return false;
        }
        LocalDate hoje = LocalDate.ofInstant(clock.instant(), fuso);
        return data.isBefore(hoje);
    }

    private static CodigoCampo senha(String senha) {
        if (senha.length() > SENHA_MAXIMO) {
            return CodigoCampo.SENHA_LONGA;
        }
        boolean forte = senha.length() >= SENHA_MINIMO
                && MAIUSCULA.matcher(senha).find()
                && MINUSCULA.matcher(senha).find()
                && DIGITO.matcher(senha).find()
                && ESPECIAL.matcher(senha).find();
        return forte ? null : CodigoCampo.SENHA_FRACA;
    }
}
