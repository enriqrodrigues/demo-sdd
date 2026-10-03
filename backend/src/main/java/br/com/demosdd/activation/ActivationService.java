package br.com.demosdd.activation;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.registration.ActivationLinkChecker;
import br.com.demosdd.shared.web.ApiException;
import br.com.demosdd.user.User;
import br.com.demosdd.user.UserRepository;
import br.com.demosdd.user.UserStatus;

/** Emissão e uso dos links de ativação (RF04, RF05, RN02). */
@Service
class ActivationService implements ActivationLinkChecker {

    static final String INVALID_TOKEN = "INVALID_TOKEN";
    static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    static final String TOKEN_ALREADY_USED = "TOKEN_ALREADY_USED";

    private final ActivationTokenRepository tokens;
    private final UserRepository users;
    private final ActivationProperties properties;
    private final Clock clock;

    ActivationService(ActivationTokenRepository tokens, UserRepository users,
                      ActivationProperties properties, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    /** Emite um link para o usuário e devolve o token puro, que só existe no e-mail. */
    @Transactional
    String issue(UUID userId) {
        Instant now = clock.instant();
        String token = ActivationTokens.generate();
        tokens.save(new ActivationToken(userId, ActivationTokens.hash(token), now,
                now.plus(properties.activation().tokenTtl())));
        return token;
    }

    /** Ativa a conta dona do token, se ele existir, não tiver sido usado e não estiver expirado. */
    @Transactional
    User activate(String token) {
        Instant now = clock.instant();
        ActivationToken activationToken = (token == null || token.isBlank())
                ? null
                : tokens.findByTokenHash(ActivationTokens.hash(token.strip())).orElse(null);
        if (activationToken == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_TOKEN, "Link inválido",
                    "Este link de ativação é inválido.");
        }
        if (activationToken.isUsed()) {
            throw alreadyUsed();
        }
        if (activationToken.isExpiredAt(now)) {
            throw new ApiException(HttpStatus.GONE, TOKEN_EXPIRED, "Link expirado",
                    "Este link de ativação expirou. Você pode fazer um novo cadastro com os mesmos dados.");
        }

        User user = users.findById(activationToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("Token sem usuário: " + activationToken.getUserId()));
        if (user.getStatus() != UserStatus.PENDENTE) {
            throw alreadyUsed();
        }
        user.activate(now);
        activationToken.markUsed(now);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidActivationLink(UUID userId, Instant now) {
        return tokens.existsByUserIdAndUsedAtIsNullAndExpiresAtAfter(userId, now);
    }

    private static ApiException alreadyUsed() {
        return new ApiException(HttpStatus.CONFLICT, TOKEN_ALREADY_USED, "Link já utilizado",
                "Este link de ativação já foi utilizado.");
    }
}
