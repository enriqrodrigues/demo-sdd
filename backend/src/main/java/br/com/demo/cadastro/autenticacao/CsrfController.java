package br.com.demo.cadastro.autenticacao;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CsrfController {

    record CsrfResposta(String token) {}

    @GetMapping("/api/csrf")
    CsrfResposta csrf(CsrfToken token) {
        return new CsrfResposta(token.getToken());
    }
}
