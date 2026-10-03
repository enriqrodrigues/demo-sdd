package br.com.demosdd.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isValidCpf(value);
    }

    /**
     * Espera somente dígitos (entrada já normalizada). Recusa sequências de um
     * mesmo dígito, que passam no cálculo mas não são CPFs válidos.
     */
    public static boolean isValidCpf(String cpf) {
        if (!cpf.matches("\\d{11}")) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(cpf, 9) == digitAt(cpf, 9)
                && checkDigit(cpf, 10) == digitAt(cpf, 10);
    }

    /** Dígito verificador da posição {@code length}, calculado sobre os {@code length} dígitos anteriores. */
    private static int checkDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digitAt(cpf, i) * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(String cpf, int index) {
        return cpf.charAt(index) - '0';
    }
}
