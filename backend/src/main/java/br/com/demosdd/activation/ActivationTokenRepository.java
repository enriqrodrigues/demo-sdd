package br.com.demosdd.activation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {

    Optional<ActivationToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndUsedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);
}
