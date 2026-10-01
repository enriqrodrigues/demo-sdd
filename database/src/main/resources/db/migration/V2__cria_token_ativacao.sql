CREATE TABLE token_ativacao (
    id         UUID        PRIMARY KEY,
    usuario_id UUID        NOT NULL REFERENCES usuario (id),
    token_hash CHAR(64)    NOT NULL,
    expira_em  TIMESTAMPTZ NOT NULL,
    usado_em   TIMESTAMPTZ,
    criado_em  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_token_ativacao_hash UNIQUE (token_hash)
);

CREATE INDEX ix_token_ativacao_usuario ON token_ativacao (usuario_id);
