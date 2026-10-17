CREATE TABLE tarefa (
    id              UUID         PRIMARY KEY,
    titulo          VARCHAR(100) NOT NULL,
    descricao       TEXT,
    data_vencimento DATE         NOT NULL,
    prioridade      VARCHAR(10)  NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    data_conclusao  DATE,
    CONSTRAINT ck_tarefa_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_tarefa_status CHECK (status IN ('PENDENTE', 'CONCLUIDA')),
    CONSTRAINT ck_tarefa_data_conclusao CHECK ((status = 'CONCLUIDA') = (data_conclusao IS NOT NULL))
);
