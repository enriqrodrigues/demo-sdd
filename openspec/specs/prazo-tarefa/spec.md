# prazo-tarefa Specification

## Purpose

Define como o sistema calcula, a cada leitura, a situação de prazo de uma tarefa a partir de seu status, de sua data de vencimento e da data corrente no fuso configurado, sem persistir essa situação.

## Requirements

### Requirement: Cálculo da situação de prazo
O sistema SHALL calcular `situacaoPrazo` para cada tarefa retornada pela API: `CONCLUIDA` se `status` é `CONCLUIDA`; para `status` `PENDENTE`, `ATRASADA` se a data corrente é posterior a `dataVencimento`, `PARA_HOJE` se é igual e `NO_PRAZO` se é anterior.

#### Scenario: Pendente com vencimento ontem
- **WHEN** a data corrente é 2026-10-21 e uma tarefa pendente vence em 2026-10-20
- **THEN** `situacaoPrazo` é `ATRASADA`

#### Scenario: Pendente com vencimento hoje
- **WHEN** a data corrente é 2026-10-21 e uma tarefa pendente vence em 2026-10-21
- **THEN** `situacaoPrazo` é `PARA_HOJE`

#### Scenario: Pendente com vencimento amanhã
- **WHEN** a data corrente é 2026-10-21 e uma tarefa pendente vence em 2026-10-22
- **THEN** `situacaoPrazo` é `NO_PRAZO`

#### Scenario: Concluída com vencimento no passado
- **WHEN** uma tarefa concluída tem `dataVencimento` anterior à data corrente
- **THEN** `situacaoPrazo` é `CONCLUIDA`

### Requirement: Data corrente no fuso configurado
A data corrente usada no cálculo de prazo SHALL ser a data local no fuso horário configurado da aplicação, cujo padrão é `America/Sao_Paulo`, independentemente do fuso do servidor ou do cliente.

#### Scenario: Virada de dia em UTC antes do fuso local
- **WHEN** o instante corrente é 2026-10-21T01:30Z (22:30 de 2026-10-20 em `America/Sao_Paulo`) e uma tarefa pendente vence em 2026-10-20
- **THEN** `situacaoPrazo` é `PARA_HOJE`

#### Scenario: Fuso configurado diferente
- **WHEN** a aplicação está configurada com o fuso `UTC`, o instante corrente é 2026-10-21T01:30Z e uma tarefa pendente vence em 2026-10-20
- **THEN** `situacaoPrazo` é `ATRASADA`

### Requirement: Situação de prazo não persistida
A situação de prazo SHALL ser derivada no momento de cada leitura e SHALL NOT ser armazenada; a passagem do tempo SHALL alterar a situação sem nenhuma escrita na tarefa. O campo `situacaoPrazo` é somente leitura na API.

#### Scenario: Passagem do tempo
- **WHEN** uma tarefa pendente com vencimento em 2026-10-22 é consultada em 2026-10-21 e de novo em 2026-10-22, sem alterações entre as consultas
- **THEN** a primeira consulta retorna `NO_PRAZO`, a segunda retorna `PARA_HOJE` e `version` é a mesma nas duas

#### Scenario: Situação após transições
- **WHEN** uma tarefa pendente atrasada é concluída e depois reaberta
- **THEN** a resposta da conclusão traz `CONCLUIDA` e a resposta da reabertura traz `ATRASADA`

#### Scenario: Situação após editar o vencimento
- **WHEN** a data corrente é 2026-10-21 e uma tarefa pendente com vencimento 2026-10-20 é editada para vencer em 2026-10-25
- **THEN** a resposta da edição traz `situacaoPrazo` `NO_PRAZO`
