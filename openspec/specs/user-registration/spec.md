# user-registration Specification

## Purpose

Permite que um visitante crie uma conta informando seus dados pessoais, de contato e de endereço, garantindo que apenas dados válidos e únicos sejam aceitos e que a conta nasça pendente de ativação.

## Requirements

### Requirement: Formulário de cadastro
O sistema SHALL disponibilizar uma página web pública com um formulário de cadastro contendo os campos: nome, CPF, e-mail, data de nascimento, senha, telefone, CEP, logradouro, número, complemento, bairro, cidade e UF.

#### Scenario: Visitante acessa o cadastro
- **WHEN** um visitante não autenticado abre a página de cadastro
- **THEN** o sistema exibe o formulário com todos os campos listados, com o complemento identificado como opcional

### Requirement: Campos obrigatórios
O sistema SHALL exigir o preenchimento de todos os campos do cadastro, exceto o complemento. Valores compostos apenas por espaços SHALL ser tratados como vazios.

#### Scenario: Campo obrigatório vazio
- **WHEN** o visitante envia o cadastro com o nome vazio ou só com espaços
- **THEN** o sistema rejeita o cadastro e indica que o nome é obrigatório

#### Scenario: Complemento vazio
- **WHEN** o visitante envia um cadastro válido com o complemento vazio
- **THEN** o sistema aceita o cadastro

### Requirement: Validação de CPF
O sistema SHALL aceitar apenas CPFs com 11 dígitos e dígitos verificadores corretos, recusando sequências de dígitos repetidos. A entrada SHALL ser aceita com ou sem a máscara `000.000.000-00`, e o CPF SHALL ser armazenado somente com os dígitos.

#### Scenario: CPF válido com máscara
- **WHEN** o visitante informa o CPF `529.982.247-25`
- **THEN** o sistema aceita o CPF e o armazena como `52998224725`

#### Scenario: Dígito verificador incorreto
- **WHEN** o visitante informa o CPF `529.982.247-26`
- **THEN** o sistema rejeita o cadastro e indica que o CPF é inválido

#### Scenario: Dígitos repetidos
- **WHEN** o visitante informa o CPF `111.111.111-11`
- **THEN** o sistema rejeita o cadastro e indica que o CPF é inválido

### Requirement: Validação de e-mail
O sistema SHALL aceitar apenas e-mails em formato válido (`local@dominio` com domínio contendo ao menos um ponto) e com no máximo 254 caracteres. O e-mail SHALL ser armazenado sem espaços nas pontas e em letras minúsculas.

#### Scenario: E-mail sem domínio
- **WHEN** o visitante informa o e-mail `maria@`
- **THEN** o sistema rejeita o cadastro e indica que o e-mail é inválido

#### Scenario: E-mail normalizado
- **WHEN** o visitante informa o e-mail ` Maria@Exemplo.com `
- **THEN** o sistema armazena o e-mail como `maria@exemplo.com`

### Requirement: Validação de data de nascimento
O sistema SHALL aceitar qualquer data de nascimento válida que não seja posterior à data atual. Nenhuma idade mínima ou máxima SHALL ser exigida.

#### Scenario: Data futura
- **WHEN** o visitante informa uma data de nascimento posterior à data atual
- **THEN** o sistema rejeita o cadastro e indica que a data de nascimento não pode ser futura

#### Scenario: Data de hoje
- **WHEN** o visitante informa a data atual como data de nascimento
- **THEN** o sistema aceita a data

### Requirement: Força da senha
O sistema SHALL exigir senhas com 8 a 64 caracteres contendo ao menos uma letra maiúscula, uma letra minúscula, um dígito e um caractere especial (qualquer caractere que não seja letra nem dígito).

#### Scenario: Senha forte
- **WHEN** o visitante informa a senha `Segura@123`
- **THEN** o sistema aceita a senha

#### Scenario: Senha sem caractere especial
- **WHEN** o visitante informa a senha `Segura1234`
- **THEN** o sistema rejeita o cadastro e indica qual critério de força não foi atendido

#### Scenario: Senha curta
- **WHEN** o visitante informa a senha `Se@1`
- **THEN** o sistema rejeita o cadastro e indica que a senha deve ter ao menos 8 caracteres

### Requirement: Validação de telefone
O sistema SHALL aceitar telefones fixos com 10 dígitos (DDD de 2 dígitos + número de 8 dígitos) e celulares com 11 dígitos (DDD de 2 dígitos + número de 9 dígitos). A entrada SHALL ser aceita com ou sem máscara, e o telefone SHALL ser armazenado somente com os dígitos.

#### Scenario: Celular válido com máscara
- **WHEN** o visitante informa o telefone `(11) 98765-4321`
- **THEN** o sistema aceita o telefone e o armazena como `11987654321`

#### Scenario: Fixo válido com máscara
- **WHEN** o visitante informa o telefone `(11) 3456-7890`
- **THEN** o sistema aceita o telefone e o armazena como `1134567890`

#### Scenario: Telefone com quantidade inválida de dígitos
- **WHEN** o visitante informa o telefone `(11) 3456-789`
- **THEN** o sistema rejeita o cadastro e indica que o telefone deve ter 10 dígitos (fixo) ou 11 dígitos (celular), incluindo o DDD

