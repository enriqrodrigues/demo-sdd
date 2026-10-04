# Design

## Context

A estrutura entregue por `add-user-onboarding` tem:

- **Backend:** Spring Boot 3.5 com os módulos `registration`, `activation`, `user` e `shared`, verificados pelo Spring Modulith. Hoje só usa `spring-security-crypto`, para o BCrypt; não há filtro de segurança, e toda a API é pública.
- **Frontend:** React 19 + React Router 8, servido pelo próprio backend na mesma origem. O cliente `api.ts` faz `POST` em JSON e trata `ProblemDetail`.
- **Testes de integração:** `IntegrationTest` (MockMvc + Testcontainers + GreenMail), com o helper `postJson`.
- **Dados:** a tabela `users` já tem `status` (`PENDENTE`/`ATIVO`) e `password_hash` (BCrypt). Não é preciso mudar o banco.

A motivação está em proposal.md e os requisitos em `specs/authentication`.

## Goals / Non-Goals

**Goals:**
- Sessão no servidor, com cookie `HttpOnly` na mesma origem, sem guardar token no `localStorage`.
- Ordem de verificação no login que nunca revela, a quem não sabe a senha, se um e-mail existe ou está pendente.
- CSRF ativo para toda a API, sem quebrar o cadastro e a ativação existentes.
- Fronteiras de módulo preservadas: a configuração de segurança não depende dos módulos de domínio.

**Non-Goals:**
- Bloqueio por tentativas, captcha e rate limiting.
- Sessão persistente (Spring Session/JDBC): a sessão fica em memória e se perde quando a aplicação reinicia.
- Papéis e autorização por perfil: existe só "autenticado ou não". O painel administrativo fica para depois.
- HTTPS e cookie `Secure`: a execução é apenas local, em http. Fica configurável para quando houver HTTPS.

## Decisions

### D1. Spring Security com sessão HTTP e endpoint de login próprio

Entra o `spring-boot-starter-security`, que traz o `spring-security-crypto` já usado. A autenticação fica guardada na `HttpSession` através do `HttpSessionSecurityContextRepository`.

O login é um `POST /api/auth/login` com JSON, tratado por um controller próprio (`AuthController` + `LoginService`). O serviço verifica as credenciais e grava o `SecurityContext` explicitamente, seguindo o padrão do Spring Security 6 para endpoints de autenticação próprios.

**Alternativa descartada: `formLogin`.** Ele é orientado a redirecionamento e a `application/x-www-form-urlencoded`, o que pediria handlers de sucesso e de falha para virar API JSON. Além disso, o `DaoAuthenticationProvider` padrão verifica se a conta está habilitada **antes** de conferir a senha, o que revelaria que um e-mail está pendente a quem não sabe a senha. Isso viola a spec "Conta pendente não acessa a plataforma".

**Alternativa descartada: JWT.** Exigiria guardar o token no `localStorage` (exposto a XSS) ou montar a mesma infraestrutura de cookie. Também não permite encerrar a sessão no servidor sem uma lista de revogação. Para um monolito na mesma origem, a sessão é mais simples e mais segura.

### D2. Ordem de verificação no login

```
normaliza e-mail (trim + minúsculas)
  |
  +-- usuário não existe --> BCrypt.matches(senha, HASH_FICTICIO)  --> 401 INVALID_CREDENTIALS
  |                          (iguala o tempo de resposta)
  +-- senha não confere  -------------------------------------------> 401 INVALID_CREDENTIALS
  |
  +-- status PENDENTE    -------------------------------------------> 403 ACCOUNT_PENDING
  |
  +-- status ATIVO --> SessionAuthenticationStrategy (novo id de sessão + novo token CSRF)
                   --> salva SecurityContext na sessão --> 200 { name, email }
```

O principal da sessão é um `AuthenticatedUser(id, name, email)` imutável, com autoridade única `ROLE_USER`.

### D3. Fixação de sessão e rotação do token CSRF

