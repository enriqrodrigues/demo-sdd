package br.com.demosdd.profile;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.shared.web.ApiException;
import br.com.demosdd.user.Address;
import br.com.demosdd.user.User;
import br.com.demosdd.user.UserRepository;

/**
 * Perfil do usuário da sessão (RF07). Recebe sempre o id do principal: não há
 * como chegar ao perfil de outro usuário.
 */
@Service
class ProfileService {

    private final UserRepository users;

    ProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    ProfileResponse get(UUID userId) {
        return ProfileResponse.of(load(userId));
    }

    /** Recebe dados já normalizados e validados (ver {@link ProfileUpdateRequest}). */
    @Transactional
    ProfileResponse update(UUID userId, ProfileUpdateRequest request) {
        User user = load(userId);
        user.updateContact(request.phone(), new Address(request.cep(), request.street(), request.number(),
                request.complement(), request.district(), request.city(), request.state()));
        return ProfileResponse.of(users.saveAndFlush(user));
    }

    /** Conta removida depois do login: a sessão deixa de valer. */
    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED", "Não autenticado", "Faça login para continuar."));
    }
}
