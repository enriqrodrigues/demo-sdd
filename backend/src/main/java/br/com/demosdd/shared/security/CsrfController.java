package br.com.demosdd.shared.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Garante a emissão do cookie {@code XSRF-TOKEN} antes do primeiro POST da
 * interface (design D4): carregar o token faz o repositório gravar o cookie.
 */
@RestController
class CsrfController {

    @GetMapping("/api/auth/csrf")
    ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
