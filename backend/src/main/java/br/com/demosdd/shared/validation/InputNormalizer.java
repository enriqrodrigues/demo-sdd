package br.com.demosdd.shared.validation;

import java.util.Locale;

/**
 * Normalização aplicada à entrada antes da validação.
 * Remove apenas caracteres de máscara: qualquer outro caractere permanece e
 * faz a validação falhar (ex.: letras em um CPF).
 */
public final class InputNormalizer {

    private InputNormalizer() {
    }

    /** Remove espaços nas pontas; texto vazio ou só com espaços vira {@code null}. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** E-mail sem espaços nas pontas e em minúsculas. */
    public static String email(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    /** CPF: remove a máscara {@code 000.000.000-00}. */
    public static String cpf(String value) {
        return removeMask(value, "[.\\-\\s]");
    }

    /** Telefone: remove a máscara {@code (00) 00000-0000}. */
    public static String phone(String value) {
        return removeMask(value, "[()\\-\\s]");
    }

    /** CEP: remove a máscara {@code 00000-000}. */
    public static String cep(String value) {
        return removeMask(value, "[\\-\\s]");
    }

    /** UF em maiúsculas. */
    public static String uf(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String removeMask(String value, String maskCharacters) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String unmasked = trimmed.replaceAll(maskCharacters, "");
        return unmasked.isEmpty() ? null : unmasked;
    }
}
