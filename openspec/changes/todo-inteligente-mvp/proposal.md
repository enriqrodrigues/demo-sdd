# Proposal

## Why

O repositório precisa de um MVP pequeno, mas com regras de negócio reais no backend, para validar o ciclo completo de desenvolvimento orientado a especificação (spec, design, tarefas, testes e entrega). O escopo inicial (`escopo_inicial.md`) define um gerenciador de tarefas pessoais que vai além do CRUD: calcula a situação de prazo de cada tarefa e um score semanal de produtividade.

## What Changes

- Novo backend Spring Boot (Java) com API REST JSON para criar, listar, consultar, atualizar e excluir tarefas.
- Tarefa com os campos `id`, `titulo`, `descricao`, `dataVencimento`, `prioridade` (`BAIXA`, `MEDIA`, `ALTA`), `status` (`PENDENTE`, `CONCLUIDA`) e `dataConclusao`. O campo `dataConclusao` não está no esboço; ele foi acrescentado porque o score precisa dele. É só de leitura, preenchido pelo backend ao concluir e apagado ao reabrir.
- Campo calculado `situacaoPrazo` (`ATRASADA`, `PARA_HOJE`, `NO_PRAZO`, `CONCLUIDA`), derivado em tempo de execução de `status`, `dataVencimento` e da data atual. Nunca é persistido.
- Endpoint analítico do score de produtividade da semana corrente (segunda a domingo, fuso `America/Sao_Paulo`): tarefas concluídas no prazo ÷ tarefas concluídas na semana, com período, contagens e `percentual` `null` quando não há tarefas concluídas na semana.
- Erros padronizados no formato RFC 7807 (ProblemDetail), com lista de campos inválidos.
- Banco PostgreSQL em container Docker, com o schema versionado por migrações.
- Frontend React em uma tela única: lista ordenada por vencimento e prioridade, formulário de criação e edição, checkbox para concluir ou reabrir, selo de situação e card do score.

Fora do escopo: autenticação ou multiusuário, filtros, paginação e histórico de conclusões.

## Capabilities

### New Capabilities
- `task-management`: cadastro e ciclo de vida das tarefas (CRUD, validações, transições de status entre concluir e reabrir, controle de `dataConclusao`, ordenação da listagem e formato de erro da API).
- `deadline-status`: cálculo em tempo de execução da situação de prazo de cada tarefa a partir do status, do vencimento e da data atual.
- `productivity-score`: indicador semanal de tarefas concluídas no prazo sobre o total de tarefas concluídas na semana corrente.
- `task-web-ui`: tela web única que permite ao usuário gerenciar as tarefas e acompanhar a situação de prazo e o score.

### Modified Capabilities
<!-- Nenhuma: o projeto ainda não possui specs. -->

## Impact

- Código novo em três módulos: `backend/` (Spring Boot, Maven), `frontend/` (React) e `database/` (Docker Compose do PostgreSQL).
- API nova sob `/api/tarefas` e `/api/produtividade/score`.
- Dependências: Spring Boot (Web, Data JPA, Validation), driver PostgreSQL, Flyway, Testcontainers para testes de integração, React com Vite e Vitest no frontend.
- Execução local depende do Docker Desktop para o banco e para os testes de integração.
