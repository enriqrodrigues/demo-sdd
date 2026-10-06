package br.demo.usuarios.compartilhado.validacao;

import br.demo.usuarios.compartilhado.erro.CodigoCampo;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Constraint de tipo para {@link ComConfirmacaoSenha}: a confirmação deve ser exatamente igual à
 * senha, sem {@code trim}. A violação {@code SENHAS_DIFERENTES} vai no campo {@code confirmacaoSenha}.
 * Confirmação vazia não é avaliada aqui: o {@code @Valida(CONFIRMACAO_SENHA)} do campo já emite
 * {@code CAMPO_OBRIGATORIO}, e o campo fica com um único código.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhasConferem.Validador.class)
public @interface SenhasConferem {

    /** Não usada: o validador sempre emite {@code SENHAS_DIFERENTES} em {@code confirmacaoSenha}. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<SenhasConferem, ComConfirmacaoSenha> {

        private static final Pattern EM_BRANCO = Pattern.compile("\\p{IsWhite_Space}*");

        @Override
        public boolean isValid(ComConfirmacaoSenha requisicao, ConstraintValidatorContext contexto) {
            if (requisicao == null) {
                return true;
            }
            String confirmacao = requisicao.confirmacaoSenha();
            if (confirmacao == null || EM_BRANCO.matcher(confirmacao).matches()
                    || confirmacao.equals(requisicao.senha())) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(CodigoCampo.SENHAS_DIFERENTES.name())
                    .addPropertyNode("confirmacaoSenha")
                    .addConstraintViolation();
            return false;
        }
    }
}
