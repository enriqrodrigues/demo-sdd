# Design

## Context

O repositório é greenfield: contém apenas o escopo inicial e a configuração do OpenSpec. Motivação e escopo estão em `proposal.md`; o comportamento exigido está nas specs `gestao-tarefas`, `prazo-tarefa`, `score-produtividade` e `interface-web`.

Restrições que moldam a solução:
- Monólito modular com três módulos independentes no mesmo repositório: backend, frontend e banco de dados.
- Credenciais exclusivamente por variável de ambiente; PostgreSQL publicado apenas em `127.0.0.1`.
- Ambiente local: JDK 21 no Maven, Node 24, Docker Desktop (precisa estar em execução para o banco e para os testes de integração).

## Goals / Non-Goals

**Goals:**
- Regras de prazo e score isoladas em código puro, testáveis sem banco e com tempo controlado.
- Testes de integração contra PostgreSQL real, não contra um banco em memória com dialeto diferente.
- Contrato HTTP único e previsível para erros, compartilhado por todos os endpoints.

**Non-Goals:**
- Deploy, empacotamento de produção ou CI — o alvo é execução local.
- Servir o frontend pelo backend; em desenvolvimento cada módulo roda com seu próprio servidor.
- Testes end-to-end automatizados no navegador; o comportamento da UI é verificado com testes de componente e a API simulada.

## Decisions

### D1. Estrutura do repositório

```
backend/    Spring Boot 3.5, Java 21, Maven Wrapper
frontend/   React 18 + TypeScript, Vite, Vitest + Testing Library
database/   docker-compose.yml (PostgreSQL 16), .env.example
```

O backend é organizado por módulo de domínio (`tarefa`, `prazo`, `produtividade`) mais um pacote `comum` com o tratamento de erros e o relógio. `prazo` e `produtividade` dependem de `tarefa` apenas por leitura; nada depende deles.

*Alternativa:* Maven multi-módulo com o frontend embutido via plugin — descartada por acoplar o build do React ao Maven sem ganho para a demo.

### D2. Tempo controlado por um `Clock` injetável e um fuso configurável

Um bean `java.time.Clock` é a única fonte de "agora". A propriedade `app.fuso-horario` (padrão `America/Sao_Paulo`) define o `ZoneId` usado em todas as conversões instante → data local. Nenhum código chama `LocalDate.now()` ou `Instant.now()` sem o `Clock`.

O bean `Clock` é `Clock.systemUTC()`; o fuso dele nunca é usado para obter datas. Toda data local de negócio ("hoje" e, a partir dela, a semana do score) vem de um único componente, `DataCorrente`, que calcula `LocalDate.ofInstant(clock.instant(), fuso)`. Nenhum código chama `LocalDate.now(clock)`.

Testes unitários usam `Clock.fixed(...)`; testes de integração substituem o bean por um relógio mutável de teste, permitindo cenários de "passagem do tempo" e viradas de dia/semana.

*Alternativa:* fuso enviado pelo cliente — descartado (decisão da exploração; usuário único).

### D3. Situação de prazo calculada no mapeamento para DTO

`CalculadoraPrazo` é uma função pura `(status, dataVencimento, hoje) → SituacaoPrazo`. O mapeador entidade → DTO a invoca com `hoje` obtido de `DataCorrente` (D2). A entidade não tem campo de situação, e nada a persiste.

O filtro `?situacaoPrazo=` é traduzido para predicados de consulta sobre `status` e `dataVencimento` relativos a `hoje` (ex.: `ATRASADA` ⇒ `status = PENDENTE AND dataVencimento < hoje`), reutilizando a mesma definição em um único lugar para não divergir da calculadora — um teste confronta os dois caminhos.

*Alternativa:* filtrar em memória após carregar tudo — viável pelo volume, mas a tradução para predicados mantém a listagem correta se a ordenação/consulta crescerem.

### D4. Score calculado sob demanda

`ServicoProdutividade` calcula a semana a partir de `hoje` obtido de `DataCorrente` (D2), com `TemporalAdjusters.previousOrSame(MONDAY)`, converte `[segunda 00:00, segunda seguinte 00:00)` no fuso configurado para um intervalo de `Instant` e consulta as tarefas `CONCLUIDA` com `concluida_em` nesse intervalo semiaberto. A classificação "no prazo" (`concluidaEm` na data local ≤ `dataVencimento`) e o percentual (`BigDecimal`, `HALF_UP`, escala 2; `null` se 0 concluídas) ficam em uma função pura testada isoladamente.

Por ser calculado a cada consulta, o score reflete automaticamente reabertura, exclusão e edição de vencimento.

*Alternativa:* tabela de eventos de conclusão — descartada; histórico não é requisito e a reabertura deve remover a tarefa do score.

### D5. Modelo de dados e migrações

Flyway gerencia o schema (`V1__cria_tarefa.sql`). Tabela `tarefa`:

| coluna            | tipo                          |
|-------------------|-------------------------------|
| `id`              | `uuid` PK (gerado pela aplicação) |
| `titulo`          | `varchar(100) not null`       |
| `descricao`       | `varchar(2000)`               |
| `data_vencimento` | `date not null`               |
| `prioridade`      | `varchar(5) not null` + `check` |
| `status`          | `varchar(9) not null` + `check` |
| `concluida_em`    | `timestamptz`                 |
| `version`         | `bigint not null`             |

Constraint `check ((status = 'CONCLUIDA') = (concluida_em is not null))` garante a invariante no banco. Índice em `(status, concluida_em)` para o score. Enums são gravados como texto (`@Enumerated(STRING)`), nunca ordinal. Hibernate fica em `ddl-auto=validate`.

