package br.demo.usuarios.compartilhado.erro;

import java.util.List;

/**
 * Corpo de toda resposta de erro da API (AD-11). {@code campos} está sempre presente.
 */
public record EnvelopeErro(String codigo, String mensagem, List<CampoErro> campos) {

    public EnvelopeErro {
        campos = campos == null ? List.of() : List.copyOf(campos);
    }
}
