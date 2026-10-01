package br.com.demo.cadastro.shared;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Set<String> CODIGOS_OBRIGATORIO = Set.of("NotBlank", "NotNull");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException ex) {
        Map<String, FieldError> porCampo = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            porCampo.merge(erro.getField(), erro,
                    (atual, novo) -> CODIGOS_OBRIGATORIO.contains(novo.getCode()) ? novo : atual);
        }
        List<ErroCampo> erros = porCampo.values().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return problema(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados inválidos", erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> corpoInvalido(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "VALIDACAO", "Corpo da requisição inválido", List.of());
    }

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ProblemDetail> negocio(NegocioException ex) {
        List<ErroCampo> erros = ex.getCampo() == null
                ? List.of()
                : List.of(new ErroCampo(ex.getCampo(), ex.getMessage()));
        return problema(ex.getStatus(), ex.getCodigo(), ex.getMessage(), erros);
    }

    private ResponseEntity<ProblemDetail> problema(
            HttpStatus status, String codigo, String detalhe, List<ErroCampo> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setProperty("codigo", codigo);
        if (!erros.isEmpty()) {
            problema.setProperty("erros", erros);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }
}
