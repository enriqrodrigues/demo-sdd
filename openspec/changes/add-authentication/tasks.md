# Tasks

> Cada grupo termina com um commit próprio, e o `./mvnw verify` precisa passar ao fim de cada grupo. A mensagem segue o padrão `tipo(escopo): descrição` e cita os requisitos.

## 1. Segurança base e CSRF (sem mudar o comportamento existente)

- [x] 1.1 Trocar `spring-security-crypto` por `spring-boot-starter-security` e adicionar `spring-security-test` no `backend/pom.xml`. Verificar com `./mvnw -pl backend -am test-compile`.
- [x] 1.2 Criar `SecurityConfig` em `shared.security` com as regras de acesso do design D5, `csrf.spa()`, as estratégias de sessão (D3) e o logout padrão em `/api/auth/logout` (204). Criar o entry point 401 `UNAUTHENTICATED` e o handler 403 `CSRF_INVALID`, ambos em `ProblemDetail`. Verificar que o `ApplicationModules.verify()` continua passando.
- [x] 1.3 Configurar a sessão no `application.yml` (timeout de 30 min, cookie `http-only`, `same-site=lax`, `secure` via `SESSION_COOKIE_SECURE`, rastreamento só por cookie). Verificar com um teste de configuração de que o timeout é de 30 minutos.
- [x] 1.4 Criar `GET /api/auth/csrf` (204, emite o cookie `XSRF-TOKEN`). Verificar com um teste de integração de que a resposta traz o cookie `XSRF-TOKEN`.
- [x] 1.5 Ajustar os testes existentes: `postJson` com `.with(csrf())`, e `@Import(SecurityConfig)` + `csrf()` no `RegistrationControllerValidationTest`. Adicionar o teste do cenário "Requisição sem token anti-CSRF" (cadastro recusado com 403 `CSRF_INVALID` e nenhum usuário gravado). Verificar que todos os testes do backend passam.
- [x] 1.6 No frontend, fazer o `api.ts` usar um `request` genérico que, nos métodos que alteram estado, garante o cookie `XSRF-TOKEN` (chamando `/api/auth/csrf` se faltar) e envia `X-XSRF-TOKEN`. Verificar com testes Vitest: o cabeçalho é enviado; o cookie é buscado só quando ausente; as respostas de cadastro e ativação continuam tratadas como antes.
- [x] 1.7 Verificar a aplicação rodando (`java -jar`): um cadastro feito pela interface chega ao servidor com o token (sem credenciais SMTP, a resposta esperada é 503, e não 403), e um `curl` sem o token recebe 403.

## 2. Login, sessão e logout (backend)

- [ ] 2.1 Criar o módulo `auth` com `AuthenticatedUser`, `LoginRequest` (normaliza o e-mail; campos obrigatórios) e o `LoginService`, seguindo a ordem do design D2: hash fictício para e-mail inexistente, 401 `INVALID_CREDENTIALS`, 403 `ACCOUNT_PENDING`, estratégia de sessão e gravação do `SecurityContext`. Verificar com testes de integração: login de conta ativa (200 com nome e e-mail); e-mail com caixa diferente; senha errada; e-mail inexistente (mesma mensagem); campos vazios (400).
- [ ] 2.2 Cobrir a RN04 no login. Verificar com testes de integração: conta pendente com senha correta recebe 403 `ACCOUNT_PENDING`, sem sessão autenticada; conta pendente com senha errada recebe 401; login após ativação via `/api/activations` funciona.
- [ ] 2.3 Criar `GET /api/auth/me` com os dados do banco pelo id do principal. Verificar com testes de integração: 200 com sessão; 401 `UNAUTHENTICATED` sem sessão; 401 com a sessão invalidada (cenário "Sessão expirada").
- [ ] 2.4 Verificar a segurança da sessão com testes de integração: o cookie de sessão é `HttpOnly`; o id de sessão muda no login; depois do logout (`POST /api/auth/logout` → 204), `/me` responde 401 com a mesma sessão; cadastro, ativação e login continuam acessíveis sem sessão.
- [ ] 2.5 Verificar as fronteiras: o `ApplicationModules.verify()` passa com o módulo `auth`, e `shared` não depende de `auth`.

## 3. Login e área interna (frontend)

- [ ] 3.1 Adicionar `login`, `logout` e `me` ao `api.ts`. Verificar com testes Vitest cobrindo 200, 401 `INVALID_CREDENTIALS`, 403 `ACCOUNT_PENDING` e 401 `UNAUTHENTICATED`.
- [ ] 3.2 Implementar a página `/login`: e-mail e senha obrigatórios, mensagem genérica de credenciais inválidas, orientação de conta pendente, link para o cadastro, estado de carregamento, redirecionamento para `/inicio` quando já autenticado, e o aviso "Você saiu da sua conta." depois do logout. Verificar com testes React Testing Library, um para cada comportamento.
- [ ] 3.3 Implementar `RequireAuth` e a `HomePage` em `/inicio`, com saudação pelo nome e botão Sair (logout leva a `/login`). Redirecionar `/` para `/inicio`. Verificar com testes React Testing Library: sem sessão vai para `/login`; com sessão mostra o nome; sair chama o logout e vai para `/login`.
- [ ] 3.4 Adicionar os links "Já tem conta? Entrar" no cadastro e "Entrar" na ativação bem-sucedida, e ajustar o teste da rota raiz. Verificar com testes React Testing Library.

## 4. Documentação e verificação integrada

- [ ] 4.1 Atualizar o README: login e área interna, novos endpoints, como obter o token CSRF para chamadas manuais (`curl` com cookie e cabeçalho) e a nota sobre a sessão em memória. Verificar executando os comandos `curl` documentados contra a aplicação rodando.
- [ ] 4.2 Rodar o `./mvnw verify` completo. Verificar que termina com BUILD SUCCESS e sem testes ignorados.
- [ ] 4.3 Fazer o teste manual ponta a ponta no navegador:
  - criar uma conta ativa direto no banco ou pelo fluxo com Gmail;
  - fazer login e ver a saudação;
  - recarregar a página e continuar logado;
  - sair e confirmar que `/inicio` volta para o login;
  - tentar login com uma conta pendente e ver a orientação de ativação.

  Registrar o resultado no PR.