Como o login não passa pelos filtros padrão de autenticação, o `LoginService` aplica uma `CompositeSessionAuthenticationStrategy` com:

- `ChangeSessionIdAuthenticationStrategy`: gera um novo id de sessão, para a spec "Renovação no login";
- `CsrfAuthenticationStrategy`: gera um novo token CSRF, para que o token anterior ao login não sirva depois dele.

As duas estratégias são definidas como beans na configuração de segurança.

### D4. CSRF para SPA (cookie + cabeçalho)

- **Configuração:** `http.csrf(csrf -> csrf.spa())`. O token vai num cookie `XSRF-TOKEN` legível por JavaScript, e o frontend o devolve no cabeçalho `X-XSRF-TOKEN`. Esse padrão é seguro contra CSRF porque um site de terceiros não consegue ler o cookie da nossa origem.
- **Emissão do cookie:** o endpoint `GET /api/auth/csrf` (204) carrega o token, o que faz o cookie ser emitido. O `api.ts` chama esse endpoint antes do primeiro `POST`, quando o cookie ainda não existe, e depois reaproveita o cookie. Assim não dependemos de um `GET` anterior acontecer por acaso.
- **Verificação na implementação:** se o `csrf.spa()` desta versão já emitir o cookie em toda resposta, o endpoint continua existindo só como garantia explícita.
- **Recusa:** sem token ou com token inválido, a resposta é 403 `CSRF_INVALID` (em `ProblemDetail`).
- **Alternativa descartada:** isentar cadastro e ativação de CSRF. Seria mais simples, mas deixaria inconsistente justamente as requisições públicas que criam estado.

### D5. Regras de acesso

| Rota | Acesso |
|---|---|
| `POST /api/registrations`, `POST /api/activations` | público |
| `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/csrf` | público |
| demais `/api/**` (ex.: `GET /api/auth/me`) | autenticado |
| tudo fora de `/api/**` (SPA, assets) | público; a proteção da área interna na interface é feita pelo frontend com `/api/auth/me` |

- **Sem sessão:** um `AuthenticationEntryPoint` próprio responde 401 `UNAUTHENTICATED` em `ProblemDetail`, em vez de redirecionar para uma página de login HTML.
- **Logout:** usa o `LogoutFilter` padrão em `POST /api/auth/logout`. Ele invalida a sessão, limpa o contexto, apaga o cookie `JSESSIONID` e responde 204.

### D6. Sessão

- **Expiração:** `server.servlet.session.timeout=30m`, para a spec "Expiração da sessão por inatividade".
- **Cookie:** `server.servlet.session.cookie.http-only=true` e `same-site=lax`.
- **`secure`:** fica em `${SESSION_COOKIE_SECURE:false}`, porque a execução local é em http.
- **Rastreamento:** só por cookie (`tracking-modes=cookie`), para o id de sessão nunca aparecer na URL.

### D7. Contrato REST

| Endpoint | Sucesso | Erros (`code`) |
|---|---|---|
| `POST /api/auth/login` `{ email, password }` | 200 `{ name, email }` + cookie de sessão | 400 `VALIDATION_ERROR` (campos vazios); 401 `INVALID_CREDENTIALS`; 403 `ACCOUNT_PENDING` |
| `POST /api/auth/logout` | 204 | — |
| `GET /api/auth/me` | 200 `{ name, email }` | 401 `UNAUTHENTICATED` |
| `GET /api/auth/csrf` | 204 + cookie `XSRF-TOKEN` | — |
| qualquer POST sem token | — | 403 `CSRF_INVALID` |

- **`/me` busca no banco:** os dados vêm do banco pelo id do principal, para refletir alterações futuras do perfil sem precisar de novo login.
- **Mensagens:**
  - `INVALID_CREDENTIALS`: "E-mail ou senha inválidos";
  - `ACCOUNT_PENDING`: "Sua conta ainda não foi ativada. Use o link enviado para o seu e-mail para ativá-la."

### D8. Módulos do backend

