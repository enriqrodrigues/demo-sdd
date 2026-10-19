# To-Do Inteligente

Gerenciador de tarefas pessoais com situação de prazo calculada (atrasada, para hoje, no prazo) e score semanal de produtividade. A especificação está em `openspec/`.

| Módulo      | Tecnologia                                         |
|-------------|----------------------------------------------------|
| `database/` | PostgreSQL 16 via Docker Compose                   |
| `backend/`  | Spring Boot 3.5, Java 21, Maven Wrapper, Flyway    |
| `frontend/` | React 18 + TypeScript, Vite, Vitest, MSW           |

## Pré-requisitos

- **JDK 21** com `JAVA_HOME` apontando para ele (o Maven Wrapper usa o `JAVA_HOME`).
- **Node 24** e npm.
- **Docker Desktop em execução** — necessário para o banco e para os testes de integração do backend (Testcontainers).

## Variáveis de ambiente

As credenciais nunca ficam no código nem em arquivos versionados.

| Onde              | Variável           | Exemplo                                      |
|-------------------|--------------------|----------------------------------------------|
| `database/.env`   | `POSTGRES_DB`      | `todo`                                       |
| `database/.env`   | `POSTGRES_USER`    | `todo`                                       |
| `database/.env`   | `POSTGRES_PASSWORD`| uma senha própria                            |
| backend           | `DB_URL`           | `jdbc:postgresql://127.0.0.1:5432/todo`      |
| backend           | `DB_USUARIO`       | o mesmo valor de `POSTGRES_USER`             |
| backend           | `DB_SENHA`         | o mesmo valor de `POSTGRES_PASSWORD`         |

Propriedades opcionais do backend (passadas como argumento, ex.: `--app.fuso-horario=UTC`):

| Propriedade                  | Padrão                  | Uso                                              |
|------------------------------|-------------------------|--------------------------------------------------|
| `app.fuso-horario`           | `America/Sao_Paulo`     | Fuso em que se define "hoje" e a semana do score |
| `app.cors.origens-permitidas`| `http://localhost:5173` | Origens liberadas no CORS (separadas por vírgula)|

## Subindo o sistema

Os exemplos usam PowerShell e partem da raiz do repositório; em bash, troque `$env:VAR="valor"` por `export VAR=valor` e `.\mvnw.cmd` por `./mvnw`. Backend e frontend ficam em execução, então use um terminal para cada um.

### 1. Banco de dados

```powershell
Copy-Item database\.env.example database\.env   # depois edite a senha
docker compose -f database/docker-compose.yml up -d --wait
```

O PostgreSQL fica publicado apenas em `127.0.0.1:5432`.

### 2. Backend (http://127.0.0.1:8080)

```powershell
cd backend
$env:DB_URL="jdbc:postgresql://127.0.0.1:5432/todo"
$env:DB_USUARIO="todo"
$env:DB_SENHA="<a senha do database/.env>"
.\mvnw.cmd spring-boot:run
```

O Flyway cria o schema na primeira inicialização.

### 3. Frontend (http://localhost:5173)

```powershell
cd frontend
npm ci
npm run dev
```

Em desenvolvimento, o Vite encaminha `/api` para o backend.

## Testes e verificações

A partir da raiz do repositório:

```powershell
# backend: unitários + integração (Testcontainers; precisa do Docker Desktop).
# Para rodar só os unitários, sem Docker, troque "verify" por "test".
Push-Location backend; .\mvnw.cmd verify; Pop-Location

# frontend
Push-Location frontend; npm ci; npm test; npm run lint; npm run build; npm audit; Pop-Location
```

Os testes de integração sobem o próprio PostgreSQL em container; não dependem do banco do `database/` nem das variáveis `DB_*`.

## Troubleshooting

### Erros de certificado (PKIX / TLS) no Maven

Antivírus que inspecionam TLS (ex.: Avast) substituem o certificado dos repositórios, e o Java não confia nele: o Maven falha com `PKIX path building failed`. Faça o Java usar o repositório de certificados do Windows, sem alterar arquivos do projeto:

```powershell
$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd verify
```

Com o mesmo antivírus, o npm pode falhar com `UNABLE_TO_GET_ISSUER_CERT_LOCALLY`; use `$env:NODE_OPTIONS="--use-system-ca"`.

### Testes de integração falham ao iniciar

`Could not find a valid Docker environment` indica que o Docker Desktop não está em execução. Inicie-o e rode os testes de novo; os testes unitários (`.\mvnw.cmd test`) não dependem dele.
