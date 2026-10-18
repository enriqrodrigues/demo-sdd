## MODIFIED Requirements

### Requirement: Lista de tarefas
A tela SHALL exibir todas as tarefas na ordem retornada pela API, mostrando para cada uma o título, a prioridade, a data de vencimento e um selo com a situação de prazo, e, para tarefas com `status` `CONCLUIDA`, a data de conclusão no formato `dd/mm/aaaa`.

#### Scenario: Exibição das tarefas
- **WHEN** o usuário abre a tela e a API retorna tarefas
- **THEN** a tela lista cada tarefa com título, prioridade, vencimento e selo de situação, na ordem recebida

#### Scenario: Lista vazia
- **WHEN** o usuário abre a tela e não há tarefas
- **THEN** a tela informa que não há tarefas cadastradas

#### Scenario: Tarefa concluída exibe a data de conclusão
- **WHEN** a API retorna uma tarefa com `status` `CONCLUIDA` e `dataConclusao` `2026-10-17`
- **THEN** a tela exibe para essa tarefa "Concluída em 17/10/2026", além do vencimento

#### Scenario: Tarefa pendente não exibe data de conclusão
- **WHEN** a API retorna uma tarefa com `status` `PENDENTE`
- **THEN** a tela não exibe data de conclusão para essa tarefa

#### Scenario: Data de conclusão some ao reabrir
- **WHEN** o usuário desmarca uma tarefa concluída e a lista é atualizada
- **THEN** a tarefa deixa de exibir a data de conclusão
