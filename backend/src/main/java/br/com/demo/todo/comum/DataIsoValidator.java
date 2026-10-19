package br.com.demo.todo.comum;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataIsoValidator implements ConstraintValidator<DataIso, String> {

    private static final Pattern FORMATO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final DateTimeFormatter ESTRITO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Converte um texto já validado por {@link DataIso}. */
    public static LocalDate converter(String texto) {
        return LocalDate.parse(texto, ESTRITO);
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        if (!FORMATO.matcher(valor).matches()) {
            return false;
        }
        try {
            converter(valor);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
