# Proposal

## Why

Precisamos de um MVP de gerenciador de tarefas pessoais que vá além de um CRUD: com regras de negócio calculadas no backend (situação de prazo e score semanal de produtividade), ele serve para validar o ciclo completo de desenvolvimento orientado a especificação — validações, testes, revisão e entrega. O repositório hoje contém apenas o escopo inicial; nada foi implementado.

## What Changes

- Novo backend Spring Boot (Java) com API REST JSON para criar, listar, consultar, editar, concluir, reabrir e excluir tarefas, com validação estrita de entrada e erros padronizados em Problem Details (RFC 9457).
- Persistência em PostgreSQL rodando em container Docker, publicado apenas em `127.0.0.1`, com credenciais lidas de variáveis de ambiente e schema versionado por migrações.
- Registro do instante de conclusão (`concluidaEm`), gravado pelo servidor na transição para `CONCLUIDA` e limpo ao reabrir — campo não previsto no escopo inicial, necessário para calcular "concluída no prazo".
- Situação de prazo (`CONCLUIDA`, `ATRASADA`, `PARA_HOJE`, `NO_PRAZO`) calculada em tempo de execução, nunca persistida; `NO_PRAZO` cobre o caso de pendente com vencimento futuro, que o escopo inicial não definia.
- Endpoint analítico do score de produtividade da semana ISO corrente (segunda a domingo), com contadores e percentual (`null` quando nenhuma tarefa foi finalizada na semana).
- Datas avaliadas em um único fuso configurável (padrão `America/Sao_Paulo`).
- Controle de concorrência otimista na edição (`version`, resposta 409 em conflito).
- Novo frontend React de página única: lista com filtros e badge de prazo, formulário de criação/edição, score no cabeçalho e tratamento explícito de conflitos entre ações sobre a mesma tarefa.

**Fora de escopo:** autenticação e múltiplos usuários (aplicação de usuário único, servida em localhost), paginação, busca, ordenação configurável, dark mode, PWA/offline e internacionalização (UI apenas em pt-BR).

## Capabilities

### New Capabilities

- `gestao-tarefas`: ciclo de vida da tarefa — criação, listagem, consulta, edição, exclusão, transições concluir/reabrir, validação de campos, concorrência otimista e formato de erro da API.
- `prazo-tarefa`: cálculo da situação de prazo de cada tarefa a partir de status, vencimento e data corrente no fuso configurado.
- `score-produtividade`: indicador semanal de tarefas concluídas no prazo versus total concluído na semana corrente.
- `interface-web`: aplicação React que consome a API, incluindo o comportamento da UI diante de ações concorrentes ou repetidas sobre a mesma tarefa.

### Modified Capabilities

(nenhuma — não existem specs no projeto)

## Impact

- **Código novo:** módulo backend (Spring Boot, Flyway, testes com Testcontainers), módulo frontend (React + Vite + Vitest) e `docker-compose.yml` para o PostgreSQL.
- **APIs novas:** `/api/tarefas` (CRUD + `/concluir` e `/reabrir`) e `/api/produtividade/semana`.
- **Dependências/infra:** Docker é necessário para subir o banco e para os testes de integração; variáveis de ambiente para credenciais (arquivo `.env` local, fora do controle de versão).
