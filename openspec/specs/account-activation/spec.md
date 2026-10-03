# account-activation Specification

## Purpose

Confirma que o usuário controla o e-mail informado no cadastro, por meio de um link de ativação exclusivo, temporário e de uso único que muda a conta de `PENDENTE` para `ATIVO`.

## Requirements

### Requirement: Envio do e-mail de ativação
Ao concluir um cadastro, o sistema SHALL enviar um e-mail ao endereço cadastrado com um link de ativação que leva à página de ativação do sistema e informa o prazo de validade do link.

#### Scenario: E-mail enviado após cadastro
- **WHEN** um cadastro válido é concluído para `maria@exemplo.com`
- **THEN** um e-mail é entregue a `maria@exemplo.com` contendo um link para a página de ativação com um token exclusivo e a informação de que o link expira em 24 horas

### Requirement: Link exclusivo e imprevisível
Cada link de ativação SHALL conter um token aleatório, imprevisível e associado a um único usuário. O sistema SHALL NOT armazenar o token em texto puro.

#### Scenario: Tokens distintos
- **WHEN** dois usuários concluem o cadastro
- **THEN** cada um recebe um link com um token diferente, e o token de um não ativa a conta do outro

### Requirement: Validade de 24 horas
O link de ativação SHALL expirar 24 horas após sua emissão.

#### Scenario: Link usado dentro do prazo
- **WHEN** o usuário usa o link 23 horas após o cadastro
- **THEN** a conta é ativada

#### Scenario: Link expirado
- **WHEN** o usuário usa o link mais de 24 horas após o cadastro
- **THEN** o sistema recusa a ativação, a conta permanece `PENDENTE` e a página informa que o link expirou e que é possível fazer um novo cadastro com os mesmos dados

### Requirement: Ativação da conta
Ao receber um token válido, não expirado e não utilizado, o sistema SHALL mudar o status do usuário de `PENDENTE` para `ATIVO` e invalidar o token.

#### Scenario: Ativação bem-sucedida
- **WHEN** o usuário confirma a ativação na página aberta por um link válido
- **THEN** o status do usuário passa a `ATIVO` e a página informa que a conta foi ativada

### Requirement: Uso único do link
Um link de ativação já utilizado SHALL ser recusado em qualquer uso posterior, sem alterar a conta.

#### Scenario: Link reutilizado
- **WHEN** o usuário usa novamente um link com o qual já ativou a conta
- **THEN** o sistema recusa o uso e a página informa que o link já foi utilizado

### Requirement: Link inválido
Um token que não corresponda a nenhum link emitido SHALL ser recusado sem alterar nenhuma conta.

#### Scenario: Token desconhecido
- **WHEN** a página de ativação é aberta com um token inexistente ou adulterado
- **THEN** o sistema recusa a ativação e a página informa que o link é inválido

### Requirement: Ativação exige ação do usuário
Abrir o link de ativação SHALL NOT alterar o estado da conta por si só. A ativação SHALL ocorrer somente quando o usuário confirmar a ação na página de ativação.

#### Scenario: Link apenas aberto
- **WHEN** o link de ativação é acessado (por exemplo, por um verificador automático de e-mail) sem que o usuário confirme a ativação
- **THEN** a conta permanece `PENDENTE` e o token continua válido
