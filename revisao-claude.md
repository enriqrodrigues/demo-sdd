# Revisão de código: OpenSpec × Superpowers × Híbrido

Revisão das três implementações do **To-Do Inteligente (MVP)** descrito em [`escopo_inicial.md`](escopo_inicial.md):

| Branch | Framework de SDD | Commits | Código (main + testes) |
| --- | --- | --- | --- |
| `release/demo-openspec` | OpenSpec | 4 | ~2.300 linhas de código + ~1.870 de testes |
| `release/demo-superpowers` | Superpowers | 18 | ~1.800 linhas de código + ~1.300 de testes |
| `release/demo-combinado-openspec-superpowers` | Híbrido: OpenSpec no planejamento, Superpowers na execução | 44 | ~2.300 linhas de código + ~3.400 de testes |

Revisor: Claude (Opus 5.5). OpenSpec e Superpowers revisados em 18/10/2026; Híbrido em 19/10/2026, com os mesmos critérios.

**Como funciona o modo híbrido.** O `CLAUDE.md` e o `openspec/config.yaml` da branch dividem o trabalho: o OpenSpec cuida do planejamento (`/opsx:explore`, `/opsx:propose`) e do acompanhamento das tarefas (`tasks.md`, `/opsx:archive`). O Superpowers entra na execução: TDD em cada tarefa, um commit por tarefa com o checkbox marcado no mesmo commit, revisão de código ao fim de cada seção do `tasks.md` e verificação antes do arquivamento. Problemas de spec achados na revisão passam por `/opsx:update` antes da correção.

> **Ressalva sobre a comparação.** O híbrido não partiu do zero como os outros dois. O `openspec/config.yaml` traz, como restrições de projeto, lições da primeira rodada desta revisão: credenciais por variável de ambiente, limite nos campos de texto, enums sem valores numéricos, Postgres só em `127.0.0.1` e cenários de interação entre ações sobre a mesma entidade, como "editar enquanto conclui". Parte da vantagem nos critérios 3, 7 e 9 vem dessas regras, e não só do processo. Ainda assim, o processo foi o que transformou as regras em cenários, testes e código sem regressões.

## Como a revisão foi feita

- Leitura integral do backend (Java/Spring Boot), do frontend (React/TypeScript), das migrations, do `docker-compose.yml`, dos READMEs e dos documentos de especificação de cada branch.
- Execução local das suítes, com Docker Desktop ativo:

  | Verificação | OpenSpec | Superpowers | Híbrido |
  | --- | --- | --- | --- |
  | `./mvnw verify` (unitários + Testcontainers) | ✅ 81 testes, 0 falhas | ✅ 58 testes, 0 falhas | ✅ 125 testes, 0 falhas |
  | `npm test` (Vitest) | ✅ 38 testes | ✅ 38 testes | ✅ 61 testes |
  | `tsc` + `npm run build` | ✅ | ✅ | ✅ |
  | Lint | ✅ `oxlint` sem avisos | não há lint configurado | ✅ `eslint` sem avisos |
  | `npm audit` | ✅ 0 vulnerabilidades | ❌ 4 (2 críticas, 1 alta, 1 moderada) | ✅ 0 vulnerabilidades |

