# Spec Delta

## Purpose

Define a aplicação web de página única em pt-BR com a qual o usuário gerencia suas tarefas e acompanha o score semanal, incluindo como a interface se comporta quando ações concorrentes ou repetidas atingem a mesma tarefa.

## ADDED Requirements

### Requirement: Lista de tarefas
A interface SHALL exibir as tarefas na ordem retornada pela API, mostrando para cada uma o título, a prioridade, a data de vencimento no formato `DD/MM/AAAA`, um indicador visual da situação de prazo e as ações de concluir/reabrir, editar e excluir.

#### Scenario: Exibição das tarefas
- **WHEN** a API retorna tarefas
- **THEN** cada tarefa aparece com título, prioridade, vencimento em `DD/MM/AAAA` e o rótulo da situação ("Atrasada", "Para hoje", "No prazo" ou "Concluída")

#### Scenario: Lista vazia
- **WHEN** a API retorna uma lista vazia
- **THEN** a interface exibe uma mensagem indicando que não há tarefas

### Requirement: Filtros da lista
A interface SHALL oferecer os filtros "Todas", "Pendentes", "Atrasadas" e "Para hoje", com apenas um ativo por vez, e exibir somente as tarefas correspondentes ao filtro ativo.

#### Scenario: Filtrar atrasadas
- **WHEN** o usuário seleciona "Atrasadas"
- **THEN** apenas tarefas com situação `ATRASADA` são exibidas

#### Scenario: Tarefa sai do filtro após ação
- **WHEN** o filtro "Atrasadas" está ativo e o usuário conclui uma tarefa atrasada
- **THEN** a tarefa deixa de ser exibida na lista filtrada

### Requirement: Criar tarefa pela interface
A interface SHALL permitir criar uma tarefa por um formulário com título, descrição, vencimento e prioridade, impedindo no próprio formulário a digitação além dos limites de 100 caracteres no título e 2000 na descrição. A nova tarefa SHALL aparecer na lista após a criação.

#### Scenario: Criação com sucesso
- **WHEN** o usuário preenche o formulário com dados válidos e salva
- **THEN** o formulário fecha e a nova tarefa aparece na lista

#### Scenario: Erros de validação da API
- **WHEN** a API responde 400 com `erros` para um ou mais campos
- **THEN** cada mensagem aparece junto ao campo correspondente, o formulário permanece aberto e os valores digitados são mantidos

#### Scenario: Duplo clique em salvar
- **WHEN** o usuário clica duas vezes rapidamente em salvar uma nova tarefa
- **THEN** apenas uma tarefa é criada, pois o botão fica desabilitado enquanto a requisição está em andamento

#### Scenario: Criação com filtro ativo
- **WHEN** o filtro "Atrasadas" está ativo e o usuário cria uma tarefa que vence no futuro
- **THEN** o filtro passa a "Todas" e a nova tarefa aparece na lista

### Requirement: Editar tarefa pela interface
A interface SHALL permitir editar uma tarefa existente no mesmo formulário, preenchido com os valores atuais, enviando a `version` da tarefa carregada. Enquanto o formulário de uma tarefa estiver aberto, as ações de concluir/reabrir e excluir dessa mesma tarefa SHALL ficar desabilitadas na lista.

#### Scenario: Edição com sucesso
- **WHEN** o usuário altera o título de uma tarefa e salva
- **THEN** o formulário fecha e a lista mostra o novo título

#### Scenario: Ações bloqueadas durante a edição
- **WHEN** o formulário de edição da tarefa X está aberto
- **THEN** os controles de concluir/reabrir e excluir da tarefa X estão desabilitados, e os das demais tarefas continuam habilitados

#### Scenario: Cancelar edição
- **WHEN** o usuário altera campos e cancela o formulário
- **THEN** nenhuma requisição de edição é enviada e a tarefa permanece como estava

### Requirement: Conflito de edição
Quando a API responder 409 a uma edição, a interface SHALL informar que a tarefa foi alterada em outro lugar, recarregar a tarefa e manter o formulário aberto com os valores digitados pelo usuário, de modo que ele possa salvar de novo sobre a versão recarregada.

