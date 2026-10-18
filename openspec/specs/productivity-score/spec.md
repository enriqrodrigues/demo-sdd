# productivity-score Specification

## Purpose

Disponibiliza um indicador simples de desempenho: o percentual de tarefas concluídas no prazo entre as tarefas concluídas na semana corrente.

## Requirements

### Requirement: Endpoint do score semanal
O sistema SHALL expor `GET /api/produtividade/score`, respondendo `200` com `inicioSemana`, `fimSemana` (datas `YYYY-MM-DD`), `concluidasNoPrazo`, `totalConcluidas` (inteiros) e `percentual` (número ou `null`).

#### Scenario: Consulta do score
- **WHEN** o cliente consulta o score
- **THEN** o sistema responde `200` com os cinco campos

### Requirement: Definição da semana corrente
O sistema SHALL considerar como semana corrente o intervalo de segunda-feira a domingo, inclusive, que contém a data atual no fuso `America/Sao_Paulo`.

#### Scenario: Data atual no meio da semana
- **WHEN** a data atual é quarta-feira, 2026-10-14
- **THEN** `inicioSemana` é `2026-10-12` e `fimSemana` é `2026-10-18`

#### Scenario: Data atual no domingo
- **WHEN** a data atual é domingo, 2026-10-18
- **THEN** `inicioSemana` é `2026-10-12` e `fimSemana` é `2026-10-18`

#### Scenario: Data atual na segunda-feira
- **WHEN** a data atual é segunda-feira, 2026-10-19
- **THEN** `inicioSemana` é `2026-10-19` e `fimSemana` é `2026-10-25`

### Requirement: Tarefas consideradas no score
O sistema SHALL contar em `totalConcluidas` as tarefas com `status` `CONCLUIDA` e `dataConclusao` dentro da semana corrente, e em `concluidasNoPrazo` as que, entre essas, têm `dataConclusao` anterior ou igual a `dataVencimento`.

#### Scenario: Concluídas no prazo e com atraso
- **WHEN** na semana corrente foram concluídas três tarefas: uma antes do vencimento, uma no dia do vencimento e uma depois do vencimento
- **THEN** `totalConcluidas` é `3` e `concluidasNoPrazo` é `2`

#### Scenario: Tarefas fora da contagem
- **WHEN** existem tarefas `PENDENTE` (inclusive atrasadas) e tarefas concluídas antes de `inicioSemana`
- **THEN** essas tarefas não entram em `totalConcluidas` nem em `concluidasNoPrazo`

#### Scenario: Tarefa reaberta
- **WHEN** uma tarefa concluída na semana corrente é reaberta
- **THEN** ela deixa de contar no score

### Requirement: Cálculo do percentual
O sistema SHALL calcular `percentual` como `concluidasNoPrazo / totalConcluidas × 100`, arredondado para duas casas decimais (meio para cima), e SHALL retornar `percentual` `null` quando `totalConcluidas` for `0`.

#### Scenario: Percentual com arredondamento
- **WHEN** `concluidasNoPrazo` é `2` e `totalConcluidas` é `3`
- **THEN** `percentual` é `66.67`

#### Scenario: Todas no prazo
- **WHEN** `concluidasNoPrazo` é `4` e `totalConcluidas` é `4`
- **THEN** `percentual` é `100`

#### Scenario: Nenhuma no prazo
- **WHEN** `concluidasNoPrazo` é `0` e `totalConcluidas` é `2`
- **THEN** `percentual` é `0`

#### Scenario: Nenhuma tarefa concluída na semana
- **WHEN** nenhuma tarefa tem `dataConclusao` na semana corrente
- **THEN** `concluidasNoPrazo` e `totalConcluidas` são `0` e `percentual` é `null`
