# task-web-ui Specification

## Purpose

Oferece ao usuário uma tela web única para gerenciar as tarefas e acompanhar a situação de prazo de cada uma e o score de produtividade da semana.

## Requirements

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

### Requirement: Criação e edição pelo formulário
A tela SHALL permitir criar uma tarefa e editar uma existente por um formulário com título, descrição, data de vencimento e prioridade, e SHALL atualizar a lista e o score após salvar.

#### Scenario: Criar tarefa
- **WHEN** o usuário preenche o formulário com dados válidos e salva
- **THEN** a nova tarefa aparece na lista e o formulário é fechado ou limpo

#### Scenario: Editar tarefa
- **WHEN** o usuário escolhe editar uma tarefa, altera campos e salva
- **THEN** a lista mostra a tarefa com os novos valores

### Requirement: Exibição de erros de validação
A tela SHALL exibir junto a cada campo do formulário a mensagem de erro de validação retornada pela API para aquele campo.

#### Scenario: Título vazio rejeitado pela API
- **WHEN** o usuário salva o formulário e a API responde `400` com erro no campo `titulo`
- **THEN** a tela mostra a mensagem junto ao campo título e mantém os dados digitados

### Requirement: Concluir e reabrir pela lista
A tela SHALL oferecer em cada tarefa um controle de marcação que conclui uma tarefa pendente e reabre uma tarefa concluída, e SHALL atualizar a situação de prazo e o score após a mudança.

#### Scenario: Concluir tarefa
- **WHEN** o usuário marca uma tarefa pendente
- **THEN** a tarefa passa a exibir o selo de concluída e o score é atualizado

#### Scenario: Reabrir tarefa
- **WHEN** o usuário desmarca uma tarefa concluída
- **THEN** a tarefa volta a exibir a situação de prazo de pendente e o score é atualizado

### Requirement: Exclusão pela lista
A tela SHALL permitir excluir uma tarefa após confirmação do usuário e SHALL removê-la da lista e atualizar o score.

#### Scenario: Excluir com confirmação
- **WHEN** o usuário pede para excluir uma tarefa e confirma
- **THEN** a tarefa sai da lista e o score é atualizado

#### Scenario: Exclusão cancelada
- **WHEN** o usuário pede para excluir uma tarefa e cancela a confirmação
- **THEN** a tarefa continua na lista

### Requirement: Card do score semanal
A tela SHALL exibir o score da semana como percentual acompanhado das contagens (por exemplo, "75% — 3 de 4 no prazo") e, quando o percentual for nulo, uma mensagem de que não há tarefas concluídas nesta semana.

#### Scenario: Score com dados
- **WHEN** a API retorna `percentual` `75`, `concluidasNoPrazo` `3` e `totalConcluidas` `4`
- **THEN** o card mostra 75% e "3 de 4 no prazo"

#### Scenario: Score sem dados
- **WHEN** a API retorna `percentual` `null`
- **THEN** o card mostra que não há tarefas concluídas nesta semana

### Requirement: Falha de comunicação com a API
A tela SHALL exibir uma mensagem de erro compreensível quando a API estiver indisponível ou responder com erro não relacionado a validação.

#### Scenario: API indisponível
- **WHEN** a tela não consegue carregar as tarefas porque a API não responde
- **THEN** a tela mostra uma mensagem de erro em vez de uma lista vazia
