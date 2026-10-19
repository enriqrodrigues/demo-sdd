create table tarefa (
    id              uuid          primary key,
    titulo          varchar(100)  not null,
    descricao       varchar(2000),
    data_vencimento date          not null,
    prioridade      varchar(5)    not null,
    status          varchar(9)    not null,
    concluida_em    timestamptz,
    version         bigint        not null,
    constraint ck_tarefa_prioridade check (prioridade in ('BAIXA', 'MEDIA', 'ALTA')),
    constraint ck_tarefa_status check (status in ('PENDENTE', 'CONCLUIDA')),
    constraint ck_tarefa_conclusao check ((status = 'CONCLUIDA') = (concluida_em is not null))
);

create index ix_tarefa_status_concluida_em on tarefa (status, concluida_em);
