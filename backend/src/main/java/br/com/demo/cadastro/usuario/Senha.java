package br.com.demo.cadastro.usuario;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Senha {

    public static final int LIMITE_BYTES = 72;
    public static final String MENSAGEM_FRACA =
            "A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial";
    public static final String MENSAGEM_LONGA = "A senha é longa demais";

    private static final Pattern FORTE =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private Senha() {}

    public static boolean isForte(String senha) {
        return senha != null && FORTE.matcher(senha).matches();
    }

    public static boolean excedeLimite(String senha) {
        return senha != null && senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES;
    }
}
