package br.demo.usuarios.compartilhado.validacao;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helpers de normalização aplicados antes da validação (AD-5), usados no construtor compacto dos
 * records de requisição. Todos aceitam {@code null} e devolvem {@code null} nesse caso.
 *
 * <p>Espaço em branco é {@code \p{IsWhite_Space}}. O frontend usa {@code \s}/{@code trim}, que difere
 * em dois caracteres (diferença aceita): U+0085 só é espaço aqui, e U+FEFF só é espaço lá.
 */
public final class Normalizacao {

    private static final Pattern ESPACO_EM_BRANCO = Pattern.compile("\\p{IsWhite_Space}");

    private static final Pattern SEPARADORES = Pattern.compile("[.\\-()/\\p{IsWhite_Space}]");

    private Normalizacao() {
    }

    /** Remove espaço em branco do início e do fim. */
    public static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        int inicio = 0;
        int fim = valor.length();
        while (inicio < fim && emBranco(valor.codePointAt(inicio))) {
            inicio += Character.charCount(valor.codePointAt(inicio));
        }
        while (fim > inicio && emBranco(valor.codePointBefore(fim))) {
            fim -= Character.charCount(valor.codePointBefore(fim));
        }
        return valor.substring(inicio, fim);
    }

    private static boolean emBranco(int codePoint) {
        return ESPACO_EM_BRANCO.matcher(Character.toString(codePoint)).matches();
    }

    /** Como {@link #texto(String)}, mas texto vazio vira {@code null}. */
    public static String textoOpcional(String valor) {
        String aparado = texto(valor);
        return aparado == null || aparado.isEmpty() ? null : aparado;
    }

    /** {@code trim} seguido de minúsculas independentes de locale. */
    public static String email(String valor) {
        String aparado = texto(valor);
        return aparado == null ? null : aparado.toLowerCase(Locale.ROOT);
    }

    /** Remove só {@code .}, {@code -}, {@code (}, {@code )}, {@code /} e espaço em branco; o resto fica. */
    public static String soDigitos(String valor) {
        return valor == null ? null : SEPARADORES.matcher(valor).replaceAll("");
    }
}
