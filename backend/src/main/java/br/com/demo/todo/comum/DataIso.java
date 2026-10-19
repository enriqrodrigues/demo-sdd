package br.com.demo.todo.comum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Texto que é uma data de calendário real no formato estrito {@code YYYY-MM-DD}; {@code null} é válido. */
@Documented
@Constraint(validatedBy = DataIsoValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataIso {

    String message() default "Informe uma data válida no formato AAAA-MM-DD.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
