package br.com.demosdd.shared.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Regra de força da senha (RF02): 8 a 64 caracteres, com ao menos uma letra
 * maiúscula, uma minúscula, um dígito e um caractere especial (qualquer
 * caractere que não seja letra nem dígito).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private PasswordPolicy() {
    }

    /** Mensagens dos critérios não atendidos; lista vazia quando a senha é forte. */
    public static List<String> unmetCriteria(String password) {
        List<String> unmet = new ArrayList<>();
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            unmet.add("A senha deve ter ao menos " + MIN_LENGTH + " caracteres");
        }
        if (length > MAX_LENGTH) {
            unmet.add("A senha deve ter no máximo " + MAX_LENGTH + " caracteres");
        }
        if (password.codePoints().noneMatch(Character::isUpperCase)) {
            unmet.add("A senha deve conter ao menos uma letra maiúscula");
        }
        if (password.codePoints().noneMatch(Character::isLowerCase)) {
            unmet.add("A senha deve conter ao menos uma letra minúscula");
        }
        if (password.codePoints().noneMatch(Character::isDigit)) {
            unmet.add("A senha deve conter ao menos um dígito");
        }
        if (password.codePoints().allMatch(Character::isLetterOrDigit)) {
            unmet.add("A senha deve conter ao menos um caractere especial");
        }
        return unmet;
    }
}