#### Scenario: Editar enquanto a tarefa é concluída em outra aba
- **WHEN** o usuário abre a edição da tarefa X, a tarefa X é concluída em outra aba e o usuário salva
- **THEN** a interface exibe o aviso de conflito, a tarefa X aparece como concluída na lista e o formulário mantém os valores digitados

#### Scenario: Salvar de novo após o conflito
- **WHEN** após o aviso de conflito o usuário salva novamente
- **THEN** a edição é enviada com a `version` recarregada e é aplicada

### Requirement: Tarefa removida durante uma ação
Quando a API responder 404 a uma edição, conclusão, reabertura ou exclusão, a interface SHALL informar que a tarefa não existe mais, removê-la da lista e fechar o formulário dela, se estiver aberto.

#### Scenario: Editar tarefa excluída em outra aba
- **WHEN** o usuário está editando a tarefa X, a tarefa X é excluída em outra aba e o usuário salva
- **THEN** a interface avisa que a tarefa não existe mais, fecha o formulário e remove X da lista

#### Scenario: Concluir tarefa excluída em outra aba
- **WHEN** a tarefa X é excluída em outra aba e o usuário clica em concluir X
- **THEN** a interface avisa que a tarefa não existe mais e remove X da lista

### Requirement: Concluir e reabrir pela interface
A interface SHALL permitir concluir uma tarefa pendente e reabrir uma tarefa concluída por um controle na própria linha, atualizando a linha com a resposta da API. O controle SHALL ficar desabilitado enquanto a requisição estiver em andamento.

#### Scenario: Concluir tarefa
- **WHEN** o usuário conclui uma tarefa pendente
- **THEN** a linha passa a mostrar a situação "Concluída"

#### Scenario: Reabrir tarefa
- **WHEN** o usuário reabre uma tarefa concluída cujo vencimento já passou
- **THEN** a linha passa a mostrar a situação "Atrasada"

#### Scenario: Duplo clique em concluir
- **WHEN** o usuário clica duas vezes rapidamente no controle de concluir
- **THEN** apenas uma requisição de conclusão é enviada e a tarefa termina concluída, não reaberta

### Requirement: Excluir pela interface
A interface SHALL pedir confirmação antes de excluir uma tarefa e só enviar a exclusão se o usuário confirmar.

#### Scenario: Confirmar exclusão
- **WHEN** o usuário clica em excluir e confirma
- **THEN** a tarefa é excluída e some da lista

#### Scenario: Cancelar exclusão
- **WHEN** o usuário clica em excluir e cancela a confirmação
- **THEN** nenhuma requisição é enviada e a tarefa continua na lista

### Requirement: Score no cabeçalho
A interface SHALL exibir no cabeçalho o score da semana corrente, com o percentual, a contagem "N de M no prazo" e o período da semana. Quando `percentual` for `null`, SHALL exibir "Sem tarefas finalizadas esta semana". O score SHALL ser recarregado após cada criação, edição, conclusão, reabertura ou exclusão bem-sucedida.

#### Scenario: Score com dados
- **WHEN** a API retorna `concluidas` 3, `noPrazo` 2, `percentual` 66.67, de 2026-10-19 a 2026-10-25
- **THEN** o cabeçalho mostra "66,67%", "2 de 3 no prazo" e "19/10 a 25/10"

#### Scenario: Score sem dados
- **WHEN** a API retorna `percentual` `null`
- **THEN** o cabeçalho mostra "Sem tarefas finalizadas esta semana"

#### Scenario: Score atualizado após concluir
- **WHEN** o usuário conclui uma tarefa
- **THEN** o score é consultado de novo e o cabeçalho reflete o novo valor

### Requirement: Falha de comunicação
Quando uma requisição falhar por erro de rede ou resposta 5xx, a interface SHALL exibir uma mensagem genérica de erro, manter o estado exibido anterior à ação e reabilitar o controle acionado.

#### Scenario: Servidor indisponível ao concluir
- **WHEN** o usuário conclui uma tarefa e a requisição falha por erro de rede
- **THEN** a interface exibe a mensagem de erro, a tarefa continua exibida como pendente e o controle volta a ficar habilitado
