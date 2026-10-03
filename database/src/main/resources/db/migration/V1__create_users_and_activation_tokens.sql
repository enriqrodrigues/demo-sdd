-- Usuários cadastrados (RF03). E-mail gravado em minúsculas e CPF/telefone/CEP
-- somente com dígitos, então os índices únicos simples garantem a RN03.
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    cpf           VARCHAR(11)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    birth_date    DATE         NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone         VARCHAR(11)  NOT NULL,
    cep           VARCHAR(8)   NOT NULL,
    street        VARCHAR(200) NOT NULL,
    number        VARCHAR(20)  NOT NULL,
    complement    VARCHAR(100),
    district      VARCHAR(100) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(2)   NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    activated_at  TIMESTAMPTZ,
    CONSTRAINT uk_users_cpf UNIQUE (cpf),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDENTE', 'ATIVO'))
);

-- Links de ativação (RF04, RF05, RN02). Guarda apenas o hash SHA-256 do token.
CREATE TABLE activation_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_activation_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_activation_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_activation_tokens_user_id ON activation_tokens (user_id);
