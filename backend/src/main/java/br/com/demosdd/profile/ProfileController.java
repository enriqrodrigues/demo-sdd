package br.com.demosdd.profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.demosdd.auth.AuthenticatedUser;

import jakarta.validation.Valid;

/**
 * Perfil do usuário autenticado (design D5). Não há id na rota: o perfil é
 * sempre o do principal da sessão. A rota é protegida pela regra geral de
 * {@code /api/**} da configuração de segurança.
 */
@RestController
@RequestMapping("/api/profile")
class ProfileController {

    private final ProfileService profiles;

    ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    ProfileResponse get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return profiles.get(principal.id());
    }

    @PutMapping
    ProfileResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                           @Valid @RequestBody ProfileUpdateRequest request) {
        return profiles.update(principal.id(), request);
    }
}
