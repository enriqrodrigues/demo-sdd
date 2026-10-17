# To-Do Inteligente (MVP) — Design

- **Data:** 2026-10-16
- **Origem:** [`escopo_inicial.md`](../../../escopo_inicial.md)
- **Branch:** `release/demo-superpowers`

## 1. Objetivo e contexto

Gerenciador de tarefas pessoais, de usuário único, com regras de negócio calculadas no backend: situação de prazo (RF02) e score semanal de produtividade (RF03). O projeto serve para validar um fluxo de desenvolvimento orientado a spec (SDD) com o framework Superpowers.

**Sucesso:**
- o sistema roda localmente;
- há testes automatizados cobrindo as regras;
- fica um rastro claro de artefatos: esta spec, o plano de implementação e os commits.

**Decisões tomadas no brainstorming**, que complementam o escopo inicial:

| # | Decisão |
|---|---------|
| D1 | Tarefa `PENDENTE` com vencimento futuro tem a situação **No Prazo** (`NO_PRAZO`). |
| D2 | A semana corrente vai de segunda a domingo (padrão ISO), no fuso `America/Sao_Paulo`. |
| D3 | Uma tarefa conta como concluída no prazo quando `dataConclusao <= dataVencimento`. |
| D4 | O score considera as tarefas `CONCLUIDA` cuja `dataConclusao` cai na semana corrente. Se nada foi concluído, o percentual é `null`. |
| D5 | O status muda pelo `PUT` da tarefa. O servidor grava `dataConclusao` ao concluir e a limpa ao reabrir. O cliente nunca envia esse campo. |
| D6 | Reabrir uma tarefa (`CONCLUIDA` para `PENDENTE`) é permitido. |
| D7 | Vencimento no passado é permitido na criação e na edição. |
| D8 | Usuário único, sem autenticação. |
| D9 | Página única em React. A listagem tem ordem fixa definida pelo backend, sem filtros e sem paginação. |

## 2. Arquitetura

Monólito modular com uma pasta por módulo. Cada módulo tem seu próprio build.

```
backend/    Spring Boot 3 (Java 21, Maven wrapper)
frontend/   React 18 + Vite + TypeScript
database/   docker-compose.yml com PostgreSQL 16
```

- O schema do banco é versionado por Flyway, com as migrations em `backend/src/main/resources/db/migration`.
- O JPA usa `ddl-auto=validate`.
- A comunicação entre frontend e backend é REST JSON sob `/api`.
- No desenvolvimento, o proxy do Vite repassa `/api` para `localhost:8080`. O CORS do backend libera `http://localhost:5173`.
- A data de "hoje" vem sempre de um bean `Clock` com o fuso `America/Sao_Paulo`. Os testes injetam um `Clock` fixo.

## 3. Modelo de dados

**Tabela `tarefa`** (`V1__cria_tabela_tarefa.sql`):

| coluna | tipo | regra |
|---|---|---|
| `id` | `UUID` PK | gerado pelo backend |
| `titulo` | `VARCHAR(100) NOT NULL` | |
| `descricao` | `TEXT NULL` | |
| `data_vencimento` | `DATE NOT NULL` | |
| `prioridade` | `VARCHAR(10) NOT NULL` | `CHECK (prioridade IN ('BAIXA','MEDIA','ALTA'))` |
| `status` | `VARCHAR(10) NOT NULL` | `CHECK (status IN ('PENDENTE','CONCLUIDA'))` |
| `data_conclusao` | `DATE NULL` | `CHECK ((status = 'CONCLUIDA') = (data_conclusao IS NOT NULL))` |

A situação de prazo não é armazenada. Ela é calculada a cada resposta.

## 4. Regras de negócio

**RF02: situação**, calculada por `CalculadoraSituacao(status, dataVencimento, hoje)`:

| condição | situação |
|---|---|
| `status == CONCLUIDA` | `CONCLUIDA` |
| `PENDENTE` e `hoje > dataVencimento` | `ATRASADA` |
| `PENDENTE` e `hoje == dataVencimento` | `PARA_HOJE` |
| `PENDENTE` e `hoje < dataVencimento` | `NO_PRAZO` |

**Transições de status** (D5, D6):
- De `PENDENTE` para `CONCLUIDA`: `dataConclusao = hoje`.
- De `CONCLUIDA` para `PENDENTE`: `dataConclusao = null`.
- Status sem mudança: a `dataConclusao` é mantida.
- Criação com `status = CONCLUIDA`: `dataConclusao = hoje`.

