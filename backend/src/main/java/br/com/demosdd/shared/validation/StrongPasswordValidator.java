package br.com.demosdd.shared.validation;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        List<String> unmet = PasswordPolicy.unmetCriteria(value);
        if (unmet.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        unmet.forEach(message -> context.buildConstraintViolationWithTemplate(message).addConstraintViolation());
        return false;
    }
}
