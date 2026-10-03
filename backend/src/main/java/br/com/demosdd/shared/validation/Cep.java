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

/** CEP com exatamente 8 dígitos (sem consulta externa). {@code null} é aceito. */
@Documented
@Pattern(regexp = "\\d{8}")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cep {

    String message() default "O CEP deve ter 8 dígitos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
