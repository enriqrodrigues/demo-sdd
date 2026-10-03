package br.com.demosdd.shared.validation;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UfValidator implements ConstraintValidator<Uf, String> {

    public static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || UFS.contains(value);
    }
}
