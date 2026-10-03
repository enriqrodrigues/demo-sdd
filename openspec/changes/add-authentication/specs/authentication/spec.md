# Spec Delta

## Purpose

Permite que usuários com conta ativa entrem na plataforma com e-mail e senha e mantenham uma sessão, garantindo que contas pendentes de ativação e visitantes não autenticados não acessem os recursos internos.

## ADDED Requirements

### Requirement: Página de login
O sistema SHALL disponibilizar uma página web pública de login com os campos e-mail e senha e com um link para a página de cadastro. Um usuário que já está autenticado e abre a página de login SHALL ser levado à área interna.

#### Scenario: Visitante acessa o login
- **WHEN** um visitante não autenticado abre a página de login
- **THEN** o sistema exibe o formulário com e-mail e senha e um link para criar uma conta

#### Scenario: Usuário já autenticado
- **WHEN** um usuário autenticado abre a página de login
- **THEN** o sistema o leva à área interna

### Requirement: Login com credenciais válidas
O sistema SHALL autenticar o usuário cujo e-mail e senha correspondem a uma conta `ATIVO`, iniciando uma sessão e levando-o à área interna. O e-mail SHALL ser comparado sem distinção de maiúsculas e minúsculas e sem os espaços nas pontas.

#### Scenario: Login bem-sucedido
- **WHEN** o usuário de uma conta `ATIVO` informa seu e-mail e a senha correta
- **THEN** o sistema inicia uma sessão e exibe a área interna com o nome do usuário

#### Scenario: E-mail com caixa diferente
- **WHEN** o usuário da conta `ATIVO` de `maria@exemplo.com` informa ` Maria@Exemplo.com ` e a senha correta
- **THEN** o sistema autentica o usuário

### Requirement: Recusa de credenciais inválidas
O sistema SHALL recusar o login quando o e-mail não pertence a nenhuma conta ou a senha está incorreta, sem iniciar sessão, e SHALL exibir nos dois casos a mesma mensagem: "E-mail ou senha inválidos".

#### Scenario: Senha incorreta
- **WHEN** o usuário de uma conta `ATIVO` informa a senha errada
- **THEN** o sistema recusa o login com a mensagem "E-mail ou senha inválidos" e nenhuma sessão é iniciada

#### Scenario: E-mail não cadastrado
- **WHEN** alguém informa um e-mail que não pertence a nenhuma conta
- **THEN** o sistema recusa o login com a mesma mensagem "E-mail ou senha inválidos"

#### Scenario: Campos vazios
- **WHEN** o usuário envia o login com o e-mail ou a senha vazios
- **THEN** o sistema indica que os campos são obrigatórios e não tenta autenticar

### Requirement: Conta pendente não acessa a plataforma
O sistema SHALL recusar o login de uma conta `PENDENTE` informando que a conta aguarda ativação pelo e-mail, sem iniciar sessão (RN04). Essa informação SHALL ser exibida somente quando a senha informada estiver correta; com a senha errada, vale a mensagem genérica de credenciais inválidas.

#### Scenario: Conta pendente com senha correta
- **WHEN** o usuário de uma conta `PENDENTE` informa seu e-mail e a senha correta
- **THEN** o sistema recusa o login, informa que a conta está pendente de ativação e que o link foi enviado por e-mail, e nenhuma sessão é iniciada

#### Scenario: Conta pendente com senha errada
- **WHEN** alguém informa o e-mail de uma conta `PENDENTE` com a senha errada
- **THEN** o sistema recusa o login com a mensagem "E-mail ou senha inválidos"

#### Scenario: Login após ativação
- **WHEN** o usuário ativa a conta pelo link de ativação e em seguida faz login com a senha correta
- **THEN** o sistema autentica o usuário

### Requirement: Recursos internos exigem autenticação
Os recursos internos da plataforma SHALL estar disponíveis somente para usuários autenticados (RN04). Na API, uma requisição a um recurso interno sem sessão válida SHALL ser recusada como não autenticada; na interface, a área interna SHALL levar o visitante sem sessão à página de login.

#### Scenario: API sem sessão
- **WHEN** um cliente sem sessão consulta os dados do usuário autenticado na API
- **THEN** o sistema recusa a requisição como não autenticada

#### Scenario: Área interna sem sessão
- **WHEN** um visitante sem sessão abre a área interna
- **THEN** o sistema o leva à página de login

#### Scenario: Recursos públicos continuam acessíveis
- **WHEN** um visitante sem sessão usa o cadastro, a ativação de conta ou o login
- **THEN** o sistema atende a requisição normalmente

### Requirement: Área interna
A área interna SHALL exibir o nome do usuário autenticado e oferecer a opção de sair.

#### Scenario: Usuário vê a área interna
- **WHEN** um usuário autenticado abre a área interna
- **THEN** o sistema exibe uma saudação com o nome do usuário e o botão de sair

### Requirement: Logout
O sistema SHALL permitir que o usuário autenticado encerre a sessão. Após o logout, os recursos internos SHALL deixar de estar acessíveis com aquela sessão.

#### Scenario: Usuário sai
- **WHEN** o usuário autenticado clica em sair
- **THEN** a sessão é encerrada, o usuário é levado à página de login e uma nova tentativa de acessar a área interna o leva de volta ao login

### Requirement: Expiração da sessão por inatividade
A sessão SHALL expirar após 30 minutos sem requisições do usuário. Uma requisição com a sessão expirada SHALL ser tratada como não autenticada.

#### Scenario: Sessão expirada
- **WHEN** o usuário autenticado fica mais de 30 minutos sem interagir e então acessa a área interna
- **THEN** o sistema o trata como não autenticado e o leva à página de login

### Requirement: Proteção do identificador de sessão
O identificador da sessão SHALL ser armazenado em cookie inacessível a scripts da página, e um novo identificador SHALL ser emitido a cada login bem-sucedido.

#### Scenario: Cookie de sessão
- **WHEN** o login é bem-sucedido
- **THEN** o cookie de sessão é marcado como inacessível a scripts (`HttpOnly`)

#### Scenario: Renovação no login
- **WHEN** um cliente que já tinha um identificador de sessão faz login com sucesso
- **THEN** o sistema emite um identificador de sessão diferente do anterior

### Requirement: Proteção contra requisições forjadas
Toda requisição à API que altera estado (cadastro, ativação, login e logout) SHALL exigir um token anti-CSRF emitido pelo sistema. Requisições sem o token ou com token inválido SHALL ser recusadas sem efeito.

#### Scenario: Requisição sem token anti-CSRF
- **WHEN** um cliente envia uma requisição de cadastro válida sem o token anti-CSRF
- **THEN** o sistema recusa a requisição e nenhum usuário é gravado

#### Scenario: Requisição com token anti-CSRF
- **WHEN** a interface do sistema envia o cadastro com o token anti-CSRF que recebeu
- **THEN** o sistema processa o cadastro normalmente
