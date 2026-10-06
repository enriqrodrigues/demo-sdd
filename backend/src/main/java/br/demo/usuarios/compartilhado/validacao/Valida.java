package br.demo.usuarios.compartilhado.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Única constraint de um campo {@code String} de requisição (AD-10): checa obrigatório e depois a
 * regra, e emite no máximo uma violação, cuja mensagem é o nome de um
 * {@link br.demo.usuarios.compartilhado.erro.CodigoCampo}.
 *
 * <p>O valor já deve chegar normalizado ({@link Normalizacao}). O alvo é só {@code FIELD} para que,
 * num componente de record, a constraint não seja copiada para o acessor e avaliada duas vezes.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorCampo.class)
public @interface Valida {

    RegraCampo value();

    /** Não usada: o validador sempre emite o nome do código como mensagem. */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
