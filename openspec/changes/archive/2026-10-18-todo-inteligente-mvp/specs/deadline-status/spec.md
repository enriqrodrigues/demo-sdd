# Spec Delta

## Purpose

Informa, em cada tarefa retornada pela API, a situação do prazo em relação à data atual, calculada no momento da consulta e nunca persistida.

## ADDED Requirements

### Requirement: Situação de prazo calculada em cada resposta
O sistema SHALL incluir o campo `situacaoPrazo` em toda tarefa retornada pela API, com um dos valores `ATRASADA`, `PARA_HOJE`, `NO_PRAZO` ou `CONCLUIDA`, calculado no momento da resposta e sem armazenamento.

#### Scenario: Situação muda com a passagem do tempo
- **WHEN** uma tarefa `PENDENTE` com vencimento 2026-10-20 é consultada em 2026-10-19, em 2026-10-20 e em 2026-10-21, sem nenhuma alteração entre as consultas
- **THEN** o sistema retorna `situacaoPrazo` `NO_PRAZO`, `PARA_HOJE` e `ATRASADA`, respectivamente

#### Scenario: Situação presente em criação, atualização, consulta e listagem
- **WHEN** o cliente cria, atualiza, consulta ou lista tarefas
- **THEN** cada tarefa na resposta traz `situacaoPrazo`

#### Scenario: Situação enviada pelo cliente é ignorada
- **WHEN** o cliente envia `situacaoPrazo` no corpo da criação ou da atualização
- **THEN** o sistema ignora o valor e retorna a situação calculada

### Requirement: Regra de cálculo da situação de prazo
O sistema SHALL calcular `situacaoPrazo` assim: `CONCLUIDA` quando `status` é `CONCLUIDA`; para `status` `PENDENTE`, `ATRASADA` se a data atual é posterior a `dataVencimento`, `PARA_HOJE` se é igual e `NO_PRAZO` se é anterior.

#### Scenario: Pendente vencida
- **WHEN** a data atual é 2026-10-18 e uma tarefa `PENDENTE` vence em 2026-10-17
- **THEN** `situacaoPrazo` é `ATRASADA`

#### Scenario: Pendente vence hoje
- **WHEN** a data atual é 2026-10-18 e uma tarefa `PENDENTE` vence em 2026-10-18
- **THEN** `situacaoPrazo` é `PARA_HOJE`

#### Scenario: Pendente com vencimento futuro
- **WHEN** a data atual é 2026-10-18 e uma tarefa `PENDENTE` vence em 2026-10-19
- **THEN** `situacaoPrazo` é `NO_PRAZO`

#### Scenario: Concluída dentro do prazo
- **WHEN** uma tarefa `CONCLUIDA` tem `dataConclusao` anterior ou igual a `dataVencimento`
- **THEN** `situacaoPrazo` é `CONCLUIDA`

#### Scenario: Concluída com atraso
- **WHEN** uma tarefa `CONCLUIDA` tem `dataConclusao` posterior a `dataVencimento`
- **THEN** `situacaoPrazo` é `CONCLUIDA`

### Requirement: Data atual no fuso de São Paulo
O sistema SHALL determinar a data atual no fuso `America/Sao_Paulo`, independentemente do fuso do servidor ou do cliente.

#### Scenario: Virada de dia em UTC antes de São Paulo
- **WHEN** o instante atual é 2026-10-18T01:00:00Z (2026-10-17 22:00 em São Paulo) e uma tarefa `PENDENTE` vence em 2026-10-17
- **THEN** `situacaoPrazo` é `PARA_HOJE`
