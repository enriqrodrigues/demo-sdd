package br.demo.usuarios.autenticacao.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/auth/sessao}: sempre 200 e entrega o cookie {@code XSRF-TOKEN} (AD-7, AD-15).
 */
@RestController
@RequestMapping("/api/auth")
class SessaoController {

    @GetMapping("/sessao")
    SessaoResposta sessao(CsrfToken csrf) {
        // Carrega o token diferido para que o CookieCsrfTokenRepository grave o cookie XSRF-TOKEN.
        csrf.getToken();
        return new SessaoResposta(false);
    }

    record SessaoResposta(boolean autenticado) {
    }
}
