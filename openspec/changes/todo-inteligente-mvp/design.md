# Design

## Context

O repositório ainda não tem código: só o esboço (`escopo_inicial.md`) e a estrutura do OpenSpec. A motivação está em `proposal.md` (Why), e o comportamento esperado está nas specs `task-management`, `deadline-status`, `productivity-score` e `task-web-ui`.

Restrições que moldam o desenho:
- O esboço pede um monólito modular com backend Spring Boot, PostgreSQL em container Docker e frontend React, comunicando-se por API REST JSON.
- As regras que dependem de "hoje" (situação de prazo, conclusão e score) precisam ser testáveis com datas fixas.
- O ambiente local tem JDK 21 no Maven, Node 24 e Docker Desktop. O Docker é necessário para o banco e para os testes de integração.

## Goals / Non-Goals

**Goals:**
- Concentrar as regras de negócio em código de domínio puro (sem Spring e sem banco), coberto por testes unitários com relógio fixo.
- Ter uma única fonte de "data atual" para toda a aplicação.
- Testar a API de ponta a ponta contra um PostgreSQL real.
- Manter o frontend fino: ele exibe o que a API calcula e não duplica regras.

**Non-Goals:**
- Empacotar frontend e backend num único artefato de deploy, ou preparar o projeto para produção (HTTPS, perfis de ambiente, observabilidade).
- Lidar com concorrência entre edições (sem lock otimista; o MVP tem um único usuário).
- Ter suporte a internacionalização: mensagens em português.

## Decisions

### Estrutura do repositório em três módulos

```
demo-sdd/
  backend/     Spring Boot (Maven wrapper)
  frontend/    React + Vite + TypeScript
  database/    docker-compose.yml do PostgreSQL
```

No backend, o código fica organizado em pacotes por funcionalidade (`tarefa`, `prazo`, `produtividade`) e um pacote `compartilhado` para configuração e tratamento de erros. Assim cada capability tem um lugar próprio.

*Alternativa considerada:* módulos Maven separados por camada. Para três funcionalidades pequenas, isso só acrescenta cerimônia.

### Stack do backend
Java 21 (LTS e versão do Maven local) com Spring Boot 3.5. Usa Spring Web, Spring Data JPA, Bean Validation, Flyway e driver PostgreSQL, com Maven Wrapper para builds reproduzíveis.

### Identificador UUID
O `id` é um `UUID` gerado pela aplicação. O esboço aceita UUID ou Long, e o UUID evita expor uma contagem sequencial. Um `{id}` que não é UUID gera `MethodArgumentTypeMismatchException`, que é traduzida em `400`.

### Schema versionado com Flyway
A migração `V1__criar_tabela_tarefa.sql` cria a tabela `tarefa` com as colunas `id uuid PK`, `titulo varchar(100) not null`, `descricao text`, `data_vencimento date not null`, `prioridade varchar(5) not null`, `status varchar(9) not null` e `data_conclusao date`. Há `CHECK` para os valores dos enums e para a consistência entre `status` e `data_conclusao`. O Hibernate roda com `ddl-auto=validate`. Não existe coluna para a situação de prazo, conforme o RF02.

*Alternativa considerada:* `ddl-auto=update`, descartada porque não deixa um histórico do schema e esconde divergências.

### Uma única fonte de "hoje": um `Clock` injetado
Há um bean `Clock` com o fuso definido pela propriedade `app.fuso` (padrão `America/Sao_Paulo`). Todo código que precisa da data atual chama `LocalDate.now(clock)`. Os testes unitários usam `Clock.fixed(...)`, e os testes de integração substituem o bean por um relógio fixo e ajustável.

*Alternativa considerada:* `LocalDate.now()` com o fuso da JVM. Os testes dependeriam do dia em que rodam, e o resultado dependeria do fuso do servidor.

### Regras de domínio isoladas e puras
- **Situação de prazo:** uma função pura `calcular(status, dataVencimento, hoje) -> SituacaoPrazo` no pacote `prazo`. O mapeamento para o DTO de resposta a chama, e por isso toda resposta traz o campo calculado.
- **Data de conclusão:** a entidade `Tarefa` expõe `alterarStatus(novoStatus, hoje)`, que aplica as transições da spec. De PENDENTE para CONCLUIDA, define a data de hoje. De CONCLUIDA para CONCLUIDA, preserva a data. De CONCLUIDA para PENDENTE, apaga a data. Não há setter público para `dataConclusao`.
- **Score:** uma função pura que recebe a data de hoje e as contagens e devolve o resultado, incluindo o cálculo da semana com `with(DayOfWeek.MONDAY)` e `plusDays(6)` e o percentual em `BigDecimal` com `RoundingMode.HALF_UP` e escala 2.

