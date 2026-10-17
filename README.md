# To-Do Inteligente (MVP)

Gerenciador de tarefas pessoais com situação de prazo calculada no backend e score semanal de produtividade.
Design: [`docs/superpowers/specs/2026-10-16-todo-inteligente-design.md`](docs/superpowers/specs/2026-10-16-todo-inteligente-design.md).

## Estrutura

| pasta | conteúdo |
|---|---|
| `backend/` | API REST em Spring Boot 3 (Java 21), com migrations Flyway |
| `frontend/` | Página única em React 18 + Vite + TypeScript |
| `database/` | `docker-compose.yml` com PostgreSQL 16 |

## Pré-requisitos

JDK 21 ou mais recente, Node 20 ou mais recente, e Docker Desktop.

## Como rodar

```bash
# 1. Banco
docker compose -f database/docker-compose.yml up -d

# 2. Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Variáveis opcionais do backend: `DB_URL` (padrão `jdbc:postgresql://localhost:5432/todo`), `DB_USER` (padrão `todo`) e `DB_PASSWORD` (padrão `todo`).

## Testes

```bash
cd backend && ./mvnw verify   # unitários + integração (Testcontainers; requer Docker)
cd frontend && npm test       # Vitest + Testing Library
```

## API

| método | rota | descrição |
|---|---|---|
| `GET` | `/api/tarefas` | lista: pendentes por vencimento, depois as concluídas |
| `GET` | `/api/tarefas/{id}` | busca uma tarefa |
| `POST` | `/api/tarefas` | cria (o `status` é opcional, padrão `PENDENTE`) |
| `PUT` | `/api/tarefas/{id}` | substitui todos os campos; mudar o `status` conclui ou reabre |
| `DELETE` | `/api/tarefas/{id}` | exclui |
| `GET` | `/api/produtividade/score-semanal` | percentual concluído no prazo na semana corrente (de segunda a domingo) |

Toda tarefa devolvida tem um campo `situacao` calculado: `ATRASADA`, `PARA_HOJE`, `NO_PRAZO` ou `CONCLUIDA`.
Os erros seguem o formato `application/problem+json` (RFC 9457). Nos erros de validação, a resposta traz a lista `erros: [{campo, mensagem}]`.

## Solução de problemas

- **Os testes de integração falham com erro de Docker.** O Docker Desktop precisa estar rodando.
- **O Maven falha com `PKIX path building failed`.** Um antivírus está interceptando o TLS. Rode `export MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (no PowerShell: `$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"`) e tente de novo.
- **O `npm install` falha com erro de certificado.** Rode `export NODE_OPTIONS=--use-system-ca`.
- **A porta 5432 está ocupada.** Pare o outro PostgreSQL ou container que estiver usando a porta.
