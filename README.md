# demo-sdd: Sistema de Cadastro de Usuários

Implementação do esboço [docs/sistema_cadastro_esboco.md](docs/sistema_cadastro_esboco.md) usando o fluxo **OpenSpec** de desenvolvimento orientado por especificação.

O que já funciona:

- Change `add-user-onboarding`:
  - Cadastro pelo formulário web (RF01), com validação ao vivo no formulário e validação definitiva no servidor (RF02).
  - Gravação do usuário como `PENDENTE`, com a senha em hash BCrypt (RF03).
  - E-mail e CPF únicos (RN03). Um cadastro pendente cujo link expirou pode ser refeito com os mesmos dados.
  - Envio real do e-mail de ativação pelo Gmail (RF04).
  - Ativação por link de uso único, válido por 24 horas (RF05, RN02).
- Change `add-authentication`:
  - Login com e-mail e senha de uma conta ativa (RF06), com a mesma mensagem para e-mail inexistente e senha errada.
  - Conta pendente não entra: com a senha correta, recebe a orientação de ativar a conta pelo e-mail (RN04).
  - Área interna (`/inicio`) com o nome do usuário e o botão Sair. Sem sessão, a área interna leva ao login, e a API responde 401.
  - Sessão no servidor, com cookie `HttpOnly` renovado a cada login, expiração após 30 minutos sem uso e proteção CSRF em todas as requisições que alteram estado.
- Change `add-user-profile`:
  - Página de perfil (`/perfil`), aberta pelo link "Meu perfil" da área interna, com todos os dados do usuário da sessão (RF07).
  - Edição de telefone e endereço com as mesmas máscaras e regras de validação do cadastro, revalidadas no servidor.
  - Nome, CPF, e-mail e data de nascimento aparecem só para leitura e a API recusa qualquer tentativa de alterá-los (RN01).
  - O perfil é sempre o do usuário da sessão: não há como consultar ou alterar o de outra pessoa.

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
| `auth` | login, sessão e dados do usuário autenticado (RF06, RN04) |
| `profile` | consulta e edição do perfil do usuário da sessão (RF07, RN01) |
| `shared` | validadores, normalização, formato de erro da API, hash de senha e configuração de segurança (sessão, CSRF, rotas públicas) |

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
| `SESSION_COOKIE_SECURE` | Opcional. `true` envia o cookie de sessão só por HTTPS. Padrão: `false`, porque a execução local é em http. |

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
| `/` | Leva à área interna, que leva ao login quando não há sessão |
| `/login` | Login com e-mail e senha. Quem já tem sessão vai direto para `/inicio`. |
| `/inicio` | Área interna: saudação pelo nome, link "Meu perfil" e botão Sair |
| `/perfil` | Perfil: dados pessoais somente leitura e formulário de telefone e endereço. Sem sessão, leva ao login. |
| `/cadastro` | Formulário de cadastro |
| `/cadastro/sucesso` | Aviso de que o e-mail de ativação foi enviado |
| `/ativar?token=...` | Página aberta pelo link do e-mail. A ativação só ocorre quando o usuário clica em "Ativar minha conta". |

| Endpoint | Sucesso | Erros (`code`) |
|---|---|---|
| `POST /api/registrations` | `201 { "email" }` | `400 VALIDATION_ERROR`, `409 ALREADY_REGISTERED`, `409 PENDING_ACTIVATION`, `503 EMAIL_UNAVAILABLE` |
| `POST /api/activations` `{ "token" }` | `200 { "email" }` | `400 INVALID_TOKEN`, `410 TOKEN_EXPIRED`, `409 TOKEN_ALREADY_USED` |
| `GET /api/auth/csrf` | `204` + cookie `XSRF-TOKEN` | — |
| `POST /api/auth/login` `{ "email", "password" }` | `200 { "name", "email" }` + cookie de sessão | `400 VALIDATION_ERROR`, `401 INVALID_CREDENTIALS`, `403 ACCOUNT_PENDING` |
| `POST /api/auth/logout` | `204` | — |
| `GET /api/auth/me` | `200 { "name", "email" }` | `401 UNAUTHENTICATED` |
| `GET /api/profile` | `200` perfil (abaixo) | `401 UNAUTHENTICATED` |
| `PUT /api/profile` `{ "phone", "cep", "street", "number", "complement", "district", "city", "state" }` | `200` perfil atualizado | `400 VALIDATION_ERROR` (inclui campos não alteráveis), `401 UNAUTHENTICATED` |
| qualquer `POST` ou `PUT` sem o token anti-CSRF | — | `403 CSRF_INVALID` |

Os erros seguem o formato `ProblemDetail` (RFC 9457), com as extensões `code` e `errors[]` (`{ field, message }`).

### Sessão e token anti-CSRF

