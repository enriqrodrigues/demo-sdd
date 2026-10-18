# Proposal

## Why

A API já registra e devolve `dataConclusao`, mas a tela não a exibe. Como o selo de situação é `Concluída` tanto para tarefas entregues no prazo quanto com atraso, o usuário não tem como saber quando concluiu uma tarefa nem se isso aconteceu antes do vencimento, embora o score semanal leve essa diferença em conta.

## What Changes

- A lista de tarefas passa a exibir "Concluída em dd/mm/aaaa" nas tarefas com `status` `CONCLUIDA`, ao lado da data de vencimento.
- Tarefas pendentes continuam sem nenhuma indicação de data de conclusão.
- A data é somente leitura: não há edição manual, e backend, API e cálculo do score não mudam.

Fora do escopo: selo distinguindo "concluída no prazo" de "concluída com atraso" (alteraria `deadline-status`) e edição manual da data de conclusão.

## Capabilities

### New Capabilities

_Nenhuma._

### Modified Capabilities

- `task-web-ui`: o requisito "Lista de tarefas" passa a incluir a data de conclusão das tarefas concluídas.

## Impact

- Frontend: `frontend/src/ListaTarefas.tsx` e seus testes em `frontend/src/ListaTarefas.test.tsx`.
- Sem impacto em backend, banco de dados, contrato da API ou dependências.
