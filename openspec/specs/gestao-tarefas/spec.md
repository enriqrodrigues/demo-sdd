# gestao-tarefas Specification

## Purpose

Define o ciclo de vida de uma tarefa pessoal pela API REST: criação, consulta, listagem, edição, conclusão, reabertura e exclusão, com validação estrita da entrada, controle de concorrência otimista e formato padronizado de erro.

## Requirements

### Requirement: Representação da tarefa
A API SHALL representar cada tarefa em JSON com os campos `id` (UUID), `titulo`, `descricao` (string ou `null`), `dataVencimento` (`YYYY-MM-DD`), `prioridade` (`BAIXA`, `MEDIA` ou `ALTA`), `status` (`PENDENTE` ou `CONCLUIDA`), `concluidaEm` (instante ISO-8601 com offset, ou `null`), `situacaoPrazo` (ver capability `prazo-tarefa`) e `version` (inteiro).

#### Scenario: Tarefa pendente representada
- **WHEN** o cliente consulta uma tarefa pendente
- **THEN** a resposta contém todos os campos listados, com `status` `PENDENTE` e `concluidaEm` `null`

#### Scenario: Tarefa concluída representada
- **WHEN** o cliente consulta uma tarefa concluída
- **THEN** a resposta contém `status` `CONCLUIDA` e `concluidaEm` com o instante da conclusão, incluindo offset

### Requirement: Criar tarefa
A API SHALL criar uma tarefa via `POST /api/tarefas` a partir de `titulo`, `descricao`, `dataVencimento` e `prioridade`, respondendo 201 com a tarefa criada no corpo e o cabeçalho `Location` apontando para `/api/tarefas/{id}`. Toda tarefa criada SHALL nascer com `status` `PENDENTE` e `concluidaEm` `null`.

#### Scenario: Criação válida
- **WHEN** o cliente envia `{"titulo":"Pagar conta","dataVencimento":"2026-10-25","prioridade":"ALTA"}`
- **THEN** a API responde 201, o corpo traz um `id` gerado, `status` `PENDENTE`, `concluidaEm` `null` e `descricao` `null`
- **AND** o cabeçalho `Location` é `/api/tarefas/{id}` com o mesmo `id`

#### Scenario: Status enviado na criação é ignorado
- **WHEN** o cliente cria uma tarefa válida enviando também `"status":"CONCLUIDA"`
- **THEN** a tarefa é criada com `status` `PENDENTE` e `concluidaEm` `null`

#### Scenario: Campos somente leitura enviados na criação são ignorados
- **WHEN** o cliente cria uma tarefa válida enviando também `id`, `concluidaEm`, `situacaoPrazo` e `version`
- **THEN** a tarefa é criada com `id` gerado pelo servidor, `concluidaEm` `null` e os demais campos calculados pelo servidor

#### Scenario: Vencimento no passado é aceito
- **WHEN** o cliente cria uma tarefa com `dataVencimento` anterior à data corrente
- **THEN** a API responde 201

### Requirement: Validação do título
O campo `titulo` SHALL ser obrigatório e, após remoção de espaços nas extremidades, ter entre 1 e 100 caracteres. O valor persistido SHALL ser o valor sem os espaços das extremidades.

#### Scenario: Título ausente
- **WHEN** o cliente cria uma tarefa sem `titulo`
- **THEN** a API responde 400 com um erro de campo para `titulo`

#### Scenario: Título só com espaços
- **WHEN** o cliente cria uma tarefa com `titulo` `"   "`
- **THEN** a API responde 400 com um erro de campo para `titulo`

#### Scenario: Título com 100 caracteres
- **WHEN** o cliente cria uma tarefa com `titulo` de exatamente 100 caracteres
- **THEN** a API responde 201

#### Scenario: Título com 101 caracteres
- **WHEN** o cliente cria uma tarefa com `titulo` de 101 caracteres
- **THEN** a API responde 400 com um erro de campo para `titulo`

