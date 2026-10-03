# demo-sdd: Sistema de Cadastro de Usuários

Implementação do esboço [docs/sistema_cadastro_esboco.md](docs/sistema_cadastro_esboco.md) usando o fluxo **OpenSpec** de desenvolvimento orientado por especificação.

O que já funciona (change `add-user-onboarding`):

- Cadastro pelo formulário web (RF01), com validação ao vivo no formulário e validação definitiva no servidor (RF02).
- Gravação do usuário como `PENDENTE`, com a senha em hash BCrypt (RF03).
- E-mail e CPF únicos (RN03). Um cadastro pendente cujo link expirou pode ser refeito com os mesmos dados.
- Envio real do e-mail de ativação pelo Gmail (RF04).
- Ativação por link de uso único, válido por 24 horas (RF05, RN02).

Login (RF06, RN04) e perfil (RF07, RN01) virão nas próximas changes: `add-authentication` e `add-user-profile`.

## Arquitetura

Monolito modular em Maven multi-módulo. Os três módulos são gerados num único jar executável, que serve a API REST e a SPA na mesma origem.

```
demo-sdd/
  pom.xml          parent (Java 21, Spring Boot 3.5)
  database/        migrações Flyway (db/migration) + docker-compose do Postgres 16
  frontend/        React 19 + Vite + TypeScript (empacotado em META-INF/resources)
  backend/         Spring Boot: API REST em /api/** e a SPA nas demais rotas
  openspec/        specs, changes e artefatos de planejamento (proposal, design, tasks)
```

O backend se divide em módulos de domínio. O Spring Modulith verifica as fronteiras entre eles num teste:

| Módulo | Responsabilidade |
|---|---|
| `registration` | cadastro, validação e unicidade (RN03) |
| `activation` | token, e-mail e ativação da conta |
| `user` | entidade `User` e repositório |
| `shared` | validadores, normalização, formato de erro da API e hash de senha |

## Pré-requisitos

- **JDK 21** ou mais recente.
- **Docker** (Docker Desktop no Windows), usado pelo Postgres e pelos testes de integração (Testcontainers).
- **Não precisa** de Maven nem de Node instalados: o Maven Wrapper (`mvnw`) e o `frontend-maven-plugin` baixam o que for necessário.
- Uma conta **Gmail com senha de app**, para enviar os e-mails de ativação.

## Configuração

### 1. Arquivo `.env`

```bash
cp .env.example .env
```

Preencha o `.env`. Esse arquivo fica no `.gitignore`.

| Variável | Descrição |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Banco local. Os padrões já funcionam. |
| `MAIL_USERNAME` | Seu endereço Gmail. |
| `MAIL_PASSWORD` | A **senha de app** do Gmail (veja abaixo), **não** a senha da conta. |
| `MAIL_FROM` | Opcional. Remetente exibido; o padrão é `MAIL_USERNAME`. |
| `APP_BASE_URL` | URL usada no link de ativação. Padrão: `http://localhost:8080`. |

### 2. Senha de app do Gmail

1. Ative a **verificação em duas etapas** na sua conta Google.
2. Acesse <https://myaccount.google.com/apppasswords> e crie uma senha de app (por exemplo, com o nome "demo-sdd").
3. Copie os 16 caracteres, sem espaços, para `MAIL_PASSWORD` no `.env`.

Sem essas credenciais a aplicação sobe normalmente, mas todo cadastro responde **503**, com "Não foi possível enviar o e-mail de ativação", e não é gravado. Esse comportamento é intencional: o cadastro só é concluído com o e-mail enviado.

## Executando

Os comandos abaixo partem da raiz do projeto. No Windows (PowerShell), use `.\mvnw.cmd` no lugar de `./mvnw`.

```bash
# 1. Banco de dados
docker compose -f database/docker-compose.yml up -d

# 2. Build completo com testes (front + back + modularidade)
./mvnw verify

# 3. Aplicação (rode a partir da raiz, para que o .env seja lido)
java -jar backend/target/backend.jar
```

Acesse <http://localhost:8080>. O Flyway aplica as migrações ao subir a aplicação.

Para conferir o status de um usuário no banco:

```bash
docker exec demo-sdd-postgres psql -U demosdd -d demosdd -c "select email, status from users"
```

### Desenvolvimento do frontend

Com o backend rodando na porta 8080:

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, com proxy de /api para o backend
npm test           # testes (Vitest + React Testing Library)
```

## Rotas e API

| Rota (SPA) | Descrição |
|---|---|
| `/cadastro` | Formulário de cadastro |
| `/cadastro/sucesso` | Aviso de que o e-mail de ativação foi enviado |
| `/ativar?token=...` | Página aberta pelo link do e-mail. A ativação só ocorre quando o usuário clica em "Ativar minha conta". |

| Endpoint | Sucesso | Erros (`code`) |
|---|---|---|
| `POST /api/registrations` | `201 { "email" }` | `400 VALIDATION_ERROR`, `409 ALREADY_REGISTERED`, `409 PENDING_ACTIVATION`, `503 EMAIL_UNAVAILABLE` |
| `POST /api/activations` `{ "token" }` | `200 { "email" }` | `400 INVALID_TOKEN`, `410 TOKEN_EXPIRED`, `409 TOKEN_ALREADY_USED` |

Os erros seguem o formato `ProblemDetail` (RFC 9457), com as extensões `code` e `errors[]` (`{ field, message }`).

## Testes

`./mvnw verify` roda:

- **Backend:** testes unitários dos validadores e do token, testes de integração com Postgres real (Testcontainers) e SMTP falso (GreenMail) cobrindo cada cenário das specs, e o teste de modularidade.
- **Frontend:** testes das regras de validação, das máscaras, do cliente da API e das páginas de cadastro e de ativação.

O Docker precisa estar rodando.

## Trilha OpenSpec

- Change em andamento: [openspec/changes/add-user-onboarding/](openspec/changes/add-user-onboarding/). Ela contém `proposal.md`, `design.md`, `tasks.md` e as specs `user-registration` e `account-activation`.
- Os commits seguem os grupos do `tasks.md`.

## Solução de problemas

### Erro `PKIX path building failed` (Maven) ou falha TLS no envio de e-mail

Antivírus com inspeção de TLS, como o Avast (Web/Mail Shield), substituem os certificados por um certificado raiz próprio. Esse certificado está no repositório do Windows, mas não no da JDK. Para a JDK usar o repositório de certificados do Windows:

```powershell
# Maven
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd verify

# Aplicação (necessário para o SMTP do Gmail se o antivírus inspeciona e-mail)
java "-Djavax.net.ssl.trustStoreType=Windows-ROOT" -jar backend/target/backend.jar
```

Outra opção é desativar a inspeção de TLS/e-mail do antivírus.

### Porta 5432 ou 8080 em uso

Pare o serviço que ocupa a porta, ou ajuste `ports` no `database/docker-compose.yml` (com `DB_PORT` no `.env`) e `server.port` da aplicação.
