package br.demo.usuarios.compartilhado.erro;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Único ponto que monta o envelope de erro (AD-11): para o advice e para os handlers de segurança.
 */
@Component
public class EscritorErro {

    private final JsonMapper jsonMapper;

    public EscritorErro(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ResponseEntity<EnvelopeErro> resposta(CodigoErro codigo, List<CampoErro> campos) {
        return resposta(codigo.status().value(), envelope(codigo, campos));
    }

    public ResponseEntity<EnvelopeErro> resposta(ErroNegocio erro) {
        return resposta(erro.getStatus(),
                new EnvelopeErro(erro.getCodigo(), erro.getMessage(), erro.getCampos()));
    }

    public void escrever(HttpServletResponse resposta, CodigoErro codigo) throws IOException {
        resposta.setStatus(codigo.status().value());
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(resposta.getOutputStream(), envelope(codigo, List.of()));
    }

    private static EnvelopeErro envelope(CodigoErro codigo, List<CampoErro> campos) {
        return new EnvelopeErro(codigo.name(), codigo.mensagem(), campos);
    }

    private static ResponseEntity<EnvelopeErro> resposta(int status, EnvelopeErro envelope) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(envelope);
    }
}