#### Scenario: Espaços nas extremidades são removidos
- **WHEN** o cliente cria uma tarefa com `titulo` `"  Revisar PR  "`
- **THEN** a tarefa é criada com `titulo` `"Revisar PR"`

### Requirement: Validação da descrição
O campo `descricao` SHALL ser opcional e ter no máximo 2000 caracteres. Descrição ausente, `null`, vazia ou só com espaços SHALL ser armazenada como `null`.

#### Scenario: Descrição com 2000 caracteres
- **WHEN** o cliente cria uma tarefa com `descricao` de exatamente 2000 caracteres
- **THEN** a API responde 201

#### Scenario: Descrição com 2001 caracteres
- **WHEN** o cliente cria uma tarefa com `descricao` de 2001 caracteres
- **THEN** a API responde 400 com um erro de campo para `descricao`

#### Scenario: Descrição vazia vira nula
- **WHEN** o cliente cria uma tarefa com `descricao` `""`
- **THEN** a tarefa é criada com `descricao` `null`

#### Scenario: Descrição só com espaços vira nula
- **WHEN** o cliente cria uma tarefa com `descricao` `"   "`
- **THEN** a tarefa é criada com `descricao` `null`

### Requirement: Validação da data de vencimento
O campo `dataVencimento` SHALL ser obrigatório e aceito apenas como data de calendário válida no formato estrito `YYYY-MM-DD`.

#### Scenario: Vencimento ausente
- **WHEN** o cliente cria uma tarefa sem `dataVencimento`
- **THEN** a API responde 400 com um erro de campo para `dataVencimento`

#### Scenario: Vencimento em formato diferente
- **WHEN** o cliente envia `dataVencimento` `"25/10/2026"` ou `"2026-10-25T10:00:00"`
- **THEN** a API responde 400 com um erro de campo para `dataVencimento`

#### Scenario: Data inexistente
- **WHEN** o cliente envia `dataVencimento` `"2026-02-30"`
- **THEN** a API responde 400 com um erro de campo para `dataVencimento`

### Requirement: Validação da prioridade
O campo `prioridade` SHALL ser obrigatório e aceitar apenas as strings exatas `BAIXA`, `MEDIA` e `ALTA`. Valores numéricos, variações de caixa e quaisquer outros valores SHALL ser rejeitados.

#### Scenario: Prioridade ausente
- **WHEN** o cliente cria uma tarefa sem `prioridade`
- **THEN** a API responde 400 com um erro de campo para `prioridade`

#### Scenario: Prioridade numérica
- **WHEN** o cliente envia `prioridade` `0` ou `"2"`
- **THEN** a API responde 400 com um erro de campo para `prioridade`

#### Scenario: Prioridade em minúsculas
- **WHEN** o cliente envia `prioridade` `"alta"`
- **THEN** a API responde 400 com um erro de campo para `prioridade`

### Requirement: Campos desconhecidos rejeitados
A API SHALL rejeitar com 400 qualquer corpo de criação ou edição que contenha propriedades fora do conjunto de campos da tarefa.

#### Scenario: Propriedade desconhecida
- **WHEN** o cliente cria uma tarefa válida enviando também `"responsavel":"ana"`
- **THEN** a API responde 400 e nenhuma tarefa é criada

### Requirement: Consultar tarefa
A API SHALL retornar uma tarefa via `GET /api/tarefas/{id}`, respondendo 404 quando não existir tarefa com aquele `id` e 400 quando o `id` não for um UUID válido.

#### Scenario: Tarefa existente
- **WHEN** o cliente consulta o `id` de uma tarefa existente
- **THEN** a API responde 200 com a tarefa

#### Scenario: Tarefa inexistente
- **WHEN** o cliente consulta um UUID que não corresponde a nenhuma tarefa
- **THEN** a API responde 404

#### Scenario: Identificador malformado
- **WHEN** o cliente consulta `/api/tarefas/abc`
- **THEN** a API responde 400

