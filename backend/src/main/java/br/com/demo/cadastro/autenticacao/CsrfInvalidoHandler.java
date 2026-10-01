package br.com.demo.cadastro.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

class CsrfInvalidoHandler implements AccessDeniedHandler {

    private static final String CORPO = "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
            + "\"detail\":\"Requisição recusada: token CSRF ausente ou inválido\",\"codigo\":\"CSRF_INVALIDO\"}";

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException erro)
            throws IOException {
        resposta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(CORPO);
    }
}
