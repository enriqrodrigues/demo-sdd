package br.demo.usuarios.compartilhado.erro;

import java.util.List;

/**
 * Base única das exceções que viram resposta HTTP com envelope (AD-16).
 */
public class ErroNegocio extends RuntimeException {

    private final String codigo;
    private final int status;
    private final List<CampoErro> campos;

    public ErroNegocio(String codigo, int status, String mensagem, List<CampoErro> campos) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
        this.campos = campos == null ? List.of() : List.copyOf(campos);
    }

    public ErroNegocio(CodigoErro codigo, List<CampoErro> campos) {
        this(codigo.name(), codigo.status().value(), codigo.mensagem(), campos);
    }

    public String getCodigo() {
        return codigo;
    }

    public int getStatus() {
        return status;
    }

    public List<CampoErro> getCampos() {
        return campos;
    }
}