**RF03: score semanal**:
- `inicioSemana` é a segunda-feira da semana de `hoje`; `fimSemana` é o domingo.
- O conjunto é formado pelas tarefas `CONCLUIDA` com `dataConclusao` entre `inicioSemana` e `fimSemana`, inclusive.
- `totalConcluidas` é o tamanho do conjunto.
- `concluidasNoPrazo` é a quantidade de tarefas do conjunto com `dataConclusao <= dataVencimento`.
- `percentualNoPrazo = concluidasNoPrazo / totalConcluidas * 100`, em `BigDecimal` com duas casas e `HALF_UP`. Vale `null` quando `totalConcluidas == 0`.

**Ordem da listagem:** pendentes antes das concluídas, depois `data_vencimento ASC`, depois `titulo ASC`.

## 5. API REST

Base: `/api`.

| método | rota | sucesso | erros |
|---|---|---|---|
| `GET` | `/tarefas` | 200, lista ordenada | |
| `GET` | `/tarefas/{id}` | 200 | 404 |
| `POST` | `/tarefas` | 201 + header `Location` | 400 |
| `PUT` | `/tarefas/{id}` | 200 | 400, 404 |
| `DELETE` | `/tarefas/{id}` | 204 | 404 |
| `GET` | `/produtividade/score-semanal` | 200 | |

**Entrada** (`TarefaRequest`):

| campo | regra |
|---|---|
| `titulo` | obrigatório, não pode ser só espaços, até 100 caracteres |
| `descricao` | opcional |
| `dataVencimento` | obrigatório, no formato `YYYY-MM-DD` |
| `prioridade` | obrigatório: `BAIXA`, `MEDIA` ou `ALTA` |
| `status` | obrigatório no `PUT`; opcional no `POST`, com padrão `PENDENTE` |

- O `PUT` é uma substituição completa.
- Os campos desconhecidos são ignorados, inclusive `dataConclusao`, `situacao` e `id`.

**Saída de uma tarefa** (`TarefaResponse`):
```json
{ "id": "…uuid…", "titulo": "Pagar boleto", "descricao": null,
  "dataVencimento": "2026-10-16", "prioridade": "ALTA", "status": "PENDENTE",
  "dataConclusao": null, "situacao": "PARA_HOJE" }
```

**Saída do score** (`ScoreSemanalResponse`):
```json
{ "inicioSemana": "2026-10-12", "fimSemana": "2026-10-18",
  "totalConcluidas": 5, "concluidasNoPrazo": 4, "percentualNoPrazo": 80.00 }
```

**Erros** (`application/problem+json`, RFC 9457, gerados por `TratadorGlobalErros`):
```json
{ "type": "about:blank", "title": "Dados inválidos", "status": 400,
  "detail": "Um ou mais campos são inválidos.", "instance": "/api/tarefas",
  "erros": [ { "campo": "titulo", "mensagem": "deve ter no máximo 100 caracteres" } ] }
```

| causa | status | observação |
|---|---|---|
| Bean Validation | 400 | lista `erros` preenchida |
| JSON malformado, enum ou data inválidos | 400 | `detail` descreve o problema |
| UUID inválido na rota | 400 | |
| Tarefa inexistente | 404 | |
| Exceção inesperada | 500 | mensagem genérica, sem stack trace (o stack trace vai só para o log) |

## 6. Backend: componentes

Pacote raiz: `br.com.demosdd.todo`.

```
tarefa/
  Tarefa                  entidade JPA; concluir(LocalDate hoje), reabrir()
  Prioridade, StatusTarefa, Situacao
  CalculadoraSituacao     classe pura, sem dependências do Spring
  TarefaRepository        Spring Data JPA; listagem ordenada; busca por concluídas no intervalo
  TarefaService           criar, buscar, listar, atualizar (decide a transição), excluir
  TarefaController
  dto/ TarefaRequest, TarefaResponse (+ mapeamento)
produtividade/
  CalculadoraScore        classe pura: cálculo da semana e do percentual
  ProdutividadeService    usa o Clock e o TarefaRepository
  ProdutividadeController
  dto/ ScoreSemanalResponse
comum/
  ClockConfig             Clock em America/Sao_Paulo
  CorsConfig
  TratadorGlobalErros     @RestControllerAdvice gerando ProblemDetail
  RecursoNaoEncontradoException
```

