package br.com.demosdd.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

/** Telefone com DDD: 10 dígitos (fixo) ou 11 dígitos (celular). {@code null} é aceito. */
@Documented
@Pattern(regexp = "\\d{10,11}")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {

    String message() default "O telefone deve ter 10 dígitos (fixo) ou 11 dígitos (celular), incluindo o DDD";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
