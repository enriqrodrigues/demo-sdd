package br.com.demosdd.todo.comum;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;

/** Converte exceções em respostas application/problem+json (RFC 9457). */
@RestControllerAdvice
public class TratadorGlobalErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalErros.class);

    public record ErroCampo(String campo, String mensagem) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErroCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroCampo::campo))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos são inválidos.", request);
        problema.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detalhe = "Corpo da requisição ausente ou malformado.";
        if (ex.getCause() instanceof JsonMappingException erro && !erro.getPath().isEmpty()) {
            String campo = erro.getPath().get(erro.getPath().size() - 1).getFieldName();
            detalhe = "Valor inválido para o campo '" + campo + "'.";
        }
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", detalhe, request));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String nome = ex instanceof MethodArgumentTypeMismatchException erro ? erro.getName() : ex.getPropertyName();
        String detalhe = "Valor inválido para '" + nome + "': " + ex.getValue();
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detalhe, request));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarNaoEncontrado(RecursoNaoEncontradoException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarErroInesperado(Exception ex, WebRequest request) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.", request));
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        if (request instanceof ServletWebRequest servlet) {
            problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
        }
        return problema;
    }
}
