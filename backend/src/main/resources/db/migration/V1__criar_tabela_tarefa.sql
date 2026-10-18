CREATE TABLE tarefa (
    id              uuid         PRIMARY KEY,
    titulo          varchar(100) NOT NULL,
    descricao       text,
    data_vencimento date         NOT NULL,
    prioridade      varchar(5)   NOT NULL,
    status          varchar(9)   NOT NULL,
    data_conclusao  date,

    CONSTRAINT ck_tarefa_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_tarefa_status CHECK (status IN ('PENDENTE', 'CONCLUIDA')),
    CONSTRAINT ck_tarefa_conclusao CHECK (
        (status = 'PENDENTE' AND data_conclusao IS NULL)
        OR (status = 'CONCLUIDA' AND data_conclusao IS NOT NULL)
    )
);
