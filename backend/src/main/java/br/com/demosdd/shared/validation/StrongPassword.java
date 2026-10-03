package br.com.demosdd.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Senha que atende a {@link PasswordPolicy}. Gera uma violação por critério
 * não atendido. {@code null} é aceito (use {@code @NotBlank}).
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Senha fraca";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
