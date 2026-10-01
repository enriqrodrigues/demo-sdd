# Sistema de Cadastro de Usuários — Design

- **Data:** 2026-09-30
- **Origem:** [docs/sistema_cadastro_esboco.md](../../sistema_cadastro_esboco.md)
- **Status:** aguardando revisão

## 1. Contexto e objetivo

Este projeto implementa o esboço de cadastro de usuários usando o framework **Superpowers**. O mesmo esboço será implementado em outros frameworks de desenvolvimento orientado a especificação (OpenSpec, BMAD) para comparar como cada um funciona.

**Critérios de sucesso**

- O sistema funciona de ponta a ponta localmente, incluindo o envio real do e-mail de ativação para o endereço cadastrado.
- Cada requisito funcional (RF) e regra de negócio (RN) do escopo é comprovado por testes automatizados.
- Há uma trilha clara de artefatos (spec, plano, commits) e o código é legível para revisão humana.

**Não objetivos:** implantação em produção, alta disponibilidade, observabilidade.

## 2. Escopo

**Dentro do escopo:** RF01–RF07 e RN01–RN04 do esboço.

**Fora do escopo** (reservados para uma rodada futura de melhorias):

- Recuperação de senha
- Reenvio do e-mail de ativação
- Painel administrativo
- Feedback visual avançado (além das mensagens de validação e de erro descritas aqui)
- Troca de senha pelo perfil, "lembrar de mim", limite de tentativas de login
- Consulta automática de CEP
- Testes E2E automatizados (por exemplo, Playwright)

**Limitação conhecida (aceita):** sem o reenvio de ativação, um usuário cujo link expirou não consegue ativar a conta nem se cadastrar de novo com o mesmo e-mail/CPF. A funcionalidade de reenvio, já prevista nos recursos sugeridos, resolverá isso.

## 3. Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web, Spring Data JPA, Spring Security, Spring Modulith 2, Bean Validation, Spring Mail |
| Banco | PostgreSQL 17 (Docker Compose), migrations com Flyway |
| Frontend | React 19, Vite, TypeScript, React Router, React Hook Form, Zod |
| Build | Maven multi-módulo; `frontend-maven-plugin` para o build do frontend |
| Testes backend | JUnit 5, AssertJ, MockMvc, Testcontainers (PostgreSQL), GreenMail |
| Testes frontend | Vitest, React Testing Library, MSW |
| E-mail | SMTP do Gmail com senha de app |

O ambiente de desenvolvimento usa Maven 3.9 com JDK 21, Node 24 e Docker com Compose.

## 4. Arquitetura

### 4.1 Módulos do repositório (monolito modular)

```
demo-sdd-superpower/
├── pom.xml          parent; agrega os módulos
├── database/        docker-compose.yml do Postgres + migrations Flyway (JAR de recursos)
├── frontend/        SPA React + Vite; o build vira um JAR com os arquivos em META-INF/resources
└── backend/         aplicação Spring Boot; API /api/** e entrega da SPA
```

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `database` | Infraestrutura do Postgres e esquema versionado (`db/migration/V*.sql`) | — |
| `frontend` | Interface web | — |
| `backend` | API REST, regras de negócio, segurança, entrega da SPA | `database`, `frontend` |

`mvn package` gera um único JAR executável em `backend/target/`. Esse JAR serve a SPA e a API na porta 8080 e aplica as migrations ao subir.

### 4.2 Módulos de domínio no backend

Pacote raiz: `br.com.demo.cadastro`. Cada subpacote direto é um módulo de aplicação do Spring Modulith. Só os tipos do pacote raiz de cada módulo são API pública; os subpacotes (`internal`, por exemplo) são privados.

| Módulo | Cobre | Expõe |
|---|---|---|
| `usuario` | Entidade `Usuario`, `Endereco` (embeddable), `StatusUsuario`, repositório, validador de CPF, regra de senha forte, unicidade, imutabilidade | `UsuarioService` (cadastrar, buscar, ativar, atualizar contato) e tipos de valor |
| `cadastro` | RF01–RF03: `POST /api/usuarios`; publica `UsuarioCadastrado` | evento `UsuarioCadastrado` |
| `ativacao` | RF04, RF05, RN02: consome `UsuarioCadastrado`, gera o token, envia o e-mail; `POST /api/ativacao` | — |
| `autenticacao` | RF06, RN04: configuração do Spring Security, login/logout por sessão | — |
| `perfil` | RF07: `GET/PUT /api/perfil` | — |
| `shared` | Tratamento global de erros (Problem Details), exceções base, configuração web (encaminhamento da SPA) | tipos de erro |

O teste de arquitetura `ApplicationModules.of(Application.class).verify()` garante essas fronteiras.

