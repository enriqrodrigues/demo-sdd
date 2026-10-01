package br.com.demo.cadastro.usuario;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isEmpty()) {
            return true;
        }
        if (Senha.excedeLimite(valor)) {
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(Senha.MENSAGEM_LONGA).addConstraintViolation();
            return false;
        }
        return Senha.isForte(valor);
    }
}