- Um teste de reprodução escrito à parte (não versionado) confirmou o bug de frontend do OpenSpec descrito em [Corretude](#3-corretude).
- No híbrido, o histórico foi conferido commit a commit: os 41 commits de implementação, revisão e arquivamento, e se cada um marca o checkbox correspondente no `tasks.md`.

## Resumo das notas

| # | Critério | OpenSpec | Superpowers | Híbrido |
| --- | --- | :---: | :---: | :---: |
| 1 | Documentação | **9** | 8 | **9** |
| 2 | Aderência aos requisitos | **9** | **9** | **9** |
| 3 | Corretude (bugs encontrados) | 7 | 9 | **10** |
| 4 | Arquitetura e design | **9** | 8 | **9** |
| 5 | Qualidade e legibilidade do código | **9** | 8 | **9** |
| 6 | Testes | 8 | 8 | **9** |
| 7 | Segurança | 7 | 6 | **9** |
| 8 | Dependências e build | 8 | 6 | **9** |
| 9 | Frontend: UX e acessibilidade | **9** | 7 | 8 |
| 10 | Rastreabilidade do processo (spec → código → commits) | 6 | **9** | **9** |
| 11 | Operação e execução local | **9** | 8 | **9** |
| | **Média** | **8,2** | **7,8** | **9,0** |

As três implementações são boas e funcionam: todos os testes passam e os três requisitos (RF01 a RF03) estão cobertos. O OpenSpec entregou código, documentação e acessibilidade um pouco melhores, mas com um bug real de interface e um histórico de commits que não deixa ver como o código chegou lá. O Superpowers entregou um processo exemplar (TDD com commits atômicos) e menos bugs, mas com dependências desatualizadas e vulneráveis e um frontend menos cuidadoso. O Híbrido juntou as duas forças: specs vivas com cenários, um commit por tarefa ligado ao `tasks.md` e nenhum bug encontrado. Ele custou mais (44 commits, revisões de código por seção, quatro delas com commit de correção, e uma da branch inteira) e herdou lições da revisão anterior (ver a ressalva acima).

---

## 1. Documentação

**OpenSpec: 9**

- O README é completo: tabela de módulos e portas, pré-requisitos com versões, passo a passo, tabela da API com códigos de status, exemplo `curl`, formato dos erros e uma seção de troubleshooting detalhada (Docker parado, antivírus interceptando TLS para Maven, npm e curl, porta 5432 ocupada).
- As specs vivas em `openspec/specs/` têm quatro capacidades (`task-management`, `deadline-status`, `productivity-score`, `task-web-ui`), com requisitos em linguagem normativa (SHALL) e cenários WHEN/THEN. Dá para auditar regra por regra, por exemplo "Virada de dia em UTC antes de São Paulo" ou "Tarefa reaberta" no score.
- As mudanças ficam arquivadas (`openspec/changes/archive/`) com proposta, design, tarefas e deltas de spec, inclusive a evolução "exibir data de conclusão".
- Javadoc curto e útil nos pontos não óbvios (`TarefaService.ORDEM_LISTAGEM`, `Tarefa.alterarStatus`, `ConfiguracaoRelogio`).
- Falta pouco: o design do MVP (102 linhas) é mais enxuto que o da outra branch, e não há documentação de implantação fora do ambiente local.

**Superpowers: 8**

- O documento de design (`docs/superpowers/specs/2026-10-16-todo-inteligente-design.md`, 252 linhas) é muito bom: arquitetura, modelo de dados, regras, API, componentes, estratégia de testes, fora de escopo e critérios de pronto.
- O plano de implementação tem 3.710 linhas, com o código de cada passo embutido. Ajuda a rastrear o que foi feito, mas duplica o código-fonte e vai ficar desatualizado no primeiro refactor. Como documentação de referência pesa mais do que ajuda.
- O README é correto, mas enxuto: sem exemplo de requisição, sem códigos de status na tabela da API, com troubleshooting curto. Documenta as variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`, o que o OpenSpec não tem.
- Não há uma spec viva consolidada: o comportamento atual é a soma do design inicial com o design incremental da coluna de conclusão.

**Híbrido: 9**

- As specs vivas (`openspec/specs/`) são as mais detalhadas das três: quatro capacidades (`gestao-tarefas`, `prazo-tarefa`, `score-produtividade`, `interface-web`), 34 requisitos e 104 cenários em 547 linhas. A spec de interface descreve também o comportamento diante de ações concorrentes ou repetidas (duplo clique, conflito entre abas, tarefa excluída durante uma ação).
- O design arquivado (120 linhas) registra as decisões numeradas (D1 a D10), citadas depois em comentários do código e nas tarefas, por exemplo `// Corrida residual entre a checagem explícita de versão e o commit (D6)`. A proposta lista o que ficou fora do escopo.
- O `tasks.md` é enxuto (52 linhas) e diz, para cada tarefa, quais cenários ela cobre e qual teste a verifica. Faz o papel do plano do Superpowers sem duplicar o código.
- O README tem pré-requisitos, tabela de variáveis de ambiente e propriedades opcionais (`app.fuso-horario`, `app.cors.origens-permitidas`), comandos em PowerShell, um roteiro de verificação manual da interface em oito passos e troubleshooting de TLS (Maven e npm) e Docker.
- Perde um ponto porque o README não descreve a API: não tem tabela de endpoints, códigos de status, exemplo de requisição nem formato dos erros. Para isso é preciso ler a spec.

## 2. Aderência aos requisitos

**OpenSpec: 9 · Superpowers: 9 · Híbrido: 9**

As três atendem a RF01, RF02 e RF03 com decisões equivalentes:

- UUID gerado na aplicação, título obrigatório com até 100 caracteres (com `strip()` antes da validação), `dataVencimento` obrigatória, enums com `CHECK` no banco.
- A situação de prazo é calculada a cada resposta e nunca persistida. As três acrescentaram `NO_PRAZO` para pendentes com vencimento futuro, uma lacuna do escopo que todas resolveram do mesmo jeito.
- O score considera as tarefas concluídas na semana corrente (segunda a domingo), com a data de conclusão comparada ao vencimento, percentual com duas casas e `null` quando não há conclusões.
- O "hoje" vem de um `Clock` no fuso `America/Sao_Paulo`, e os testes usam um relógio ajustável.
- As três criaram uma coluna de conclusão com uma constraint que garante coerência com o `status`. Sem ela o RF03 não seria calculável.

Diferenças de interpretação, nenhuma errada:

- Ordenação: o OpenSpec ordena por vencimento e depois prioridade (ALTA → BAIXA). O Superpowers põe as pendentes antes das concluídas, depois ordena por vencimento e título. O Híbrido ordena por vencimento, prioridade decrescente e título.
- O Superpowers aceita `status: CONCLUIDA` já no `POST`; o OpenSpec ignora `status` na criação.
- O Híbrido tira o `status` do `PUT`: a mudança de status é feita só por `POST /api/tarefas/{id}/concluir` e `/reabrir`, idempotentes, e `status` enviado no corpo é aceito e descartado. A conclusão é gravada como instante (`concluida_em timestamptz`), e não como data, e o instante é convertido para data no fuso configurado na hora de classificar "no prazo".
- O Híbrido acrescentou filtros na listagem (`status` e `situacaoPrazo`, com "Todas", "Pendentes", "Atrasadas" e "Para hoje" na interface) e concorrência otimista na edição (`version` obrigatória, 409 em conflito).
- A lista do Híbrido não mostra a descrição nem a data de conclusão. A spec não pede, mas a descrição só aparece ao abrir a edição.

## 3. Corretude

**OpenSpec: 7**

- **Bug confirmado: salvar uma edição reverte uma conclusão feita na lista.** Em `frontend/src/App.tsx`, `salvar()` envia `status: emEdicao.status`, e `alternarStatus()` não atualiza `emEdicao`. Para reproduzir: clique em **Editar** numa tarefa pendente, marque o checkbox de concluir a mesma tarefa na lista e clique em **Salvar alterações**. O `PUT` sai com `status: PENDENTE`, a tarefa é reaberta e a `dataConclusao` é apagada, o que também altera o score. Um teste de reprodução com Vitest confirmou o comportamento. A correção é a mesma da outra branch: atualizar `emEdicao` com a resposta do `PUT` de alternância.
- Detalhe menor: em `excluir()`, o formulário em edição é fechado antes da chamada à API, então ele se perde mesmo quando a exclusão falha.
- O backend não tem defeitos encontrados. A transição em `Tarefa.alterarStatus` (concluir, manter concluída preservando a data, reabrir) está correta e testada.

**Superpowers: 9**

- O mesmo cenário foi tratado explicitamente (commit `2faa06c`, *"mantém o formulário de edição sincronizado após concluir/reabrir"*), com teste de regressão em `App.test.tsx`.
- **Defeito menor:** o Jackson aceita números para enums por padrão, então `"prioridade": 1` é gravado como `MEDIA`. O OpenSpec bloqueia isso com `fail-on-numbers-for-enums: true` em `application.yml`.
- Erros de enum ou data inválidos no corpo voltam só com `detail` ("Valor inválido para o campo 'prioridade'"), sem a lista `erros`. O formulário mostra isso como erro geral, e não junto ao campo.

**Híbrido: 10**

- Nenhum defeito encontrado. O cenário que derrubou o OpenSpec não acontece aqui, por três camadas de proteção: o `PUT` não altera `status`; concluir, reabrir e excluir ficam desabilitados na linha da tarefa que está em edição; e, se a tarefa for concluída em outra aba, o `PUT` com a `version` antiga recebe 409. Nesse caso a interface recarrega a tarefa, mantém o texto digitado e aceita um segundo salvar. Tudo isso tem cenário na spec e teste (`ConflitoEdicao.test.tsx`, `EditarTarefaIT`).
- Enum numérico é rejeitado em duas camadas: `fail-on-numbers-for-enums` no Jackson e `prioridade` recebida como texto e validada por `@ValorEnum`, que exige o nome exato da constante. Erros de enum, data e versão voltam na lista `erros`, junto ao campo.
- Casos de borda tratados: data impossível (`2026-02-30`) e ano 0 rejeitados por `@DataIso` em modo `STRICT`, com comentário explicando que o PostgreSQL não tem ano 0; instante de conclusão truncado a microssegundos para bater com o `timestamptz`; uma única leitura de "hoje" por resposta de listagem; corrida entre a checagem de versão e o commit coberta pelo `@Version`.
- Observação, sem desconto: o Jackson ainda converte escalares em texto (`"titulo": 123` vira `"123"`), porque a coerção de escalares não foi desligada. Não fere nenhuma regra do escopo.

## 4. Arquitetura e design

**OpenSpec: 9**

- Pacotes por capacidade (`tarefa`, `prazo`, `produtividade`, `compartilhado`), alinhados às specs.
- DTOs separados para criação e atualização (`CriarTarefaRequest` sem `status`, `AtualizarTarefaRequest` com `status` obrigatório). É mais explícito que grupos de validação.
- O serviço devolve DTOs, e o controller fica fino. `Semana` é um record com a regra da semana. `RecursoNaoEncontradoException` é abstrata, com título específico por recurso.
- O score usa `count` direto no banco (`contarConcluidasEntre` e `contarConcluidasNoPrazoEntre`).
- Ponto fraco: `listar()` faz `findAll()` e ordena em memória. Para um MVP pessoal não é problema, mas não escala e impede paginação.

**Superpowers: 8**

- A ordenação é feita no banco por JPQL (`listarOrdenado`), uma escolha melhor que a do OpenSpec.
- O serviço devolve a entidade, e o controller monta o `TarefaResponse` chamando `service.hoje()`. Isso expõe a entidade fora da transação (com `open-in-view: false` funciona porque não há associações lazy, mas é frágil) e espalha a obtenção de "hoje" entre camadas.
- Um único `TarefaRequest` com grupo de validação `Atualizacao` funciona, mas é menos legível que dois DTOs.
- O score carrega as entidades da semana para contar em Java (`findByStatusAndDataConclusaoBetween`), quando bastaria um `count`.
- O fuso é uma constante (`ClockConfig.FUSO`); no OpenSpec é configurável por `app.fuso`.

**Híbrido: 9**

- Pacotes por capacidade (`tarefa`, `prazo`, `produtividade`, `comum`), com `prazo` como módulo folha. A dependência entre módulos está documentada (D1) e protegida por teste (`DependenciasModulosTest`, `DependenciasPrazoTest`).
- As regras de negócio são funções puras sem Spring (`CalculadoraPrazo`, `CalculadoraScore`, `Semana`), testadas sem contexto. "Hoje" tem uma única fonte, `DataCorrente`, que combina o `Clock` em UTC com o fuso configurável (`app.fuso-horario`).
- Ordenação e filtros rodam no banco por `Specification`, com a prioridade ordenada por peso numa expressão `case`. O filtro por situação de prazo repete em SQL a regra da calculadora, e um teste confronta os dois para que não divirjam.
- Concorrência otimista em duas camadas: checagem explícita da `version` enviada pelo cliente, mais `@Version` para a corrida entre a checagem e o commit.
- Os mesmos pontos fracos do Superpowers, em menor grau: o serviço devolve a entidade e o controller a converte (`MapeadorTarefa`); um único `TarefaRequisicao` com grupo de validação `Edicao`; e o score carrega as entidades da semana para classificar em Java. Neste caso há um motivo, porque a comparação "no prazo" converte o instante para data no fuso, mas ainda daria para contar no banco com `AT TIME ZONE`.

## 5. Qualidade e legibilidade do código

**OpenSpec: 9**

- Nomes de domínio em português, consistentes e expressivos. Mensagens de validação centralizadas em `MensagensValidacao`.
- O tratamento de erros é o mais completo das duas: um item por campo (`putIfAbsent`), caminho completo do campo vindo do Jackson, mensagem que lista os valores aceitos do enum e mensagem específica para data inválida.
- Comentários explicam o porquê (`// sem isto, "prioridade": 1 seria aceito como MEDIA`).
- Frontend com `oxlint` configurado e TypeScript em modo estrito (padrão do TS 6, confirmado na verificação).

**Superpowers: 8**

- Código limpo e conciso, com comentários pontuais e úteis (por exemplo, o motivo de não usar `Date` em `formatarData`, e a versão do Testcontainers fixada por causa do Docker Engine 29).
- Mensagens de validação soltas ("é obrigatório", "é obrigatória") só fazem sentido concatenadas ao nome do campo, e o cliente não faz essa concatenação.
- Não há linter no frontend. O `tsconfig.json` tem `strict: true`, o que é bom.

**Híbrido: 9**

- Nomes em português, consistentes no backend e no frontend (`ServicoTarefa`, `MapeadorTarefa`, `useTarefas`, `operar`, `removerInexistente`). Mensagens de validação são frases completas que fazem sentido sozinhas ("Informe o título.", "A prioridade deve ser BAIXA, MEDIA ou ALTA.").
- O tratamento de erros cobre também as exceções do próprio Spring MVC (rota inexistente, método não permitido, tipo de conteúdo), com textos em português e preservando cabeçalhos como `Allow`. A mensagem de 404 não repete o identificador pedido.
- Comentários explicam o porquê, inclusive no frontend (`// O estado só reflete após renderizar; o ref barra um segundo clique no mesmo instante.`).
- Frontend com ESLint (regras de hooks incluídas), TypeScript `strict`, `noUnusedLocals` e `noUnusedParameters`. O cliente HTTP converte Problem Details em erros tipados (`ErroValidacao`, `ErroConflito`, `ErroNaoEncontrado`, `ErroComunicacao`).
- Pontos menores: as mensagens de validação ficam nas anotações, sem um lugar central como no OpenSpec; o controle de aberturas do formulário em `App.tsx` (contador em `useRef` para ignorar respostas de uma abertura anterior) é correto, mas exige atenção para ler; e alguns comentários TypeScript usam a sintaxe `{@code}` do Javadoc.

## 6. Testes

**OpenSpec: 8 · Superpowers: 8 · Híbrido: 9**

| | OpenSpec | Superpowers | Híbrido |
| --- | --- | --- | --- |
| Backend | 81 (75 `@Test` + 3 `@ParameterizedTest`) | 58 | 125 |
| Frontend | 38 | 38 | 61 |
| Tipos | unitários de domínio, `@WebMvcTest` só da validação, repositório, serviço, API completa com Testcontainers | unitários de domínio, repositório, migração, relógio, API completa e atualização com Testcontainers | unitários de domínio e de dependência entre módulos (Surefire, sem Docker); migração, CORS, erros do framework, fuso e API completa com Testcontainers (Failsafe); componentes com MSW |

- **OpenSpec:** cobertura de backend mais ampla, com uma fatia `@WebMvcTest` que roda sem Docker (e um README que explica como rodar só esses testes). Os testes parametrizados cobrem as regras de prazo e score. Perde ponto porque a suíte do frontend não pegou o bug de reversão de status, um fluxo que combina duas ações comuns.
- **Superpowers:** a suíte foi construída por TDD, como mostra o histórico, e inclui testes de regressão para bugs achados durante o desenvolvimento, testes de CORS (origem permitida e recusada), um teste de migração Flyway e um de 100 caracteres acentuados no título. Tem menos casos no backend e não testa enum numérico.
- **Híbrido:** a maior suíte das três, com um teste por cenário da spec, citado no `tasks.md`. Os testes de integração ficam separados dos unitários (`*IT` no Failsafe), então `mvnw test` roda sem Docker. O frontend usa MSW com uma API simulada e cobre as interações: duplo clique em salvar e em concluir (contando as requisições recebidas), conflito 409 com segundo salvar, tarefa removida durante uma ação, falha de rede e cancelamento da exclusão sem `DELETE`. Há também um relógio de teste ajustável validado por teste próprio.
- O Híbrido perde um ponto porque a verificação no navegador (tarefa 6.1) não deixou artefato: o commit `df33e9b` só marca o checkbox, sem script nem registro do roteiro executado. Além disso, o teste de dependência entre módulos procura `import` no texto dos arquivos, mais frágil que uma ferramenta como ArchUnit.

## 7. Segurança

Autenticação e autorização estão fora do escopo do MVP nas três branches, e por isso não entraram na nota. Também nas três: o JPA e as queries parametrizadas evitam SQL injection, o React escapa o conteúdo (não há `dangerouslySetInnerHTML`) e os erros 500 devolvem uma mensagem genérica, com o detalhe só no log.

Problemas comuns a OpenSpec e Superpowers:

- `descricao` é `text` sem limite de tamanho, então um corpo arbitrariamente grande é aceito e gravado.
- `GET /api/tarefas` não tem paginação nem limite.
- O `docker-compose.yml` publica a porta 5432 em todas as interfaces (`"5432:5432"`) com usuário e senha `todo`. Em rede local, isso expõe o banco. O ideal seria `"127.0.0.1:5432:5432"`.
- Não há controle de concorrência (`@Version`): dois `PUT` simultâneos resultam em *last write wins*.

**OpenSpec: 7**

- `npm audit` sem vulnerabilidades e Spring Boot 3.5.16.
- Sem CORS no backend: o frontend usa o proxy do Vite, o que é um padrão seguro (mesma origem).
- Bloqueia enums numéricos.
- Credenciais do banco fixas em `application.yml`, sem possibilidade de override por variável de ambiente sem editar o arquivo. Por convenção o Spring aceita `SPRING_DATASOURCE_PASSWORD`, mas isso não está documentado.

**Superpowers: 6**

- Credenciais externalizadas (`${DB_PASSWORD:todo}`) e documentadas.
- CORS restrito a `http://localhost:5173` e aos métodos usados, com teste. Na prática é desnecessário com o proxy, e a origem está fixa no código.
- **`npm audit`: 4 vulnerabilidades** nas versões fixadas de `vite@6.3.5`, `vitest@3.2.4` e `tinypool`, incluindo duas críticas (prototype pollution levando a RCE no tinypool) e várias de leitura arbitrária de arquivos pelo dev server do Vite, algumas específicas de Windows. São dependências de desenvolvimento, mas o dev server roda na máquina do desenvolvedor, e em Windows, como neste projeto.
- Aceita enums numéricos.

**Híbrido: 9**

- Resolve três dos quatro problemas comuns às outras branches: `descricao` limitada a 2.000 caracteres no DTO e na coluna (`varchar(2000)`); Postgres publicado só em `127.0.0.1`; `@Version` com checagem explícita da versão na edição.
- Credenciais só por variável de ambiente, sem valor padrão no código (`${DB_SENHA}`), com `database/.env` fora do versionamento e um `.env.example` com senha de exemplo. O backend também escuta só em `127.0.0.1` (`server.address`).
- Entrada estrita: propriedades desconhecidas rejeitadas, enums numéricos ou com outra caixa rejeitados, `accept-float-as-int: false` e data em formato estrito.
- CORS restrito às origens configuradas (`app.cors.origens-permitidas`) e aos métodos usados, com teste de origem aceita e recusada. A resposta de 404 não repete o identificador pedido.
- `npm audit` sem vulnerabilidades e Spring Boot 3.5.16.
- Continua sem paginação na listagem, uma decisão declarada como fora de escopo na proposta.

## 8. Dependências e build

**OpenSpec: 8**

- Versões atuais (React 19, Vite 8, TypeScript 6, Vitest 5, Spring Boot 3.5.16), faixas `^` com `package-lock.json` versionado, `build` com `tsc -b`, script de lint.
- `.gitignore` separado por módulo. Não há `.gitattributes`, o que deixa o `mvnw` sujeito a conversão de fim de linha no Windows.
- PostgreSQL 17.

**Superpowers: 6**

- Versões fixadas exatamente (bom para reprodutibilidade), mas defasadas: React 18, Vite 6.3.5, Spring Boot 3.5.7. Isso causou as vulnerabilidades da seção anterior.
- Bom cuidado de infraestrutura: `.gitattributes` com `mvnw text eol=lf` e `*.cmd eol=crlf`, e Testcontainers fixado com justificativa.
- Sem linter. PostgreSQL 16.

**Híbrido: 9**

- Ferramental atual (Vite 8, Vitest 5, ESLint 10, MSW 3, Spring Boot 3.5.16), faixas `^` com `package-lock.json` versionado, `build` com `tsc -b`, scripts de lint e de teste, e `engines` exigindo Node 24.
- `.gitattributes` no backend com `mvnw text eol=lf` e `*.cmd eol=crlf`, como no Superpowers.
- Separação Surefire/Failsafe no `pom.xml`, que permite rodar os unitários sem Docker.
- Perde um ponto por ficar uma versão atrás em parte da base: React 18 em vez de 19, TypeScript 5.9 em vez de 6 e PostgreSQL 16 em vez de 17. São versões suportadas e sem vulnerabilidades conhecidas.

## 9. Frontend: UX e acessibilidade

**OpenSpec: 9**

- Acessibilidade bem cuidada: `aria-invalid` e `aria-describedby` ligando cada campo à sua mensagem de erro, `aria-label` com o título da tarefa em cada checkbox e botão ("Excluir Pagar conta"), regiões nomeadas e `role="alert"` nos erros.
- Layout em duas colunas (score e formulário ao lado da lista), estado de carregamento, mensagem de lista vazia, confirmação antes de excluir.
- O formulário não valida no cliente, por decisão de design: mostra as mensagens da API junto aos campos. É coerente com a spec, mas custa uma ida ao servidor para erros triviais.
- O bug de corretude da seção 3 está nesta camada, mas foi pontuado lá.

**Superpowers: 7**

- Validação no cliente (título e vencimento), com feedback imediato, mais as mensagens da API. Prioridade com padrão `MEDIA`.
- Lista em tabela com coluna de conclusão, mas os botões "Editar", "Concluir" e "Excluir" se repetem em cada linha sem nome acessível que identifique a tarefa. Faltam `aria-invalid` e `aria-describedby`.
- Erros de campos que o formulário não conhece aparecem como erro geral, com teste para isso.

**Híbrido: 8**

- O tratamento de estados é o mais completo das três: controles desabilitados durante a requisição, proteção contra duplo clique, aviso e recarga em conflito, remoção da tarefa com aviso quando ela foi excluída em outro lugar, mensagem genérica com o estado mantido em falha de rede e um botão "Tentar novamente" quando a lista não carrega. Uma tarefa criada fora do filtro ativo faz a lista voltar para "Todas", para que ela apareça.
- Boa acessibilidade no formulário e no diálogo: `aria-invalid` e `aria-describedby` em cada campo, `maxLength` igual aos limites da API, diálogo de exclusão com `aria-modal`, foco inicial em "Cancelar", Tab preso entre os botões, Esc para cancelar e foco devolvido a quem abriu. Filtros com `aria-pressed` e avisos com `role="alert"`, com uma chave que muda a cada aviso para que o leitor de tela anuncie de novo.
- Perde por três pontos. Os botões "Concluir", "Editar" e "Excluir" se repetem em cada linha sem nome acessível que identifique a tarefa (o mesmo problema do Superpowers; o OpenSpec usa `aria-label` com o título). Não há validação no cliente, como no OpenSpec. E a lista não mostra a descrição nem a data de conclusão.
- Atrito pequeno, já previsto na spec: se a tarefa foi concluída em outra aba enquanto o formulário estava aberto, o usuário precisa salvar duas vezes, mesmo que a outra ação não tenha mexido em nenhum campo do formulário.

## 10. Rastreabilidade do processo

**OpenSpec: 6**

- Os artefatos de spec são excelentes: proposta → design → deltas → tarefas → arquivamento → spec viva.
- O histórico do git não acompanha: a implementação inteira está num único commit, *"Implementacao completa"* (71 arquivos, ~6.800 linhas). Não dá para revisar por partes, bisectar ou ver em que ordem as decisões foram tomadas. O `tasks.md` marca as tarefas como feitas, mas sem vínculo com commits.

**Superpowers: 9**

- 18 commits atômicos em Conventional Commits, em português, um por tarefa do plano (`feat: entidade Tarefa…`, `fix: formulário exibe erros…`, `docs: corrige os blocos de comandos do README`). Os commits de `fix` mostram a revisão funcionando durante o desenvolvimento.
- Spec → plano → commits têm correspondência direta.
- Perde um ponto pelo volume do plano, que tende a envelhecer.

**Híbrido: 9**

- A cadeia completa existe e é verificável: cenário da spec → tarefa do `tasks.md`, que cita os cenários → commit da tarefa, que marca o checkbox no mesmo commit. As 31 tarefas têm commit próprio em Conventional Commits, em português e com escopo (`feat(tarefa): edita tarefas com concorrência otimista`, `feat(frontend): trata conflito de versão na edição`).
- A revisão por seção deixa rastro: um commit `fix: aplica a revisão de código da seção N` para as seções 1, 3, 4 e 5, e um `fix: aplica a revisão da branch inteira`. Quando a revisão encontrou um problema de spec, a spec foi corrigida antes, em commit próprio (`docs(openspec): recebe dataVencimento como texto para agregar erros`, `docs(openspec): torna prazo um módulo folha e alinha D7 ao código`), como manda o `CLAUDE.md`.
- O arquivamento (`41d676a`) consolida os deltas em specs vivas, então a spec continua válida depois da entrega, ao contrário do plano do Superpowers.
- Perde um ponto pela granularidade das correções: cada rodada de revisão vira um único commit (o da seção 5 tem 8 arquivos e ~400 linhas alteradas), sem ligação entre cada achado e a sua correção. A seção 2 não tem commit de revisão, e o histórico não diz se ela foi revisada sem achados. Os três primeiros commits (configuração e planejamento) não seguem Conventional Commits.

## 11. Operação e execução local

**OpenSpec: 9**

- O README cobre os problemas reais do ambiente (Docker Desktop parado, Avast/Web Shield interceptando TLS, porta ocupada) com comandos para PowerShell.
- Fuso configurável (`app.fuso`), `healthcheck` no Postgres, nome de container explícito, instruções para derrubar o banco e apagar o volume.

**Superpowers: 8**

- Variáveis de ambiente para o banco, `healthcheck`, troubleshooting curto, mas suficiente.
- Comandos de teste em subshell (`(cd backend && ./mvnw verify)`), que funcionam em sequência a partir da raiz.

**Híbrido: 9**

- `.env.example` para o banco, `docker compose up -d --wait` com `healthcheck`, nome de projeto e de container explícitos, e propriedades opcionais (fuso e origens de CORS) configuráveis por variável de ambiente.
- Comandos de teste e verificação com `Push-Location`/`Pop-Location`, que rodam em sequência a partir da raiz, incluindo lint, build e `npm audit`. O README explica como rodar só os unitários sem Docker.
- Troubleshooting para TLS no Maven e no npm e para Docker parado, além do roteiro de verificação manual.
- Faltam instruções para derrubar o banco e apagar o volume, e para porta 5432 ocupada. Sem valor padrão, as três variáveis `DB_*` precisam ser definidas em cada terminal que sobe o backend: é mais seguro, mas dá mais trabalho.

---

## Recomendações

**OpenSpec**

1. Corrigir a reversão de status em `App.tsx` (atualizar `emEdicao` após alternar o status) e adicionar o teste de regressão.
2. Permitir override das credenciais por variável de ambiente e documentá-las.
3. Mover a ordenação para o banco (`ORDER BY` com `CASE` para a prioridade) antes de pensar em paginação.
4. Em próximas mudanças, fazer commits por tarefa do `tasks.md`.

**Superpowers**

1. Atualizar `vite` (≥ 6.4.4) e `vitest`/`tinypool` para eliminar as vulnerabilidades do `npm audit`.
2. Adicionar `spring.jackson.deserialization.fail-on-numbers-for-enums: true`.
3. Fazer o serviço devolver DTOs e calcular "hoje" num único lugar.
4. Trocar o `findBy…` do score por consultas `count`.
5. Configurar um linter no frontend e adicionar `aria-describedby` e nomes acessíveis nos botões da tabela.

**Híbrido**

1. Dar nome acessível aos botões de cada linha, por exemplo `aria-label="Concluir Pagar conta"`.
2. Acrescentar ao README a tabela de endpoints, os códigos de status e o formato dos erros, e instruções para derrubar o banco.
3. Versionar a verificação no navegador da tarefa 6.1 como script Playwright, para que ela possa ser repetida.
4. Mostrar a descrição (e, se fizer sentido, a data de conclusão) na lista.
5. Em próximas rodadas de revisão, fazer um commit por achado, ou citar os achados na mensagem do commit de correção.

**OpenSpec e Superpowers**

- Limitar o tamanho de `descricao` (DTO e coluna), publicar o Postgres só em `127.0.0.1` e considerar `@Version` na entidade.

## Conclusão

| | OpenSpec | Superpowers | Híbrido |
| --- | --- | --- | --- |
| Média | 8,2 | 7,8 | **9,0** |
| Ponto mais forte | Specs vivas com cenários e código e UX mais refinados | Processo disciplinado (TDD, commits atômicos) e menos bugs | Spec viva ligada a tarefas, testes e commits; nenhum bug encontrado; tratamento completo de ações concorrentes |
| Ponto mais fraco | Bug de reversão de status na UI e implementação num único commit | Dependências vulneráveis e frontend menos acessível | Botões da lista sem nome acessível, README sem descrição da API e custo maior de processo |

Entre OpenSpec e Superpowers a diferença é pequena e vem de fatores diferentes. O OpenSpec produziu um **produto** ligeiramente melhor e uma documentação de requisitos mais útil a longo prazo, porque as specs continuam válidas depois da entrega. O Superpowers produziu um **processo** mais auditável, e esse processo pegou um bug que escapou ao OpenSpec.

O Híbrido confirma a hipótese da primeira rodada: a spec viva do OpenSpec com a disciplina de TDD e de commits do Superpowers deu o melhor resultado nos dois eixos. Os cenários de interação ("editar enquanto conclui", duplo clique, tarefa excluída em outra aba) viraram testes antes do código, e por isso a classe de bug que derrubou o OpenSpec não apareceu. O `tasks.md` cumpriu o papel do plano do Superpowers sem duplicar o código, e o arquivamento deixou specs vivas no lugar de um plano que envelhece.

A comparação tem dois limites. Primeiro, o Híbrido começou com as lições desta revisão escritas como restrições no `openspec/config.yaml`, então parte da vantagem em segurança e corretude não pode ser atribuída só ao fluxo. Uma comparação justa pediria uma nova rodada dos frameworks puros com as mesmas restrições. Segundo, o resultado custou mais: 44 commits, quatro rodadas de correção pós-revisão de seção mais a da branch inteira, e ~3.400 linhas de teste para ~2.300 de código. Para um MVP desse porte o custo se paga na confiança. Num time, vale ajustar a granularidade, por exemplo revisando por seção só as seções com regra de negócio.