### Contagens do score feitas no banco
O repositório expõe duas consultas: o total de tarefas concluídas com `dataConclusao` entre o início e o fim da semana, e o mesmo total restrito a `dataConclusao <= dataVencimento`. Isso evita carregar todas as tarefas na memória. O `percentual` é serializado como número JSON (por exemplo, `66.67` e `100.00`), ou `null` quando não há tarefas concluídas na semana.

### Ordenação da listagem em memória
A listagem carrega todas as tarefas e ordena com um `Comparator` por `dataVencimento` crescente, depois por prioridade (`ALTA`, `MEDIA`, `BAIXA`) e por fim pelo `id`, que torna a ordem estável. Ordenar o enum guardado como texto exigiria um `CASE` em SQL. Sem paginação, a ordenação em memória é mais simples e fácil de testar.

*Alternativa considerada:* uma coluna numérica de peso da prioridade. Ela vira dado redundante só por causa da ordenação.

### Contrato da API e validação
- Os DTOs de entrada são `record`s separados para a criação (sem `status`) e para a atualização (com `status` obrigatório). Como eles não declaram `id`, `dataConclusao` nem `situacaoPrazo`, o Jackson ignora esses campos se o cliente os enviar.
- O construtor compacto do `record` faz o trim de `titulo` antes da validação (`@NotBlank`, `@Size(max = 100)`). Isso garante que a regra vale para o valor já sem os espaços das extremidades.
- Datas e enums com valor inválido falham na desserialização (`HttpMessageNotReadableException` com `InvalidFormatException`). O handler extrai o caminho do campo dessa exceção para preencher `errors`. Corpos que não são JSON válidos geram `400` sem `errors`.

### Erros com ProblemDetail
Um `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler` produz `ProblemDetail` (`application/problem+json`) para todos os erros, com a propriedade `errors: [{campo, mensagem}]` nos erros de validação. As tarefas inexistentes lançam uma exceção de domínio, mapeada para `404`. Qualquer outra exceção vira `500` com `detail` genérico, e o erro completo vai só para o log.

### Frontend: React + Vite + TypeScript, sem bibliotecas de estado
- Uma tela (`App`) com três componentes: `ListaTarefas`, `FormularioTarefa` e `CardScore`, mais um módulo `api.ts` que encapsula o `fetch` e converte as respostas de erro em ProblemDetail em mensagens por campo.
- O estado fica em `useState`/`useEffect`. Depois de cada operação que altera dados, a tela recarrega a lista e o score da API. Isso evita recalcular no cliente a situação de prazo e o score.
- Para concluir ou reabrir, a tela envia um `PUT` com a tarefa completa e o novo `status`.
- A exclusão pede confirmação com `window.confirm`.
- Em desenvolvimento, o proxy do Vite encaminha `/api` para `http://localhost:8080`, o que dispensa configurar CORS.

*Alternativa considerada:* um endpoint `PATCH` dedicado para o status. O `PUT` completo já cobre o caso, e a spec define a atualização só por `PUT`.

### Estratégia de testes
- **Backend, unitários:** regras de situação de prazo, transições de status e score, todos com `Clock` fixo. Cada scenario das specs com datas vira pelo menos um caso de teste.
- **Backend, camada web:** `@WebMvcTest` para validações e formato de erro.
- **Backend, integração:** `@SpringBootTest` com Testcontainers (PostgreSQL) cobrindo o fluxo REST completo e as migrações Flyway.
- **Frontend:** Vitest com React Testing Library, simulando a `api.ts`, para os scenarios de `task-web-ui`.

## Risks / Trade-offs

- [O fuso do container ou da JVM é diferente de São Paulo] → A data atual vem sempre do `Clock` configurado, e um teste cobre a virada de dia em UTC.
- [A ordenação em memória não escala para muitas tarefas] → Para um MVP pessoal sem paginação, o volume é pequeno. Se surgir paginação numa change futura, a ordenação passa para o banco.
- [Testes de integração exigem Docker em execução] → Os testes unitários e os `@WebMvcTest` rodam sem Docker. O README documenta como iniciar o Docker Desktop.
- [Recarregar lista e score após cada operação gera requisições extras] → Isso é aceitável para o volume do MVP e mantém o frontend sem regras duplicadas.
- [O `PUT` completo pode sobrescrever edições concorrentes] → Há um único usuário e nenhum lock otimista; esse limite fica registrado como não objetivo.

## Migration Plan

Não se aplica: o projeto é novo e não há dados nem versão anterior. Para subir localmente, a ordem é: `docker compose -f database/docker-compose.yml up -d`, depois o backend (o Flyway aplica a `V1` na inicialização) e por fim o frontend com `npm run dev`.
