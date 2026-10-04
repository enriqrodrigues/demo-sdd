# Proposal

## Why

O cadastro e a ativação já funcionam (specs `user-registration` e `account-activation`), mas ainda não há como entrar na plataforma. O usuário ativa a conta e não tem o que fazer com ela. Esta change entrega o RF06 (login com e-mail e senha) e a RN04 (contas pendentes não acessam recursos internos), que são pré-requisito da edição de perfil (RF07) na change `add-user-profile`.

O texto do RF06 no esboço é circular ("o usuário autenticado faz login"). Ele é interpretado aqui como: **um usuário com conta ativa se autentica com e-mail e senha e obtém uma sessão.**

## What Changes

- **RF06, login:** página `/login` com e-mail e senha. Credenciais válidas de uma conta `ATIVO` iniciam uma sessão e levam à área interna.
- **Credenciais inválidas:** e-mail inexistente ou senha errada recebem sempre a mesma mensagem genérica, para não revelar quais e-mails estão cadastrados.
- **RN04, conta pendente:** com a senha **correta** e a conta `PENDENTE`, o login é recusado com a orientação de ativar a conta pelo e-mail, e nenhuma sessão é criada. Com a senha errada, a mensagem genérica de sempre.
- **Área interna mínima** (`/inicio`): mostra o nome do usuário autenticado e o botão de sair. É o primeiro "recurso interno" protegido, e a change de perfil vai ampliá-la.
- **Proteção de recursos internos:**
  - na API, rotas internas sem sessão respondem 401;
  - no frontend, a página interna sem sessão redireciona para `/login`.
- **Logout:** encerra a sessão.
- **Expiração da sessão:** após 30 minutos de inatividade.
- **Segurança da sessão:**
  - cookie de sessão `HttpOnly`, renovado a cada login (contra fixação de sessão);
  - proteção CSRF em **todas** as requisições que alteram estado, incluindo as de cadastro e ativação já existentes. O comportamento delas não muda; o frontend passa a enviar o token anti-CSRF.
- **Navegação:**
  - login e cadastro passam a ter links um para o outro;
  - a página de ativação bem-sucedida oferece o link para entrar;
  - a rota `/` passa a levar à área interna, que redireciona para o login quando não há sessão.

### Fora do escopo

- Ficam para a change `add-user-profile`: perfil, edição de endereço e telefone, e RN01.
- Ficam para a fase de melhorias:
  - "lembrar-me";
  - bloqueio após tentativas de login malsucedidas;
  - recuperação e troca de senha;
  - reenvio do e-mail de ativação.

## Capabilities

### New Capabilities
- `authentication`: login com e-mail e senha, recusa de credenciais inválidas e de contas pendentes (RN04), sessão do usuário (criação, expiração, logout), proteção de recursos internos e proteção CSRF das requisições que alteram estado.

### Modified Capabilities
<!-- Nenhuma: cadastro e ativação mantêm os mesmos requisitos. O envio do token
     anti-CSRF e os links de navegação são detalhes de implementação, sem mudança
     no comportamento especificado. -->

## Impact

- **Backend:**
  - novo módulo `auth`;
  - dependência `spring-boot-starter-security`, que substitui o uso isolado de `spring-security-crypto`;
  - configuração de segurança com rotas públicas (SPA, cadastro, ativação, login) e rotas internas autenticadas.
- **APIs novas:**
  - `POST /api/auth/login`;
  - `POST /api/auth/logout`;
  - `GET /api/auth/me`.
- **APIs existentes:** `POST /api/registrations` e `POST /api/activations` passam a exigir o token anti-CSRF. Os testes existentes precisam enviá-lo.
- **Frontend:**
  - páginas `/login` e `/inicio`;
  - o cliente da API passa a enviar o token anti-CSRF e a tratar 401;
  - links novos nas páginas de cadastro e de ativação.
- **Testes:** `spring-security-test` no backend.
