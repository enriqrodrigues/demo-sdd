# Spec Delta

## Purpose

Permite ao usuário cadastrar, consultar, alterar, concluir, reabrir e excluir suas tarefas pessoais por meio de uma API REST JSON com validação e erros padronizados.

## ADDED Requirements

### Requirement: Criação de tarefa
O sistema SHALL criar uma tarefa via `POST /api/tarefas` a partir de `titulo`, `descricao`, `dataVencimento` e `prioridade`, responder `201 Created` com a tarefa criada e o cabeçalho `Location`, gerar o `id` e iniciar a tarefa com `status` `PENDENTE` e `dataConclusao` nula.

#### Scenario: Criação com dados válidos
- **WHEN** o cliente envia `{"titulo":"Pagar conta","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA"}`
- **THEN** o sistema responde `201` com a tarefa contendo `id` gerado, os campos enviados, `status` `PENDENTE`, `dataConclusao` `null` e `situacaoPrazo`

#### Scenario: Descrição omitida
- **WHEN** o cliente cria uma tarefa válida sem o campo `descricao`
- **THEN** o sistema cria a tarefa com `descricao` `null`

#### Scenario: Campos controlados pelo sistema são ignorados na criação
- **WHEN** o cliente envia na criação `id`, `status` `CONCLUIDA` ou `dataConclusao`
- **THEN** o sistema ignora esses valores, gera um novo `id` e cria a tarefa com `status` `PENDENTE` e `dataConclusao` `null`

### Requirement: Validação do título
O sistema SHALL exigir `titulo` não vazio após remover espaços nas extremidades, com no máximo 100 caracteres após essa remoção, e SHALL armazenar o título sem os espaços das extremidades.

#### Scenario: Título ausente ou em branco
- **WHEN** o cliente cria ou atualiza uma tarefa com `titulo` ausente, vazio ou composto só de espaços
- **THEN** o sistema responde `400` com erro de validação apontando o campo `titulo`

#### Scenario: Título acima do limite
- **WHEN** o cliente envia `titulo` com 101 caracteres após a remoção dos espaços das extremidades
- **THEN** o sistema responde `400` com erro de validação apontando o campo `titulo`

#### Scenario: Título no limite com espaços nas extremidades
- **WHEN** o cliente envia `titulo` com 100 caracteres cercados por espaços
- **THEN** o sistema aceita a tarefa e armazena o título com os 100 caracteres, sem os espaços

### Requirement: Validação da data de vencimento
O sistema SHALL exigir `dataVencimento` como data válida no formato `YYYY-MM-DD` e SHALL aceitar datas no passado, no presente e no futuro.

#### Scenario: Vencimento ausente
- **WHEN** o cliente cria ou atualiza uma tarefa sem `dataVencimento`
- **THEN** o sistema responde `400` com erro de validação apontando o campo `dataVencimento`

#### Scenario: Vencimento em formato inválido
- **WHEN** o cliente envia `dataVencimento` como `"20/10/2026"` ou `"2026-02-30"`
- **THEN** o sistema responde `400` com erro identificando o campo `dataVencimento`

#### Scenario: Vencimento no passado
- **WHEN** o cliente cria uma tarefa com `dataVencimento` anterior à data atual
- **THEN** o sistema cria a tarefa normalmente

### Requirement: Validação da prioridade
O sistema SHALL exigir `prioridade` com um dos valores `BAIXA`, `MEDIA` ou `ALTA`, sem valor padrão.

#### Scenario: Prioridade ausente
- **WHEN** o cliente cria ou atualiza uma tarefa sem `prioridade`
- **THEN** o sistema responde `400` com erro de validação apontando o campo `prioridade`

#### Scenario: Prioridade fora do enum
- **WHEN** o cliente envia `prioridade` `"URGENTE"`
- **THEN** o sistema responde `400` com erro identificando o campo `prioridade`

### Requirement: Listagem de tarefas
O sistema SHALL listar todas as tarefas via `GET /api/tarefas`, ordenadas por `dataVencimento` crescente e, em caso de empate, por prioridade na ordem `ALTA`, `MEDIA`, `BAIXA`.

#### Scenario: Ordenação por vencimento e prioridade
- **WHEN** existem as tarefas A (vence 2026-10-20, `BAIXA`), B (vence 2026-10-19, `MEDIA`) e C (vence 2026-10-20, `ALTA`)
- **THEN** o sistema retorna a lista na ordem B, C, A

#### Scenario: Nenhuma tarefa cadastrada
- **WHEN** não existe nenhuma tarefa
- **THEN** o sistema responde `200` com uma lista vazia

### Requirement: Consulta de tarefa por id
O sistema SHALL retornar uma tarefa via `GET /api/tarefas/{id}` e SHALL responder `404` quando o `id` não existir.

