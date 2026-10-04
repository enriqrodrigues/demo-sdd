# Spec Delta

## Purpose

Dá ao usuário autenticado uma área para consultar seus dados e manter atualizados o telefone e o endereço, preservando inalterados os dados críticos informados no cadastro.

## ADDED Requirements

### Requirement: Acesso ao perfil exige autenticação
A área de perfil e a API de perfil SHALL estar disponíveis somente para usuários autenticados. Sem sessão válida, a API SHALL recusar a requisição como não autenticada e a interface SHALL levar o visitante à página de login.

#### Scenario: Perfil sem sessão na interface
- **WHEN** um visitante sem sessão abre a página de perfil
- **THEN** o sistema o leva à página de login

#### Scenario: Perfil sem sessão na API
- **WHEN** um cliente sem sessão consulta ou tenta alterar o perfil pela API
- **THEN** o sistema recusa a requisição como não autenticada e nenhum dado é alterado

#### Scenario: Acesso pela área interna
- **WHEN** o usuário autenticado está na área interna
- **THEN** o sistema oferece um link para a página de perfil

### Requirement: Visualização do perfil
O sistema SHALL exibir ao usuário autenticado os seus dados: nome, CPF, e-mail, data de nascimento, telefone e endereço (CEP, logradouro, número, complemento, bairro, cidade e UF). Nome, CPF, e-mail e data de nascimento SHALL ser apresentados como somente leitura.

#### Scenario: Usuário abre o perfil
- **WHEN** o usuário autenticado abre a página de perfil
- **THEN** o sistema exibe todos os seus dados, com CPF, telefone e CEP formatados, e nome, CPF, e-mail e data de nascimento sem opção de edição

### Requirement: Perfil restrito ao próprio usuário
O perfil exibido e alterado SHALL ser sempre o do usuário da sessão. O sistema SHALL NOT oferecer meio de consultar ou alterar o perfil de outro usuário.

#### Scenario: Dois usuários
- **WHEN** dois usuários autenticados, cada um na sua sessão, abrem o perfil
- **THEN** cada um vê somente os próprios dados

#### Scenario: Alteração afeta apenas o próprio usuário
- **WHEN** um usuário autenticado altera o telefone no perfil
- **THEN** somente o telefone desse usuário é alterado

### Requirement: Edição de telefone e endereço
O sistema SHALL permitir que o usuário autenticado altere o telefone e os campos de endereço. Os dados alterados SHALL ser gravados e o sistema SHALL confirmar a alteração. O complemento SHALL poder ser preenchido, alterado ou removido.

#### Scenario: Alteração bem-sucedida
- **WHEN** o usuário altera o telefone para `(21) 3456-7890` e o CEP para `20040-020` e salva
- **THEN** o sistema grava `2134567890` e `20040020`, exibe a confirmação "Dados atualizados com sucesso." e, ao reabrir o perfil, mostra os novos valores

#### Scenario: Remoção do complemento
- **WHEN** o usuário apaga o complemento e salva
- **THEN** o sistema grava o endereço sem complemento

### Requirement: Validação da edição
A edição SHALL aplicar as mesmas regras do cadastro para os campos editáveis: telefone com 10 dígitos (fixo) ou 11 dígitos (celular) incluindo o DDD; CEP com 8 dígitos; UF entre as 27 siglas válidas; todos obrigatórios exceto o complemento; e os mesmos tamanhos máximos. Uma alteração inválida SHALL ser recusada sem gravar nenhum campo, indicando cada campo inválido e o motivo.

#### Scenario: Telefone inválido
- **WHEN** o usuário informa o telefone `(21) 3456-789` e salva
- **THEN** o sistema recusa a alteração, indica que o telefone deve ter 10 ou 11 dígitos incluindo o DDD, e nenhum dado é alterado

#### Scenario: Campo obrigatório vazio
- **WHEN** o usuário apaga a cidade e salva
- **THEN** o sistema recusa a alteração e indica que a cidade é obrigatória

#### Scenario: Validação em tempo real
- **WHEN** o usuário digita um CEP com 7 dígitos e sai do campo
- **THEN** o formulário exibe a mensagem de CEP inválido junto ao campo, sem enviar a alteração

### Requirement: Imutabilidade dos dados críticos
Nome, CPF, e-mail e data de nascimento SHALL NOT ser alterados após o cadastro (RN01). Uma requisição de alteração de perfil que inclua qualquer um desses campos SHALL ser recusada, indicando que o campo não pode ser alterado, sem gravar nenhum dado.

#### Scenario: Tentativa de alterar o e-mail pela API
- **WHEN** um cliente autenticado envia uma alteração de perfil contendo um novo e-mail
- **THEN** o sistema recusa a requisição indicando que o e-mail não pode ser alterado, e nenhum dado do usuário é modificado

#### Scenario: Tentativa de alterar nome, CPF e data de nascimento
- **WHEN** um cliente autenticado envia uma alteração de perfil contendo nome, CPF e data de nascimento
- **THEN** o sistema recusa a requisição indicando cada um desses campos como não alterável

#### Scenario: Interface sem edição de dados críticos
- **WHEN** o usuário abre a página de perfil
- **THEN** nome, CPF, e-mail e data de nascimento não estão disponíveis como campos editáveis
