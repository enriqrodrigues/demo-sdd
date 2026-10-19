package br.com.demo.todo.comum;

import java.util.List;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Formato único de erro da API (Problem Details, RFC 9457). Exceções do Spring MVC e
 * {@code ErrorResponseException} são tratadas pela classe base; erros de validação ganham a
 * lista {@code erros}; o resto vira 500 genérico.
 */
@RestControllerAdvice
public class ManipuladorErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManipuladorErros.class);

    /** Um campo inválido do corpo ou dos parâmetros da requisição. */
    public record ErroCampo(String campo, String mensagem) {
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excecao,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        List<ErroCampo> erros = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(problemaDeValidacao(erros));
    }

    /** Parâmetro de caminho ou de consulta com valor inválido (UUID malformado, filtro fora do enum). */
    @Override
    @Nullable
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException excecao, HttpHeaders cabecalhos,
            HttpStatusCode status, WebRequest requisicao) {
        String campo = excecao instanceof MethodArgumentTypeMismatchException argumento
                ? argumento.getName()
                : excecao.getPropertyName();
        return ResponseEntity.badRequest().body(problemaDeValidacao(
                List.of(new ErroCampo(campo, "Valor inválido: " + excecao.getValue() + "."))));
    }

    /** Corpo ilegível: propriedade desconhecida ou tipo errado viram erro de campo; JSON quebrado, 400 simples. */
    @Override
    @Nullable
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException excecao,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        if (excecao.getCause() instanceof UnrecognizedPropertyException desconhecida) {
            return ResponseEntity.badRequest().body(problemaDeValidacao(
                    List.of(new ErroCampo(desconhecida.getPropertyName(), "Campo desconhecido."))));
        }
        if (excecao.getCause() instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
            String campo = mapeamento.getPath().get(mapeamento.getPath().size() - 1).getFieldName();
            if (campo != null) {
                return ResponseEntity.badRequest().body(problemaDeValidacao(
                        List.of(new ErroCampo(campo, "Valor em formato inválido."))));
            }
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O corpo da requisição não é um JSON válido.");
        problema.setTitle("Requisição malformada");
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception excecao) {
        LOG.error("Erro inesperado ao processar a requisição", excecao);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problema.setTitle("Erro interno");
        return problema;
    }

    static ProblemDetail problemaDeValidacao(List<ErroCampo> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Um ou mais campos são inválidos.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("erros", erros);
        return problema;
    }
}
