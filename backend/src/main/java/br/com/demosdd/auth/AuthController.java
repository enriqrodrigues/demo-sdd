package br.com.demosdd.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.demosdd.shared.web.ApiException;
import br.com.demosdd.user.User;
import br.com.demosdd.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Login e dados do usuário autenticado (design D7). O logout é tratado pelo
 * filtro padrão do Spring Security em {@code POST /api/auth/logout}.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final LoginService loginService;
    private final UserRepository users;

    AuthController(LoginService loginService, UserRepository users) {
        this.loginService = loginService;
        this.users = users;
    }

    @PostMapping("/login")
    CurrentUserResponse login(@Valid @RequestBody LoginRequest credentials, HttpServletRequest request,
                              HttpServletResponse response) {
        AuthenticatedUser user = loginService.login(credentials, request, response);
        return new CurrentUserResponse(user.name(), user.email());
    }

    /** Dados lidos do banco pelo id do principal, para refletir alterações sem novo login. */
    @GetMapping("/me")
    CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = users.findById(principal.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED", "Não autenticado", "Faça login para continuar."));
        return new CurrentUserResponse(user.getName(), user.getEmail());
    }

    record CurrentUserResponse(String name, String email) {
    }
}
