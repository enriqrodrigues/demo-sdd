# score-produtividade Specification

## Purpose

Fornece um indicador analítico simples de desempenho: o percentual de tarefas concluídas no prazo em relação ao total de tarefas concluídas na semana corrente.

## Requirements

### Requirement: Consulta do score semanal
A API SHALL expor `GET /api/produtividade/semana`, respondendo 200 com `inicioSemana` e `fimSemana` (datas `YYYY-MM-DD`), `concluidas` (inteiro), `noPrazo` (inteiro) e `percentual` (número ou `null`), calculados no momento da consulta.

#### Scenario: Estrutura da resposta
- **WHEN** o cliente consulta o score
- **THEN** a resposta é 200 e contém os campos `inicioSemana`, `fimSemana`, `concluidas`, `noPrazo` e `percentual`

### Requirement: Semana corrente
A semana corrente SHALL ser a semana ISO-8601 (segunda-feira a domingo) que contém a data corrente no fuso configurado. `inicioSemana` é a segunda-feira e `fimSemana` é o domingo dessa semana.

#### Scenario: Consulta numa quarta-feira
- **WHEN** a data corrente é quarta-feira, 2026-10-21
- **THEN** `inicioSemana` é 2026-10-19 e `fimSemana` é 2026-10-25

#### Scenario: Consulta num domingo
- **WHEN** a data corrente é domingo, 2026-10-25
- **THEN** `inicioSemana` é 2026-10-19 e `fimSemana` é 2026-10-25

#### Scenario: Consulta numa segunda-feira
- **WHEN** a data corrente é segunda-feira, 2026-10-26
- **THEN** `inicioSemana` é 2026-10-26 e `fimSemana` é 2026-11-01

### Requirement: Tarefas finalizadas na semana
`concluidas` SHALL contar as tarefas com `status` `CONCLUIDA` cuja data local de `concluidaEm`, no fuso configurado, está entre `inicioSemana` e `fimSemana`, inclusive. Tarefas pendentes, reabertas ou excluídas SHALL NOT ser contadas.

#### Scenario: Conclusão no domingo da semana anterior
- **WHEN** a semana corrente começa em 2026-10-19 e uma tarefa foi concluída em 2026-10-18T23:59 no fuso configurado
- **THEN** essa tarefa não é contada em `concluidas`

#### Scenario: Conclusão na segunda-feira à meia-noite
- **WHEN** a semana corrente começa em 2026-10-19 e uma tarefa foi concluída em 2026-10-19T00:00 no fuso configurado
- **THEN** essa tarefa é contada em `concluidas`

#### Scenario: Limite da semana avaliado no fuso configurado
- **WHEN** o fuso é `America/Sao_Paulo`, a semana corrente começa em 2026-10-19 e uma tarefa foi concluída em 2026-10-19T02:00Z (23:00 de 2026-10-18 no fuso)
- **THEN** essa tarefa não é contada em `concluidas`

#### Scenario: Pendentes não contam
- **WHEN** existem tarefas pendentes, inclusive atrasadas
- **THEN** elas não afetam `concluidas`, `noPrazo` nem `percentual`

#### Scenario: Tarefa reaberta sai do score
- **WHEN** uma tarefa concluída nesta semana é reaberta
- **THEN** ela deixa de ser contada em `concluidas` e em `noPrazo`

#### Scenario: Tarefa excluída sai do score
- **WHEN** uma tarefa concluída nesta semana é excluída
- **THEN** ela deixa de ser contada em `concluidas` e em `noPrazo`

### Requirement: Conclusão no prazo
Entre as tarefas contadas em `concluidas`, `noPrazo` SHALL contar as que têm a data local de `concluidaEm`, no fuso configurado, igual ou anterior à `dataVencimento` vigente no momento da consulta.

#### Scenario: Concluída antes do vencimento
- **WHEN** uma tarefa que vence em 2026-10-23 foi concluída em 2026-10-21
- **THEN** ela é contada em `noPrazo`

#### Scenario: Concluída no dia do vencimento
- **WHEN** uma tarefa que vence em 2026-10-21 foi concluída em 2026-10-21 às 23:59 no fuso configurado
- **THEN** ela é contada em `noPrazo`

#### Scenario: Concluída depois do vencimento
- **WHEN** uma tarefa que vence em 2026-10-20 foi concluída em 2026-10-21
- **THEN** ela é contada em `concluidas` mas não em `noPrazo`

#### Scenario: Vencimento editado após a conclusão
- **WHEN** uma tarefa concluída em 2026-10-21 com vencimento 2026-10-20 tem o vencimento editado para 2026-10-22
- **THEN** a consulta seguinte passa a contá-la em `noPrazo`

### Requirement: Percentual do score
`percentual` SHALL ser `noPrazo / concluidas × 100`, arredondado para 2 casas decimais com arredondamento meio para cima, e SHALL ser `null` quando `concluidas` for 0.

#### Scenario: Sem tarefas finalizadas
- **WHEN** nenhuma tarefa foi concluída na semana corrente
- **THEN** `concluidas` é 0, `noPrazo` é 0 e `percentual` é `null`

#### Scenario: Todas no prazo
- **WHEN** 4 tarefas foram concluídas na semana, todas no prazo
- **THEN** `percentual` é 100

#### Scenario: Dízima arredondada
- **WHEN** 3 tarefas foram concluídas na semana e 2 delas no prazo
- **THEN** `percentual` é 66.67

#### Scenario: Nenhuma no prazo
- **WHEN** 2 tarefas foram concluídas na semana, ambas após o vencimento
- **THEN** `percentual` é 0
