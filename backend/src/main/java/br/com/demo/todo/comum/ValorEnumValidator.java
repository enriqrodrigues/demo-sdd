package br.com.demo.todo.comum;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValorEnumValidator implements ConstraintValidator<ValorEnum, String> {

    private Set<String> nomes;

    @Override
    public void initialize(ValorEnum anotacao) {
        nomes = Arrays.stream(anotacao.value().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || nomes.contains(valor);
    }
}