- **Módulo novo `auth`:** `AuthController`, `LoginService`, `AuthenticatedUser` e `LoginRequest`. Depende de `user` e `shared`.
- **`shared.security`:**
  - a `SecurityConfig` (filter chain, CSRF, logout e estratégias de sessão);
  - o entry point e o handler de acesso negado, que escrevem `ProblemDetail`.

  Essas classes não conhecem os módulos de domínio, só os padrões de URL. O `PasswordEncoderConfig` existente continua no mesmo lugar.
- **Verificação:** o `ApplicationModules.verify()` continua garantindo que não há ciclos.

### D9. Frontend

- **`api.ts`:**
  - `request(method, url, body?)` genérico, com `credentials: 'same-origin'`;
  - nos métodos que alteram estado, garante o cookie `XSRF-TOKEN` (chama `/api/auth/csrf` se ele não existir) e envia `X-XSRF-TOKEN`;
  - novas funções: `login`, `logout`, `me`.
- **Rotas:**
  - `/login`: `LoginPage`. Se `me()` responde 200, redireciona para `/inicio`.
  - `/inicio`: `HomePage`, envolvida por `RequireAuth`. Esse componente chama `me()`; se receber 401, vai para `/login`. Enquanto carrega, mostra "Carregando...".
  - `/`: redireciona para `/inicio`.
- **Links:**
  - cadastro: "Já tem conta? Entrar";
  - login: "Ainda não tem conta? Cadastre-se";
  - ativação bem-sucedida: "Entrar".
- **Após o logout:** navega para `/login` com o aviso "Você saiu da sua conta."
- **Por que sem contexto global (`AuthContext`):** com uma única página interna, o `RequireAuth` busca o usuário e o repassa para a página. O contexto pode entrar quando a change de perfil adicionar mais páginas internas.

### D10. Estratégia de testes

| Nível | Cobre |
|---|---|
| Integração (MockMvc + Testcontainers) | cada cenário da spec: login de conta ativa, e-mail com caixa diferente, senha errada, e-mail inexistente, campos vazios, conta pendente com senha certa e errada, login após ativação, `/me` com e sem sessão, logout, sessão invalidada tratada como não autenticada, cookie `HttpOnly`, id de sessão renovado, recusa sem token CSRF |
| Configuração | `server.servlet.session.timeout` igual a 30 minutos |
| Ajuste dos testes existentes | `postJson` passa a usar `.with(csrf())` (`spring-security-test`); o `@WebMvcTest` do cadastro importa a `SecurityConfig` |
| Componente (front) | `LoginPage` (validação, 401, 403, sucesso, redirecionamento se já autenticado), `RequireAuth`/`HomePage` (saudação, redirecionamento sem sessão, logout) e `api.ts` (envio do cabeçalho CSRF, busca do cookie quando ausente) |

## Risks / Trade-offs

- **Expiração real de 30 min não é exercitada em teste automatizado.** A sessão é gerenciada pelo container (Tomcat) com o relógio do sistema, e o MockMvc não simula a passagem do tempo. → Um teste garante a configuração de 30 min, e outro garante que uma sessão invalidada é tratada como não autenticada.
- **Ligar o CSRF quebra clientes que não enviam o token,** como chamadas manuais com curl. → É o comportamento desejado. O README passa a explicar como obter o token para testar a API manualmente.
- **Sessões em memória se perdem ao reiniciar a aplicação.** → Aceitável para uso local; o usuário só precisa entrar de novo.
- **Sem limite de tentativas de login, a força bruta fica possível.** → Fora do escopo do esboço, registrado como melhoria futura. O BCrypt já torna cada tentativa cara.
- **A diferença entre 401 e 403 revela que a conta está pendente,** mas só para quem sabe a senha. → Decisão aceita na exploração, porque é exatamente o que a RN04 pede ao usuário legítimo.

## Migration Plan

Não há mudança de banco. Depois do deploy local, as sessões abertas antes não existem, porque não havia login. O README ganha a seção de login e a nota sobre o token CSRF para chamadas manuais à API.
