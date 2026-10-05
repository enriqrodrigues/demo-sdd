package br.demo.usuarios.compartilhado.erro;

import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Único {@code @RestControllerAdvice} da aplicação (AD-11): traduz exceções para o envelope.
 *
 * <p>Convenção de validação: a {@code message} de cada constraint é o nome de um {@link CodigoCampo}.
 * {@code NotNull}, {@code NotBlank} e {@code NotEmpty} com a mensagem padrão viram
 * {@link CodigoCampo#CAMPO_OBRIGATORIO}. Qualquer outra mensagem é bug: loga {@code ERROR} e vira 500.
 */
@RestControllerAdvice
public class TratadorErros {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorErros.class);

    private static final Set<String> CONSTRAINTS_OBRIGATORIO = Set.of("NotNull", "NotBlank", "NotEmpty");

    private final EscritorErro escritor;

    public TratadorErros(EscritorErro escritor) {
        this.escritor = escritor;
    }

    @ExceptionHandler(ErroNegocio.class)
    ResponseEntity<EnvelopeErro> erroNegocio(ErroNegocio erro) {
        return escritor.resposta(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<EnvelopeErro> validacao(MethodArgumentNotValidException erro) {
        var resultado = erro.getBindingResult();
        if (resultado.hasGlobalErrors()) {
            LOG.error("Erro de validação sem campo associado: {}", resultado.getGlobalErrors());
            return escritor.resposta(CodigoErro.ERRO_INTERNO, List.of());
        }
        List<CampoErro> campos = new ArrayList<>();
        for (FieldError erroCampo : resultado.getFieldErrors()) {
            Optional<CodigoCampo> codigo = traduzir(erroCampo);
            if (codigo.isEmpty()) {
                LOG.error("Constraint sem CodigoCampo no campo '{}': código '{}', mensagem '{}'",
                        erroCampo.getField(), erroCampo.getCode(), erroCampo.getDefaultMessage());
                return escritor.resposta(CodigoErro.ERRO_INTERNO, List.of());
            }
            campos.add(new CampoErro(erroCampo.getField(), codigo.get()));
        }
        return escritor.resposta(CodigoErro.VALIDACAO, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<EnvelopeErro> requisicaoInvalida(HttpMessageNotReadableException erro) {
        return escritor.resposta(CodigoErro.REQUISICAO_INVALIDA, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<EnvelopeErro> naoEncontrado(NoResourceFoundException erro) {
        return escritor.resposta(CodigoErro.NAO_ENCONTRADO, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<EnvelopeErro> erroInterno(Exception erro) {
        LOG.error("Erro inesperado ao processar a requisição", erro);
        return escritor.resposta(CodigoErro.ERRO_INTERNO, List.of());
    }

    private static Optional<CodigoCampo> traduzir(FieldError erroCampo) {
        String mensagem = erroCampo.getDefaultMessage();
        for (CodigoCampo codigo : CodigoCampo.values()) {
            if (codigo.name().equals(mensagem)) {
                return Optional.of(codigo);
            }
        }
        if (CONSTRAINTS_OBRIGATORIO.contains(erroCampo.getCode()) && usaMensagemPadrao(erroCampo)) {
            return Optional.of(CodigoCampo.CAMPO_OBRIGATORIO);
        }
        return Optional.empty();
    }

    private static boolean usaMensagemPadrao(FieldError erroCampo) {
        if (!erroCampo.contains(ConstraintViolation.class)) {
            return false;
        }
        String modelo = erroCampo.unwrap(ConstraintViolation.class).getMessageTemplate();
        return ("{jakarta.validation.constraints." + erroCampo.getCode() + ".message}").equals(modelo);
    }
}