### Requirement: Listar tarefas
A API SHALL listar todas as tarefas via `GET /api/tarefas`, sem paginação, ordenadas por `dataVencimento` crescente, depois por prioridade decrescente (`ALTA`, `MEDIA`, `BAIXA`) e depois por `titulo` crescente.

#### Scenario: Lista vazia
- **WHEN** não há tarefas e o cliente lista
- **THEN** a API responde 200 com um array vazio

#### Scenario: Ordenação
- **WHEN** existem as tarefas A (vence 2026-10-21, BAIXA), B (vence 2026-10-20, BAIXA) e C (vence 2026-10-21, ALTA)
- **THEN** a lista retorna na ordem B, C, A

### Requirement: Filtrar a listagem
A listagem SHALL aceitar os parâmetros opcionais `status` e `situacaoPrazo`, combináveis, retornando apenas as tarefas que atendem a todos os filtros informados. Valor de filtro fora do enum correspondente SHALL resultar em 400.

#### Scenario: Filtro por status
- **WHEN** o cliente lista com `?status=PENDENTE`
- **THEN** apenas tarefas pendentes são retornadas

#### Scenario: Filtro por situação de prazo
- **WHEN** o cliente lista com `?situacaoPrazo=ATRASADA`
- **THEN** apenas tarefas cuja situação de prazo corrente é `ATRASADA` são retornadas

#### Scenario: Filtro inválido
- **WHEN** o cliente lista com `?situacaoPrazo=VENCIDA`
- **THEN** a API responde 400

### Requirement: Editar tarefa
A API SHALL atualizar `titulo`, `descricao`, `dataVencimento` e `prioridade` via `PUT /api/tarefas/{id}`, substituindo todos os campos editáveis com as mesmas validações da criação. A edição SHALL NOT alterar `status` nem `concluidaEm`; esses campos, se enviados, são ignorados. Tarefas concluídas também podem ser editadas.

#### Scenario: Edição válida
- **WHEN** o cliente envia um `PUT` válido, que altera algum campo, com a `version` atual da tarefa
- **THEN** a API responde 200 com a tarefa atualizada e `version` incrementada

#### Scenario: Status enviado na edição é ignorado
- **WHEN** o cliente edita uma tarefa pendente enviando `"status":"CONCLUIDA"`
- **THEN** a tarefa permanece `PENDENTE` com `concluidaEm` `null`

#### Scenario: Edição inválida
- **WHEN** o cliente edita uma tarefa com `titulo` de 101 caracteres
- **THEN** a API responde 400 e a tarefa não é alterada

#### Scenario: Edição de tarefa inexistente
- **WHEN** o cliente edita um `id` que não existe
- **THEN** a API responde 404

### Requirement: Concorrência otimista na edição
Toda edição SHALL informar a `version` que o cliente conhece. A API SHALL responder 400 se `version` estiver ausente e 409 se for diferente da versão atual, sem alterar a tarefa. Toda alteração efetiva de uma tarefa (edição, conclusão ou reabertura) SHALL incrementar `version`.

#### Scenario: Versão ausente
- **WHEN** o cliente envia um `PUT` sem `version`
- **THEN** a API responde 400 com um erro de campo para `version`

#### Scenario: Editar enquanto outra ação conclui
- **WHEN** o cliente A lê a tarefa com `version` 3, o cliente B conclui a tarefa (que passa a `version` 4) e então A envia um `PUT` com `version` 3
- **THEN** a API responde 409, a tarefa continua concluída e os campos não são alterados por A

#### Scenario: Duas edições simultâneas
- **WHEN** dois clientes leem a tarefa com `version` 3 e ambos enviam `PUT` com `version` 3
- **THEN** o primeiro recebe 200 e o segundo recebe 409

### Requirement: Concluir tarefa
A API SHALL concluir uma tarefa via `POST /api/tarefas/{id}/concluir`, definindo `status` `CONCLUIDA` e `concluidaEm` com o instante corrente do servidor, e respondendo 200 com a tarefa. Concluir uma tarefa já concluída SHALL ser idempotente: responde 200 sem alterar `concluidaEm` nem `version`.