#### Scenario: Tarefa existente
- **WHEN** o cliente consulta o `id` de uma tarefa existente
- **THEN** o sistema responde `200` com a tarefa, incluindo `situacaoPrazo`

#### Scenario: Tarefa inexistente
- **WHEN** o cliente consulta um `id` que não existe
- **THEN** o sistema responde `404` no formato de erro padronizado

### Requirement: Atualização de tarefa
O sistema SHALL substituir `titulo`, `descricao`, `dataVencimento`, `prioridade` e `status` de uma tarefa via `PUT /api/tarefas/{id}`, aplicando as mesmas validações da criação, exigindo `status` com valor `PENDENTE` ou `CONCLUIDA`, e responder `200` com a tarefa atualizada.

#### Scenario: Atualização válida
- **WHEN** o cliente envia um `PUT` válido para uma tarefa existente
- **THEN** o sistema responde `200` com a tarefa contendo os novos valores

#### Scenario: Status ausente ou inválido na atualização
- **WHEN** o cliente envia um `PUT` sem `status` ou com `status` `"ATRASADA"`
- **THEN** o sistema responde `400` com erro identificando o campo `status`

#### Scenario: Atualização de tarefa inexistente
- **WHEN** o cliente envia um `PUT` para um `id` que não existe
- **THEN** o sistema responde `404` no formato de erro padronizado

#### Scenario: Campos controlados pelo sistema são ignorados na atualização
- **WHEN** o cliente envia `id` ou `dataConclusao` no corpo do `PUT`
- **THEN** o sistema ignora esses valores

### Requirement: Controle da data de conclusão
O sistema SHALL definir `dataConclusao` como a data atual quando o `status` de uma tarefa muda de `PENDENTE` para `CONCLUIDA`, SHALL preservar `dataConclusao` em atualizações que mantêm o `status` `CONCLUIDA`, e SHALL tornar `dataConclusao` nula quando a tarefa é reaberta (de `CONCLUIDA` para `PENDENTE`).

#### Scenario: Concluir tarefa
- **WHEN** a data atual é 2026-10-18 e o cliente atualiza uma tarefa `PENDENTE` para `CONCLUIDA`
- **THEN** a tarefa passa a ter `status` `CONCLUIDA` e `dataConclusao` `2026-10-18`

#### Scenario: Editar tarefa já concluída
- **WHEN** uma tarefa foi concluída em 2026-10-15 e, em 2026-10-18, o cliente altera só o título mantendo `status` `CONCLUIDA`
- **THEN** a tarefa mantém `dataConclusao` `2026-10-15`

#### Scenario: Reabrir tarefa
- **WHEN** o cliente atualiza uma tarefa `CONCLUIDA` para `PENDENTE`
- **THEN** a tarefa passa a ter `status` `PENDENTE` e `dataConclusao` `null`

#### Scenario: Concluir novamente após reabrir
- **WHEN** uma tarefa concluída em 2026-10-15 é reaberta e concluída de novo em 2026-10-18
- **THEN** a tarefa passa a ter `dataConclusao` `2026-10-18`

### Requirement: Exclusão de tarefa
O sistema SHALL excluir uma tarefa via `DELETE /api/tarefas/{id}` respondendo `204 No Content`, e SHALL responder `404` quando o `id` não existir.

#### Scenario: Exclusão de tarefa existente
- **WHEN** o cliente exclui uma tarefa existente
- **THEN** o sistema responde `204` e a tarefa deixa de aparecer na listagem e na consulta por id

#### Scenario: Exclusão de tarefa inexistente
- **WHEN** o cliente exclui um `id` que não existe
- **THEN** o sistema responde `404` no formato de erro padronizado

### Requirement: Formato padronizado de erro
O sistema SHALL responder todos os erros da API no formato RFC 7807 (`application/problem+json`) com `type`, `title`, `status` e `detail`; erros de validação SHALL incluir `errors`, uma lista de objetos `{"campo", "mensagem"}`, um por campo inválido.

#### Scenario: Vários campos inválidos
- **WHEN** o cliente cria uma tarefa com `titulo` vazio e sem `prioridade`
- **THEN** o sistema responde `400` com `errors` contendo uma entrada para `titulo` e outra para `prioridade`

#### Scenario: Corpo JSON malformado
- **WHEN** o cliente envia um corpo que não é JSON válido
- **THEN** o sistema responde `400` no formato de erro padronizado

#### Scenario: Identificador em formato inválido
- **WHEN** o cliente usa em `/api/tarefas/{id}` um valor que não é um identificador válido
- **THEN** o sistema responde `400` no formato de erro padronizado

#### Scenario: Erro inesperado
- **WHEN** ocorre uma falha interna não prevista
- **THEN** o sistema responde `500` no formato de erro padronizado, sem expor stack trace ou mensagens internas