### Requirement: Validação de endereço
O sistema SHALL aceitar apenas CEPs com exatamente 8 dígitos, informados com ou sem hífen e armazenados somente com os dígitos, sem consulta a serviço externo. A UF SHALL ser uma das 27 siglas de unidades federativas do Brasil.

#### Scenario: CEP com hífen
- **WHEN** o visitante informa o CEP `01310-100`
- **THEN** o sistema aceita o CEP e o armazena como `01310100`

#### Scenario: CEP com 7 dígitos
- **WHEN** o visitante informa o CEP `0131010`
- **THEN** o sistema rejeita o cadastro e indica que o CEP deve ter 8 dígitos

#### Scenario: UF inexistente
- **WHEN** o visitante informa a UF `XX`
- **THEN** o sistema rejeita o cadastro e indica que a UF é inválida

### Requirement: Validação em tempo real no formulário
O formulário SHALL validar cada campo enquanto o visitante o preenche, exibindo a mensagem de erro junto ao campo, e SHALL exibir o atendimento de cada critério de força da senha enquanto ela é digitada.

#### Scenario: Erro exibido ao sair do campo
- **WHEN** o visitante digita um CPF inválido e sai do campo
- **THEN** o formulário exibe a mensagem de CPF inválido junto ao campo, sem enviar o cadastro

#### Scenario: Critérios da senha
- **WHEN** o visitante digita uma senha que tem letras e dígitos mas nenhum caractere especial
- **THEN** o formulário mostra os critérios atendidos e destaca o critério de caractere especial como pendente

### Requirement: Validação definitiva no servidor
O servidor SHALL aplicar todas as regras de validação de forma independente do formulário. Um cadastro inválido SHALL ser rejeitado sem gravar dados, com uma resposta que identifique cada campo inválido e o motivo.

#### Scenario: Envio direto à API com dados inválidos
- **WHEN** um cliente envia à API de cadastro um CPF inválido e um telefone com 9 dígitos
- **THEN** o sistema rejeita a requisição, não grava nenhum usuário e retorna os erros dos campos CPF e telefone

### Requirement: Gravação do usuário como pendente
O sistema SHALL gravar o cadastro válido com status `PENDENTE` e SHALL armazenar a senha somente na forma de hash irreversível, nunca em texto puro.

#### Scenario: Cadastro bem-sucedido
- **WHEN** o visitante envia um cadastro válido com e-mail e CPF não utilizados
- **THEN** o sistema grava o usuário com status `PENDENTE`, a senha armazenada não é igual à senha informada, e o formulário informa que um e-mail de ativação foi enviado ao endereço cadastrado

### Requirement: Unicidade de e-mail e CPF
O sistema SHALL recusar um cadastro cujo e-mail ou CPF pertença a um usuário `ATIVO` ou a um usuário `PENDENTE` com link de ativação ainda válido, indicando qual campo está em conflito. A comparação de e-mail SHALL ignorar maiúsculas e minúsculas.

#### Scenario: E-mail de conta ativa
- **WHEN** o visitante tenta se cadastrar com o e-mail de um usuário `ATIVO`
- **THEN** o sistema rejeita o cadastro e indica que o e-mail já está cadastrado

#### Scenario: CPF de conta ativa
- **WHEN** o visitante tenta se cadastrar com o CPF de um usuário `ATIVO`
- **THEN** o sistema rejeita o cadastro e indica que o CPF já está cadastrado

#### Scenario: E-mail com caixa diferente
- **WHEN** existe um usuário `ATIVO` com `maria@exemplo.com` e o visitante tenta se cadastrar com `MARIA@exemplo.com`
- **THEN** o sistema rejeita o cadastro e indica que o e-mail já está cadastrado

#### Scenario: Cadastro pendente com link válido
- **WHEN** o visitante tenta se cadastrar com o e-mail de um usuário `PENDENTE` cujo link de ativação ainda não expirou
- **THEN** o sistema rejeita o cadastro e informa que existe um cadastro aguardando ativação e que o e-mail de ativação deve ser verificado

### Requirement: Substituição de cadastro pendente expirado
Quando todos os usuários em conflito de e-mail ou CPF com um novo cadastro estiverem `PENDENTE` com link de ativação expirado, o sistema SHALL removê-los e aceitar o novo cadastro.

#### Scenario: Recadastro após expiração
- **WHEN** existe um usuário `PENDENTE` com o mesmo e-mail cujo link expirou e o visitante envia um novo cadastro válido
- **THEN** o sistema remove o cadastro antigo, grava o novo como `PENDENTE` e envia um novo e-mail de ativação

#### Scenario: Conflito misto
- **WHEN** o e-mail do novo cadastro pertence a um `PENDENTE` expirado e o CPF pertence a um usuário `ATIVO`
- **THEN** o sistema rejeita o cadastro indicando que o CPF já está cadastrado e mantém o cadastro pendente expirado inalterado

### Requirement: Cadastro só é concluído com o e-mail enviado
Se o e-mail de ativação não puder ser enviado, o sistema SHALL desfazer o cadastro e informar que o cadastro não pôde ser concluído e deve ser tentado novamente.

#### Scenario: Falha no envio do e-mail
- **WHEN** o visitante envia um cadastro válido e o serviço de e-mail está indisponível
- **THEN** o sistema não mantém o usuário gravado e informa que o cadastro deve ser tentado novamente