- A sessão fica **na memória** da aplicação: reiniciar a aplicação encerra todas as sessões, e o usuário precisa entrar de novo.
- Toda requisição que altera estado (cadastro, ativação, login, logout e alteração do perfil) exige o token anti-CSRF. O servidor o emite no cookie `XSRF-TOKEN`, e o cliente o devolve no cabeçalho `X-XSRF-TOKEN`. A interface faz isso sozinha.

Para chamar a API manualmente com `curl`, guarde os cookies num arquivo e envie o token no cabeçalho:

```bash
# 1. Obter o token anti-CSRF (grava o cookie XSRF-TOKEN em cookies.txt)
curl -s -c cookies.txt http://localhost:8080/api/auth/csrf
TOKEN=$(awk '$6 == "XSRF-TOKEN" { print $7 }' cookies.txt)

# 2. Login (grava o cookie de sessão JSESSIONID e o novo token)
curl -s -b cookies.txt -c cookies.txt -H "X-XSRF-TOKEN: $TOKEN"   -H "Content-Type: application/json"   -d '{"email": "maria@exemplo.com", "password": "Segura@123"}'   http://localhost:8080/api/auth/login

# 3. Usuário da sessão
curl -s -b cookies.txt http://localhost:8080/api/auth/me

# 4. Logout (o token muda a cada login, então é lido de novo)
TOKEN=$(awk '$6 == "XSRF-TOKEN" { print $7 }' cookies.txt)
curl -s -b cookies.txt -c cookies.txt -H "X-XSRF-TOKEN: $TOKEN" -X POST -o /dev/null -w "%{http_code}
"   http://localhost:8080/api/auth/logout
```

Sem o token, a resposta é `403 CSRF_INVALID`.

### Perfil

O perfil é sempre o do usuário da sessão; não existe id de usuário na rota nem no corpo. A resposta traz `{ name, cpf, email, birthDate, phone, cep, street, number, complement, district, city, state }`, com CPF, telefone e CEP só com dígitos.

O `PUT` recebe o conjunto completo dos campos editáveis, com ou sem máscara. Todos são obrigatórios, exceto `complement`, que pode ser enviado vazio para removê-lo. As regras são as do cadastro: telefone com 10 ou 11 dígitos incluindo o DDD, CEP com 8 dígitos, UF válida e os mesmos tamanhos máximos.

**Campos imutáveis (RN01):** nome, CPF, e-mail e data de nascimento não podem ser alterados. Se o `PUT` trouxer qualquer um deles preenchido, a resposta é `400 VALIDATION_ERROR` com um item em `errors[]` por campo ("Este campo não pode ser alterado"), e nada é gravado. Enviá-los como `null` não conta como tentativa de alteração.

Com a sessão do login acima (cookies em `cookies.txt`):

```bash
# 1. Consultar o perfil
curl -s -b cookies.txt http://localhost:8080/api/profile

# 2. Alterar telefone e endereço (o token muda a cada login, então é lido de novo)
TOKEN=$(awk '$6 == "XSRF-TOKEN" { print $7 }' cookies.txt)
curl -s -b cookies.txt -c cookies.txt -X PUT -H "X-XSRF-TOKEN: $TOKEN"   -H "Content-Type: application/json"   -d '{"phone": "(21) 3456-7890", "cep": "20040-020", "street": "Rua da Assembleia", "number": "10",
       "complement": "", "district": "Centro", "city": "Rio de Janeiro", "state": "RJ"}'   http://localhost:8080/api/profile

# 3. Tentar trocar o e-mail: 400, com errors[] apontando "email"
curl -s -b cookies.txt -c cookies.txt -X PUT -H "X-XSRF-TOKEN: $TOKEN"   -H "Content-Type: application/json"   -d '{"phone": "(21) 3456-7890", "cep": "20040-020", "street": "Rua da Assembleia", "number": "10",
       "complement": "", "district": "Centro", "city": "Rio de Janeiro", "state": "RJ",
       "email": "outro@exemplo.com"}'   http://localhost:8080/api/profile
```

## Testes

`./mvnw verify` roda:

- **Backend:** testes unitários dos validadores e do token, testes de integração com Postgres real (Testcontainers) e SMTP falso (GreenMail) cobrindo cada cenário das specs, e o teste de modularidade.
- **Frontend:** testes das regras de validação, das máscaras, do cliente da API (incluindo o envio do token anti-CSRF) e das páginas de cadastro, ativação, login, área interna e perfil.

O Docker precisa estar rodando.

## Trilha OpenSpec

- Specs principais em [openspec/specs/](openspec/specs/): `user-registration` e `account-activation` (change `add-user-onboarding`), `authentication` (change `add-authentication`) e `user-profile` (change `add-user-profile`).
- As três changes estão arquivadas em [openspec/changes/archive/](openspec/changes/archive/), cada uma com `proposal.md`, `design.md`, `tasks.md` e a spec delta. Não há change em andamento.
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
