package com.demosdd.todo.compartilhado;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/** Converte todos os erros da API em ProblemDetail (RFC 7807). */
@RestControllerAdvice
public class TratamentoGlobalErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratamentoGlobalErros.class);

    private static final String DETALHE_CAMPOS_INVALIDOS = "Um ou mais campos são inválidos.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // um item por campo: se o campo violar mais de uma regra, fica a primeira mensagem
        Map<String, String> mensagensPorCampo = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            mensagensPorCampo.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        List<CampoInvalido> erros = mensagensPorCampo.entrySet().stream()
                .map(e -> new CampoInvalido(e.getKey(), e.getValue()))
                .toList();
        return problemaDeValidacao(erros);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // data ou enum inválido: o Jackson informa o caminho do campo que falhou
        if (ex.getCause() instanceof MismatchedInputException erro) {
            String campo = caminhoDoCampo(erro);
            if (!campo.isEmpty()) {
                return problemaDeValidacao(
                        List.of(new CampoInvalido(campo, mensagemDeValorInvalido(erro.getTargetType()))));
            }
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou não é um JSON válido.");
        problema.setTitle("Requisição malformada");
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O valor '" + ex.getValue() + "' não é válido para o parâmetro '" + parametro + "'.");
        problema.setTitle("Parâmetro inválido");
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle(ex.getTitulo());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarErroInesperado(Exception ex) {
        // o detalhe vai só para o log; o cliente recebe uma mensagem genérica
        log.error("Erro inesperado ao processar a requisição", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problema.setTitle("Erro interno");
        return ResponseEntity.internalServerError().body(problema);
    }

    private static ResponseEntity<Object> problemaDeValidacao(List<CampoInvalido> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, DETALHE_CAMPOS_INVALIDOS);
        problema.setTitle("Dados inválidos");
        problema.setProperty("errors", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    private static String caminhoDoCampo(JsonMappingException erro) {
        return erro.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private static String mensagemDeValorInvalido(Class<?> tipo) {
        if (tipo != null && tipo.isEnum()) {
            String aceitos = Arrays.stream(tipo.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "Valor inválido. Valores aceitos: " + aceitos + ".";
        }
        if (LocalDate.class.equals(tipo)) {
            return "Data inválida. Use uma data existente no formato YYYY-MM-DD.";
        }
        return "Valor inválido.";
    }
}
