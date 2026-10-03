package br.com.demosdd.user;

/** Ciclo de vida da conta: nasce pendente (RF03) e passa a ativa pelo link de e-mail (RF05). */
public enum UserStatus {
    PENDENTE,
    ATIVO
}