### 4.3 Fluxo de cadastro e ativação

1. `POST /api/usuarios` → `cadastro` valida a requisição e chama `UsuarioService.cadastrar`, que grava o usuário com status `PENDENTE_ATIVACAO` e publica `UsuarioCadastrado(usuarioId, nome, email)`.
2. Depois do commit, um `@ApplicationModuleListener` em `ativacao` roda em transação nova: gera o token, grava o hash e envia o e-mail.
3. Se o envio falhar, a transação do listener é desfeita, nenhum token é gravado e o evento fica incompleto no registro de publicações do Modulith (com JPA). Com `spring.modulith.events.republish-outstanding-events-on-restart=true`, ele é reprocessado no próximo restart.
4. O usuário abre `/ativar?token=...` → o frontend chama `POST /api/ativacao` → o status muda para `ATIVO` e o token é marcado como usado.

### 4.4 Execução

- **Desenvolvimento:** `docker compose -f database/docker-compose.yml up -d`; `mvn -pl backend spring-boot:run` (porta 8080); `npm run dev` em `frontend/` (porta 5173, com proxy de `/api` → 8080).
- **Integrado:** `mvn package` e depois `java -jar backend/target/backend-*.jar` (porta 8080).
- **Configuração** (variáveis de ambiente, lidas de `.env`; o `.env.example` fica versionado e o `.env` vai para o `.gitignore`):

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cadastro` | conexão com o banco |
| `DB_USERNAME` / `DB_PASSWORD` | `cadastro` / `cadastro` | credenciais do banco |
| `MAIL_USERNAME` | — (obrigatória) | conta Gmail e remetente |
| `MAIL_PASSWORD` | — (obrigatória) | senha de app do Gmail |
| `APP_BASE_URL` | `http://localhost:8080` | base do link de ativação |
| `APP_ATIVACAO_EXPIRACAO` | `PT24H` | validade do token (ISO-8601) |

## 5. Modelo de dados

### 5.1 `usuario`

| Coluna | Tipo | Nulo | Editável | Regra |
|---|---|---|---|---|
| `id` | uuid | não | — | PK |
| `nome` | varchar(150) | não | não | obrigatório, sem espaços nas pontas |
| `cpf` | char(11) | não | não | só dígitos; DV válido; rejeita sequências repetidas; `UNIQUE` |
| `email` | varchar(254) | não | não | formato válido; gravado em minúsculas e sem espaços; `UNIQUE` |
| `data_nascimento` | date | não | não | anterior à data atual |
| `senha_hash` | varchar(100) | não | não | BCrypt |
| `telefone` | varchar(11) | não | sim | só dígitos; DDD (2) + 8 ou 9 dígitos |
| `cep` | char(8) | não | sim | 8 dígitos |
| `logradouro` | varchar(200) | não | sim | obrigatório |
| `numero` | varchar(10) | não | sim | obrigatório; aceita "S/N" |
| `complemento` | varchar(100) | sim | sim | opcional |
| `bairro` | varchar(100) | não | sim | obrigatório |
| `cidade` | varchar(100) | não | sim | obrigatório |
| `uf` | char(2) | não | sim | uma das 27 UFs |
| `status` | varchar(20) | não | — | `PENDENTE_ATIVACAO` ou `ATIVO` |
| `criado_em` | timestamptz | não | — | definido na criação |
| `atualizado_em` | timestamptz | não | — | atualizado a cada alteração |

No JPA, os campos de endereço formam um `@Embeddable Endereco`. A entidade não tem setters para `nome`, `cpf`, `email`, `dataNascimento` nem `senhaHash`; as mudanças de estado passam só pelos métodos `ativar()` e `atualizarContato(telefone, endereco)`.

### 5.2 `token_ativacao`

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `id` | uuid | não | PK |
| `usuario_id` | uuid | não | FK → `usuario(id)` |
| `token_hash` | char(64) | não | SHA-256 (hex) do token; `UNIQUE` |
| `expira_em` | timestamptz | não | criação + `APP_ATIVACAO_EXPIRACAO` |
| `usado_em` | timestamptz | sim | preenchido na ativação |
| `criado_em` | timestamptz | não | definido na criação |

O token em claro tem 32 bytes do `SecureRandom` em Base64URL sem padding. Ele só aparece no link do e-mail e nunca é gravado.

### 5.3 Tabelas do Spring Modulith

O registro de publicação de eventos usa a tabela padrão do Spring Modulith, criada por uma migration Flyway explícita (sem geração automática de esquema).

## 6. Regras de validação (RF02)

As mesmas regras valem no frontend (schemas Zod) e no backend (Bean Validation e validadores próprios). O backend é a fonte da verdade.

