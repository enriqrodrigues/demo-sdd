# To-Do Inteligente

Gerenciador de tarefas pessoais com situação de prazo calculada pelo backend e score semanal de produtividade. O escopo está em [`escopo_inicial.md`](escopo_inicial.md) e as especificações em [`openspec/`](openspec/).

| Módulo | Tecnologia | Porta |
| --- | --- | --- |
| [`database/`](database/) | PostgreSQL 17 em Docker Compose | 5432 |
| [`backend/`](backend/) | Java 21, Spring Boot 3.5, Flyway | 8080 |
| [`frontend/`](frontend/) | React, TypeScript, Vite | 5173 |

## Pré-requisitos

- **JDK 21** (com `JAVA_HOME` apontando para ele). O Maven vem pelo wrapper (`mvnw`), então não precisa instalar.
- **Docker Desktop** em execução. Ele roda o banco e os testes de integração (Testcontainers).
- **Node.js 24** com npm.

Os comandos abaixo partem da raiz do repositório. No PowerShell, troque `./mvnw` por `.\mvnw.cmd`.

## Como rodar

### 1. Banco de dados

```bash
docker compose -f database/docker-compose.yml up -d
```

Isso sobe o container `demo-sdd-postgres` com o banco `todo` (usuário `todo`, senha `todo`) e os dados num volume nomeado. Para conferir se está saudável, rode `docker compose -f database/docker-compose.yml ps`. O backend cria as tabelas pelo Flyway ao iniciar.

Para parar o banco, rode `docker compose -f database/docker-compose.yml down`. Acrescente `-v` para também apagar os dados.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

A API fica em `http://localhost:8080/api`. A data de "hoje" usada nos cálculos vem do fuso `America/Sao_Paulo` (propriedade `app.fuso` em `application.yml`), seja qual for o fuso da máquina.

### 3. Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. Em desenvolvimento, o Vite encaminha `/api` para `http://localhost:8080`, então o backend precisa estar no ar.

## Testes

```bash
cd backend && ./mvnw verify    # unitários, @WebMvcTest e integração com PostgreSQL (precisa do Docker)
cd frontend && npm test        # Vitest + React Testing Library
```

Os testes de integração sobem um PostgreSQL próprio via Testcontainers e não usam o banco do Docker Compose. Para rodar só os testes que não precisam de Docker:

```bash
cd backend && ./mvnw test -Dtest='*Test,!*IntegrationTest,!TarefaRepositoryTest,!TodoApplicationTests'
```

## API

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/api/tarefas` | Cria uma tarefa (`201` com `Location`) |
| `GET` | `/api/tarefas` | Lista as tarefas por vencimento e prioridade (`ALTA`, `MEDIA`, `BAIXA`) |
| `GET` | `/api/tarefas/{id}` | Consulta uma tarefa |
| `PUT` | `/api/tarefas/{id}` | Substitui os dados e o `status` (conclui ou reabre) |
| `DELETE` | `/api/tarefas/{id}` | Exclui uma tarefa (`204`) |
| `GET` | `/api/produtividade/score` | Score da semana corrente (segunda a domingo) |

Exemplo de criação:

```bash
curl -i -X POST http://localhost:8080/api/tarefas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Pagar conta","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA"}'
```

Cada tarefa retornada traz `situacaoPrazo` (`ATRASADA`, `PARA_HOJE`, `NO_PRAZO` ou `CONCLUIDA`), calculada na hora da resposta e nunca gravada. Já `dataConclusao` é preenchida pelo backend ao concluir e apagada ao reabrir. Os erros seguem a RFC 7807 (`application/problem+json`), e os de validação trazem `errors: [{"campo", "mensagem"}]`.

## Troubleshooting

### Docker Desktop parado

Sintomas: `docker compose` falha com `error during connect` ou `open //./pipe/dockerDesktopLinuxEngine`, os testes de integração falham com `Could not find a valid Docker environment`, ou o backend não sobe por `Connection to localhost:5432 refused`.

Solução: abra o Docker Desktop, espere o status "Engine running" e rode de novo. Pela linha de comando, no Windows:

```powershell
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
docker info   # repita até responder sem erro
```

### Certificados TLS interceptados por antivírus

Antivírus com inspeção de HTTPS (por exemplo, o Web Shield do Avast) trocam o certificado dos sites pelo deles. As ferramentas que não usam o repositório de certificados do sistema passam a recusar a conexão.

- **Maven** (`PKIX path building failed` ao baixar dependências): use o repositório de certificados do Windows.

  ```powershell
  $env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
  .\mvnw.cmd verify
  ```

- **npm** (`UNABLE_TO_GET_ISSUER_CERT_LOCALLY` ou `SELF_SIGNED_CERT_IN_CHAIN`): faça o Node usar os certificados do sistema.

  ```powershell
  $env:NODE_OPTIONS = "--use-system-ca"
  npm install
  ```

- **curl no Windows** (`CRYPT_E_NO_REVOCATION_CHECK`): acrescente `--ssl-no-revoke`.

Outra opção é desativar a inspeção de HTTPS no antivírus. Não grave essas configurações no repositório, porque elas só fazem sentido na máquina afetada.

### Porta 5432 ocupada

Se já existe outro PostgreSQL na máquina, o container não consegue publicar a porta. Pare o outro serviço ou troque o mapeamento em `database/docker-compose.yml` (por exemplo, `"5433:5432"`) e a URL em `backend/src/main/resources/application.yml`.
