package br.com.demosdd.shared.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Converte erros em respostas {@code ProblemDetail} (RFC 9457) com as
 * extensões {@code code} e {@code errors[]} usadas pelo frontend.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        return problem(ex.getStatus(), ex.getCode(), ex.getTitle(), ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationProblem(errors);
    }

    /** JSON malformado ou valor em formato inesperado (ex.: data inválida). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException invalidFormat && !invalidFormat.getPath().isEmpty()) {
            String field = invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            return validationProblem(List.of(new FieldErrorResponse(field, "Valor em formato inválido")));
        }
        return problem(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Dados inválidos",
                "O corpo da requisição não pôde ser lido", List.of());
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<FieldErrorResponse> errors) {
        return problem(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Dados inválidos",
                "Um ou mais campos são inválidos", errors);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title, String detail,
                                                  List<FieldErrorResponse> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setProperty("code", code);
        if (!errors.isEmpty()) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
