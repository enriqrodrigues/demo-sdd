# Sistema de Cadastro de Usuários

Monorepo da demo: `database/` (PostgreSQL 18 em Docker), `backend/` (Spring Boot 4.1, Java 21) e `frontend/` (React 19 com Vite).

## Pré-requisitos

- Docker Desktop em execução
- JDK 21 (com `JAVA_HOME` apontando para ele; o Maven vem pelo wrapper `mvnw`)
- Node.js 24
- Portas livres: 5432 (Postgres), 8080 (backend) e 5173 (frontend)

## Configuração

Copie o arquivo de exemplo e preencha os valores. O `.env` fica na raiz, é ignorado pelo Git e nunca deve ser versionado.

```powershell
cp .env.example .env
```

| Variável | Uso |
| --- | --- |
| `DB_URL` | URL JDBC do Postgres (valor do exemplo: `jdbc:postgresql://localhost:5432/usuarios`) |
| `DB_USERNAME`, `DB_PASSWORD` | usuário e senha do banco, usados pelo compose e pelo backend |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | conta do Gmail e senha de app usadas para enviar os e-mails |
| `APP_FRONTEND_URL` | endereço do frontend usado nos links enviados por e-mail (valor do exemplo: `http://localhost:5173`) |

## Como executar (três passos)

1. Banco de dados, em `database/`:

   ```powershell
   docker compose --env-file ../.env up -d
   ```

2. Backend, em `backend/` (responde em `http://localhost:8080`):

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. Frontend, em `frontend/` (abre em `http://localhost:5173`; as chamadas a `/api` são repassadas ao backend):

   ```powershell
   npm install
   npm run dev
   ```

Para parar o banco e apagar os dados (volume nomeado), em `database/`:

```powershell
docker compose down -v
```

## Testes

- Backend, em `backend/` (precisa do Docker em execução, pois usa Testcontainers):

  ```powershell
  .\mvnw.cmd test
  ```

- Frontend, em `frontend/`:

  ```powershell
  npm test
  npm run build
  ```

## Problemas comuns (Avast)

O Avast (Web Shield / Mail Shield) intercepta conexões TLS e o Java e o Node passam a recusar os certificados (erros como `PKIX path building failed` ou `unable to get local issuer certificate`). Os contornos abaixo valem só para a sua máquina e não vão para o código nem para o `pom.xml`.

- **Maven** (download de dependências pelo `mvnw`):

  ```powershell
  $env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
  ```

- **Aplicação** (envio de e-mail pelo Gmail; o `spring-boot:run` abre uma JVM separada, que não herda o `MAVEN_OPTS`):

  ```powershell
  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStoreType=Windows-ROOT"
  ```

- **npm** (`npm install`):

  ```powershell
  $env:NODE_OPTIONS = "--use-system-ca"
  ```
