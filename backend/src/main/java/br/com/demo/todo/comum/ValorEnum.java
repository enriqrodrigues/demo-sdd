package br.com.demo.todo.comum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Texto igual, exatamente, ao nome de uma constante do enum; números e variações de caixa são
 * rejeitados. {@code null} é válido.
 */
@Documented
@Constraint(validatedBy = ValorEnumValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValorEnum {

    Class<? extends Enum<?>> value();

    String message() default "Valor inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
