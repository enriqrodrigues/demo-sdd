package br.com.demo.cadastro.shared;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final String campo;

    public NegocioException(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, null);
    }

    public NegocioException(HttpStatus status, String codigo, String mensagem, String campo) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.campo = campo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCampo() {
        return campo;
    }
}
