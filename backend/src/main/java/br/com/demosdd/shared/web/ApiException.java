package br.com.demosdd.shared.web;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Erro de negócio exposto pela API no formato {@code ProblemDetail}, com um
 * {@code code} estável para o frontend e, opcionalmente, erros por campo.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final List<FieldErrorResponse> errors;

    public ApiException(HttpStatus status, String code, String title, String detail) {
        this(status, code, title, detail, List.of());
    }

    public ApiException(HttpStatus status, String code, String title, String detail,
                        List<FieldErrorResponse> errors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
        this.errors = List.copyOf(errors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public List<FieldErrorResponse> getErrors() {
        return errors;
    }
}