| Campo | Regra | Mensagem |
|---|---|---|
| Todos, exceto complemento | obrigatório | "Campo obrigatório" |
| `email` | formato de e-mail | "E-mail inválido" |
| `cpf` | 11 dígitos, DV válido, sem sequência repetida | "CPF inválido" |
| `senha` | ≥ 8 caracteres, com maiúscula, minúscula, número e caractere especial | "A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial" |
| `confirmacaoSenha` (só no front) | igual à senha | "As senhas não conferem" |
| `dataNascimento` | data passada | "Data de nascimento inválida" |
| `telefone` | 10 ou 11 dígitos | "Telefone inválido" |
| `cep` | 8 dígitos | "CEP inválido" |
| `uf` | uma das 27 UFs | "UF inválida" |

Máscaras de entrada no frontend: CPF `000.000.000-00`, telefone `(00) 00000-0000`, CEP `00000-000`. O frontend envia só os dígitos.

## 7. API REST

Todas as rotas ficam sob `/api` e trafegam JSON. Datas seguem ISO-8601 (`yyyy-MM-dd`).

| Método | Rota | Acesso | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/api/usuarios` | público | `201` `{id, email, status}` | `400 VALIDACAO`; `409 EMAIL_JA_CADASTRADO` ou `CPF_JA_CADASTRADO` |
| POST | `/api/ativacao` `{token}` | público | `204` | `400 TOKEN_INVALIDO`, `TOKEN_EXPIRADO` ou `TOKEN_JA_UTILIZADO` |
| POST | `/api/auth/login` `{email, senha}` | público | `200` `{nome, email}` | `401 CREDENCIAIS_INVALIDAS`; `403 CONTA_PENDENTE` |
| POST | `/api/auth/logout` | autenticado | `204` | — |
| GET | `/api/perfil` | autenticado | `200` perfil completo, sem senha | `401 NAO_AUTENTICADO` |
| PUT | `/api/perfil` `{telefone, endereco}` | autenticado | `200` perfil atualizado | `400 VALIDACAO`; `401 NAO_AUTENTICADO` |

**Corpo do cadastro:** `{nome, cpf, email, dataNascimento, senha, telefone, endereco: {cep, logradouro, numero, complemento, bairro, cidade, uf}}`.

**Corpo do perfil (GET):** `{nome, cpf, email, dataNascimento, telefone, endereco: {...}, status}`.

**Formato de erro:** `application/problem+json` (RFC 9457) com as propriedades extras `codigo` e, quando há erro de validação, `erros: [{campo, mensagem}]`. O nome do campo usa notação de ponto para campos aninhados (`endereco.cep`). O `PUT /api/perfil` usa um DTO que contém só `telefone` e `endereco`, então campos imutáveis enviados no corpo são ignorados (RN01).

## 8. Segurança

- **Sessão no servidor** com Spring Security; cookie `JSESSIONID` `HttpOnly`, `SameSite=Lax`. Front e back ficam na mesma origem, sem CORS e sem JWT.
- **CSRF** ativo com `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN` legível pelo JS e header `X-XSRF-TOKEN`). Login, cadastro e ativação também exigem o token CSRF; o frontend o obtém antes da primeira requisição mutável.
- **Login (RN04):** primeiro a senha é conferida, depois o status. E-mail inexistente e senha errada retornam o mesmo `401 CREDENCIAIS_INVALIDAS`. `403 CONTA_PENDENTE` só aparece quando a senha está correta e o usuário está `PENDENTE_ATIVACAO`. Usuário pendente nunca recebe sessão.
- **Login bem-sucedido:** o ID da sessão é trocado (proteção contra fixação de sessão).
- **Senhas:** BCrypt; nunca são registradas em log nem devolvidas pela API.
- **Rotas:** `/api/usuarios`, `/api/ativacao` e `/api/auth/login` são públicas; o resto de `/api/**` exige autenticação. Recursos estáticos e rotas da SPA são públicos. Requisições não autenticadas à API recebem `401` em JSON, sem redirecionamento.

## 9. E-mail de ativação (RF04)

- Spring Mail em `smtp.gmail.com:587` com STARTTLS, autenticado com `MAIL_USERNAME`/`MAIL_PASSWORD`. O remetente é `MAIL_USERNAME`.
- Assunto: "Ative sua conta".
- Corpo multipart (HTML + texto puro) gerado por template em text block. O nome do usuário é escapado no HTML. Contém o link `{APP_BASE_URL}/ativar?token={token}` e informa a validade (24h).
- Testes usam GreenMail; nenhum teste automatizado acessa o Gmail.

## 10. Frontend

| Rota | Tela | Comportamento |
|---|---|---|
| `/cadastro` | Formulário de cadastro | Todos os campos da seção 6, com máscaras; validação ao sair do campo e, depois do primeiro erro, a cada digitação; indicador de força da senha; confirmação de senha; um `409` marca o campo de e-mail ou de CPF; erros `400` da API marcam os campos correspondentes |
| `/cadastro/concluido` | Confirmação | "Enviamos um link de ativação para {email}" |
| `/ativar?token=` | Ativação | Chama `POST /api/ativacao` ao abrir; mostra sucesso com link para o login ou uma mensagem específica por código de erro |
| `/login` | Login | `401` → "E-mail ou senha inválidos"; `403` → "Sua conta ainda não foi ativada. Verifique seu e-mail." |
| `/perfil` | Perfil (protegida) | Nome, CPF, e-mail e data de nascimento em somente leitura; telefone e endereço editáveis com "Salvar"/"Cancelar"; mensagem de sucesso ao salvar; botão "Sair" |
| `/` | — | Redireciona para `/perfil` se há sessão, senão para `/login` |

- A proteção de rota usa `GET /api/perfil`; um `401` redireciona para `/login`.
- No backend, toda rota que não é `/api/**` nem arquivo estático é encaminhada para `index.html`.
- Estilo com CSS próprio e enxuto, sem biblioteca de componentes.

## 11. Testes

TDD: cada comportamento começa por um teste que falha.

**Backend**

| Nível | Ferramentas | Cobertura |
|---|---|---|
| Unitário | JUnit 5, AssertJ | Validador de CPF; regra de senha; geração e hash do token; expiração e uso único; métodos de domínio de `Usuario` |
| Integração | `@SpringBootTest`, Testcontainers (PostgreSQL 17), MockMvc | Todos os endpoints da seção 7: sucesso, `400` com lista de campos, `409`, erros de token, `401`/`403` no login, `401` no perfil, PUT ignorando campos imutáveis, rejeição sem CSRF |
| E-mail | GreenMail | O cadastro gera um e-mail para o destinatário certo; o link extraído ativa a conta; falha de SMTP não desfaz o cadastro |
| Arquitetura | Spring Modulith | `ApplicationModules.verify()` |
| Migrations | Testcontainers | O Flyway aplica todas as migrations em um banco vazio |

**Frontend:** Vitest + React Testing Library + MSW. Cobre os schemas Zod e o comportamento de cada tela da seção 10, incluindo o mapeamento de erros da API.

**Execução:** `mvn verify` roda os testes do backend e do frontend (pelo plugin). Exige o Docker ativo.

**Verificação manual (ponta a ponta com Gmail real)**

1. Configurar o `.env` com a conta Gmail e a senha de app; subir o banco; rodar o JAR.
2. Cadastrar um usuário com um e-mail real → aparece a tela de confirmação.
3. Tentar logar antes de ativar → aparece a mensagem de conta pendente.
4. Abrir o e-mail recebido e clicar no link → aparece a mensagem de sucesso.
5. Abrir o mesmo link de novo → aparece a mensagem de token já utilizado.
6. Logar → a tela de perfil aparece com os dados corretos.
7. Editar telefone e endereço, salvar e recarregar a página → as alterações persistiram.
8. Tentar cadastrar de novo com o mesmo e-mail e depois com o mesmo CPF → erro no campo correspondente.
9. Sair → acessar `/perfil` redireciona para o login.

## 12. Rastreabilidade

| Requisito | Onde é implementado | Como é comprovado |
|---|---|---|
| RF01 Cadastro | `cadastro`, tela `/cadastro` | Integração `POST /api/usuarios`; teste da tela de cadastro |
| RF02 Validação | Bean Validation, validadores de CPF e senha, schemas Zod | Unitários dos validadores; integração `400`; testes dos schemas |
| RF03 Persistência pendente | `usuario`, migration V1 | Integração: status `PENDENTE_ATIVACAO` após o cadastro |
| RF04 E-mail de confirmação | `ativacao` (listener + e-mail) | GreenMail |
| RF05 Ativação | `ativacao`, tela `/ativar` | Integração `POST /api/ativacao`; teste da tela |
| RF06 Login | `autenticacao`, tela `/login` | Integração do login; teste da tela |
| RF07 Edição de perfil | `perfil`, tela `/perfil` | Integração `GET/PUT /api/perfil`; teste da tela |
| RN01 Imutabilidade | DTO de atualização restrito; entidade sem setters | Integração: PUT com campos imutáveis não os altera |
| RN02 Expiração do link | `ativacao` | Unitário e integração com token expirado (relógio controlado por `Clock`) |
| RN03 Unicidade | Checagem no serviço + `UNIQUE` no banco | Integração `409` para e-mail e para CPF |
| RN04 Controle de acesso | `autenticacao` | Integração: login pendente → `403`, sem sessão criada |