**Configuração** (`application.yml`): `DB_URL`, `DB_USER` e `DB_PASSWORD`, com padrões de desenvolvimento que apontam para o container local.

## 7. Frontend: componentes

React 18, Vite e TypeScript, sem biblioteca de estado e sem UI kit, com CSS simples.

```
src/
  tipos.ts                 Tarefa, TarefaInput, ScoreSemanal, Situacao, Prioridade, Status
  api/tarefasApi.ts        fetch tipado; converte problem+json em ErroApi { status, detail, erros[] }
  App.tsx                  carrega tarefas e score; recarrega os dois após qualquer mutação
  componentes/
    ScoreCard.tsx          "80,00% no prazo (4 de 5)" mais o intervalo da semana; null → "Nenhuma tarefa concluída nesta semana"
    TarefaForm.tsx         criar e editar; validação no cliente (título obrigatório até 100, vencimento obrigatório); erros por campo vindos do backend
    TarefaLista.tsx        título, prioridade, vencimento (dd/mm/aaaa), selo e ações
    SeloSituacao.tsx       Atrasada (vermelho), Para Hoje (laranja), No Prazo (azul), Concluída (verde)
```

**Ações por linha:**
- **Editar:** carrega a tarefa no formulário.
- **Concluir ou Reabrir:** envia um `PUT` com o status trocado.
- **Excluir:** pede confirmação com `window.confirm` e envia um `DELETE`.

**Estados da tela:**
- carregando;
- lista vazia: "Nenhuma tarefa cadastrada";
- erro de rede ou 500: banner com o `detail`.

O frontend não calcula situação nem score.

## 8. Testes

**Backend: unitários** (JUnit 5 + AssertJ, sem Spring):
- `CalculadoraSituacao`: as quatro situações; a fronteira em que o vencimento é hoje; concluída com vencimento passado resulta em `CONCLUIDA`.
- `CalculadoraScore`:
  - lista vazia resulta em `null`;
  - conclusão no dia do vencimento conta no prazo;
  - conclusão um dia depois conta fora do prazo;
  - 2 de 3 resulta em `66.67`;
  - limites da semana quando hoje é segunda ou domingo.
- `Tarefa.concluir` e `Tarefa.reabrir`.

**Backend: integração** (`@SpringBootTest` + MockMvc + Testcontainers com PostgreSQL 16 e `Clock` fixo):
- CRUD completo com os status 201, 200, 204 e 404;
- validações com 400 e lista `erros`;
- JSON malformado, enum inválido e UUID inválido com 400;
- ordem da listagem;
- concluir e depois reabrir limpa a `dataConclusao`;
- criação com `CONCLUIDA` grava a `dataConclusao`;
- o score ignora conclusões fora da semana;
- as migrations Flyway rodam em um banco limpo.

**Frontend** (Vitest + Testing Library, com fetch mockado):
- o formulário valida o título e a data;
- os erros de campo vindos do backend aparecem;
- cada selo mostra o texto e a cor certos;
- o ScoreCard trata o caso `null`;
- concluir e excluir chamam a API e recarregam a lista.

**Manual:** verificação no navegador ao final, com tarefas nas quatro situações e o score conferido.

## 9. Execução local (README)

1. `docker compose -f database/docker-compose.yml up -d` sobe o PostgreSQL 16 na porta 5432, com volume nomeado.
2. `cd backend && ./mvnw spring-boot:run` sobe a API em `http://localhost:8080`.
3. `cd frontend && npm install && npm run dev`, depois abrir `http://localhost:5173`.

O README terá uma seção de troubleshooting:
- o Docker Desktop precisa estar rodando para o banco e para os testes de integração;
- o antivírus pode interceptar o TLS do Maven. A correção é `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`.

## 10. Fora do escopo

- autenticação e múltiplos usuários;
- filtros, busca e paginação;
- histórico do score ou score de outras semanas;
- notificações de vencimento;
- deploy e build de produção integrado;
- testes E2E automatizados.

## 11. Critérios de pronto

- `./mvnw verify` (backend) e `npm test` (frontend) passam.
- O sistema sobe localmente seguindo o README.
- Na interface, dá para ter tarefas nas quatro situações.
- Depois de concluir tarefas no prazo e fora dele, o score mostra os valores esperados.
