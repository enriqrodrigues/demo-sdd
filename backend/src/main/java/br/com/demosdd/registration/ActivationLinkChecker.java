package br.com.demosdd.registration;

import java.time.Instant;
import java.util.UUID;

/**
 * Consulta usada pela regra de unicidade (RN03) para saber se um cadastro
 * pendente ainda pode ser ativado. Implementada pelo módulo de ativação, o que
 * mantém o cadastro independente de como os links são guardados.
 */
public interface ActivationLinkChecker {

    /** {@code true} se o usuário tem um link de ativação não utilizado e não expirado em {@code now}. */
    boolean hasValidActivationLink(UUID userId, Instant now);
}