A ordenação por prioridade decrescente usa uma expressão `case` (`ALTA`=3, `MEDIA`=2, `BAIXA`=1), já que a ordem textual não corresponde à semântica.

*Alternativa:* `id` `bigserial` — o escopo permitia ambos; UUID evita ids enumeráveis e simplifica o 400 para id malformado.

### D6. Concorrência otimista via `@Version` com checagem explícita

A entidade usa `@Version`. No `PUT`, o serviço compara a `version` recebida com a carregada e lança `ConflitoVersaoException` (→ 409) se diferirem; o `@Version` do JPA cobre a corrida residual entre leitura e commit (`ObjectOptimisticLockingFailureException` → também 409). Concluir/reabrir não recebem `version`; quando já estão no estado alvo, retornam sem escrita, então `version` não muda (idempotência exigida pela spec).

### D7. Desserialização estrita e erros em Problem Details

Jackson configurado com `FAIL_ON_UNKNOWN_PROPERTIES=true`, `FAIL_ON_NUMBERS_FOR_ENUMS=true` e `ACCEPT_CASE_INSENSITIVE_ENUMS=false`. `dataVencimento`, assim como `prioridade`, chega como texto no DTO de entrada e é validada por um validador de bean: apenas `YYYY-MM-DD` estrito e data real (`ResolverStyle.STRICT`, rejeitando `2026-02-30`). Os campos somente leitura (`id`, `status`, `concluidaEm`, `situacaoPrazo`) são declarados no DTO de entrada e marcados como ignorados, para que sejam aceitos e descartados — e não rejeitados como desconhecidos. `version` existe apenas no DTO de edição.

Um único `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler` produz `ProblemDetail` (`spring.mvc.problemdetails.enabled=true`) para: validação de bean (agrega todos os campos em `erros[{campo, mensagem}]`), JSON malformado e erros de tipo/enum/data do Jackson (mapeados para o campo de origem quando identificável), parâmetro de filtro ou UUID inválido (400), tarefa inexistente (404), conflito (409) e qualquer outra exceção (500 com `detail` genérico, sem stack trace; o detalhe vai só para o log). Para que todos os campos inválidos sejam reportados juntos, o DTO recebe `prioridade` e `dataVencimento` como texto e a validação de domínio é feita por validadores de bean, em vez de o Jackson abortar no primeiro valor inválido.

### D8. Configuração e segurança local

`application.yml` lê `DB_URL`, `DB_USUARIO`, `DB_SENHA` do ambiente, sem valores padrão com credenciais. `database/docker-compose.yml` publica `127.0.0.1:5432:5432` e lê as credenciais de `database/.env` (ignorado pelo git; `.env.example` versionado). O backend escuta em `127.0.0.1:8080`. CORS libera apenas a origem do Vite (`http://localhost:5173`), configurável.

### D9. Testes

- **Unitários (JUnit 5 + AssertJ):** `CalculadoraPrazo`, cálculo de semana/score, validações de DTO.
- **Integração (`@SpringBootTest` + MockMvc + Testcontainers PostgreSQL 16):** um teste por scenario de API das specs `gestao-tarefas`, `prazo-tarefa` e `score-produtividade`, usando o relógio de teste. Um container compartilhado por suíte (`@ServiceConnection`), com limpeza da tabela entre testes.
- **Frontend (Vitest + Testing Library + MSW):** um teste por scenario de `interface-web`, com a API simulada pelo MSW — inclusive respostas 409/404/500 e atrasos para os cenários de duplo clique.

### D10. Frontend

Estado em um hook `useTarefas` (lista, filtro, ids em operação, tarefa em edição) e um cliente HTTP fino em `api.ts` que transforma Problem Details em erros tipados (`ErroValidacao`, `ErroConflito`, `ErroNaoEncontrado`, `ErroComunicacao`). Sem biblioteca de estado ou de dados (React Query, Redux): o volume e o número de telas não justificam. O filtro da UI mapeia para a API: "Pendentes" → `status=PENDENTE`, "Atrasadas" → `situacaoPrazo=ATRASADA`, "Para hoje" → `situacaoPrazo=PARA_HOJE`. A confirmação de exclusão usa um diálogo próprio acessível (não `window.confirm`), facilitando o teste. Formatação de datas e números em `pt-BR` via `Intl`. Em desenvolvimento o Vite faz proxy de `/api` para o backend.

## Risks / Trade-offs

- [Avast intercepta TLS e quebra downloads do Maven] → documentar no README a execução com `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`, sem alterar arquivos versionados.
- [Docker Desktop parado faz os testes de integração falharem de forma pouco clara] → README orienta iniciar o Docker; os testes unitários de regra não dependem dele.
- [Predicados do filtro por situação divergirem da `CalculadoraPrazo`] → teste de integração que, para um conjunto de tarefas cobrindo os quatro casos, compara o filtro com a situação calculada de cada item.
- [Validação agregada x falha precoce do Jackson em enum ou data inválidos] → receber `prioridade` e `dataVencimento` como texto no DTO de entrada (D7); teste específico do cenário "Vários campos inválidos".
- [Editar `dataVencimento` de tarefa concluída altera o score retroativamente] → comportamento aceito e especificado (cenário "Vencimento editado após a conclusão").
- [Sem paginação] → aceitável para usuário único; revisitar se o volume crescer.

## Migration Plan

Não há sistema anterior nem dados a migrar. Execução local: subir `database/` com `docker compose up -d`, exportar as variáveis do banco, rodar o backend (`./mvnw spring-boot:run`) — o Flyway cria o schema na primeira inicialização — e o frontend (`npm run dev`). Rollback = derrubar os containers e o volume.
