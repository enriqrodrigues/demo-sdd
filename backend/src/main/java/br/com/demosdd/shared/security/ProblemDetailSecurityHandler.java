package br.com.demosdd.shared.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Recusas da camada de segurança no mesmo formato {@code ProblemDetail} do
 * restante da API (design D5): 401 {@code UNAUTHENTICATED} sem sessão e 403
 * {@code CSRF_INVALID} sem token anti-CSRF válido.
 */
final class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    static final String CSRF_INVALID = "CSRF_INVALID";
    static final String FORBIDDEN = "FORBIDDEN";

    private final ObjectMapper objectMapper;

    ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, UNAUTHENTICATED, "Não autenticado",
                "Faça login para continuar.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (accessDeniedException instanceof CsrfException) {
            write(response, HttpStatus.FORBIDDEN, CSRF_INVALID, "Requisição recusada",
                    "Token anti-CSRF ausente ou inválido. Recarregue a página e tente novamente.");
        } else {
            write(response, HttpStatus.FORBIDDEN, FORBIDDEN, "Acesso negado",
                    "Você não tem permissão para acessar este recurso.");
        }
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String title, String detail)
            throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setProperty("code", code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