#### Scenario: Concluir tarefa pendente
- **WHEN** o cliente conclui uma tarefa pendente
- **THEN** a tarefa passa a `CONCLUIDA`, `concluidaEm` recebe o instante corrente e `version` é incrementada

#### Scenario: Concluir duas vezes
- **WHEN** o cliente conclui uma tarefa e, depois, envia de novo o pedido de conclusão
- **THEN** a segunda resposta é 200 com o mesmo `concluidaEm` e a mesma `version` da primeira

#### Scenario: Concluir tarefa inexistente
- **WHEN** o cliente conclui um `id` que não existe
- **THEN** a API responde 404

#### Scenario: Concluir tarefa excluída em paralelo
- **WHEN** um cliente exclui a tarefa e outro tenta concluí-la em seguida
- **THEN** a conclusão responde 404

### Requirement: Reabrir tarefa
A API SHALL reabrir uma tarefa via `POST /api/tarefas/{id}/reabrir`, definindo `status` `PENDENTE` e `concluidaEm` `null`, e respondendo 200. Reabrir uma tarefa já pendente SHALL ser idempotente: responde 200 sem alterar `version`.

#### Scenario: Reabrir tarefa concluída
- **WHEN** o cliente reabre uma tarefa concluída
- **THEN** a tarefa passa a `PENDENTE`, `concluidaEm` vira `null` e `version` é incrementada

#### Scenario: Reabrir tarefa pendente
- **WHEN** o cliente reabre uma tarefa já pendente
- **THEN** a API responde 200 sem alterar a tarefa nem `version`

#### Scenario: Concluir de novo após reabrir
- **WHEN** uma tarefa concluída é reaberta e concluída novamente
- **THEN** `concluidaEm` passa a ser o instante da nova conclusão

#### Scenario: Reabrir tarefa inexistente
- **WHEN** o cliente reabre um `id` que não existe
- **THEN** a API responde 404

### Requirement: Excluir tarefa
A API SHALL excluir definitivamente uma tarefa via `DELETE /api/tarefas/{id}`, respondendo 204, e responder 404 quando a tarefa não existir.

#### Scenario: Excluir tarefa existente
- **WHEN** o cliente exclui uma tarefa existente
- **THEN** a API responde 204 e uma consulta posterior ao mesmo `id` responde 404

#### Scenario: Excluir tarefa inexistente
- **WHEN** o cliente exclui um `id` que não existe
- **THEN** a API responde 404

#### Scenario: Editar tarefa excluída
- **WHEN** um cliente exclui a tarefa e outro envia um `PUT` para ela em seguida
- **THEN** a edição responde 404

### Requirement: Formato padronizado de erro
Toda resposta de erro (4xx e 5xx) SHALL usar `Content-Type: application/problem+json` com os campos `type`, `title`, `status` e `detail`. Erros de validação (400) SHALL incluir `erros`, uma lista de objetos `{campo, mensagem}` com todos os campos inválidos. Erros 5xx SHALL NOT expor stack trace ou detalhes internos.

#### Scenario: Vários campos inválidos
- **WHEN** o cliente cria uma tarefa sem `titulo` e com `prioridade` `"URGENTE"`
- **THEN** a API responde 400 em Problem Details, e `erros` contém uma entrada para `titulo` e outra para `prioridade`

#### Scenario: JSON malformado
- **WHEN** o cliente envia um corpo que não é JSON válido
- **THEN** a API responde 400 em Problem Details

#### Scenario: Recurso não encontrado
- **WHEN** a API responde 404 a uma operação sobre tarefa
- **THEN** o corpo está em Problem Details com `status` 404

#### Scenario: Conflito de versão
- **WHEN** a API responde 409 a uma edição
- **THEN** o corpo está em Problem Details com `status` 409

#### Scenario: Erro inesperado
- **WHEN** ocorre uma falha interna não tratada
- **THEN** a API responde 500 em Problem Details sem stack trace, nome de classe ou SQL no corpo
