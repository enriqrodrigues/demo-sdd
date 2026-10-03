package br.com.demosdd.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Sigla de uma das 27 unidades federativas do Brasil. {@code null} é aceito. */
@Documented
@Constraint(validatedBy = UfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Uf {

    String message() default "UF inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
