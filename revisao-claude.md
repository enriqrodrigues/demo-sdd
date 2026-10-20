# Revisão de código: OpenSpec × Superpowers × Híbrido × OpenSpec ajustado

Revisão das quatro implementações do **To-Do Inteligente (MVP)** descrito em [`escopo_inicial.md`](escopo_inicial.md):

| Branch | Framework de SDD | Commits | Código (main + testes) |
| --- | --- | --- | --- |
| `release/demo-openspec` | OpenSpec | 4 | ~2.300 linhas de código + ~1.870 de testes |
| `release/demo-superpowers` | Superpowers | 18 | ~1.800 linhas de código + ~1.300 de testes |
| `release/demo-combinado-openspec-superpowers` | Híbrido: OpenSpec no planejamento, Superpowers na execução | 44 | ~2.300 linhas de código + ~3.400 de testes |
| `release/demo-openspec-ajustado` | OpenSpec ajustado: só OpenSpec, com `config.yaml` mais rígido e fluxo de verificação no `CLAUDE.md` | 30 | ~1.550 linhas de código + ~1.650 de testes |

Revisor: Claude (Opus 5.5). OpenSpec e Superpowers revisados em 18/10/2026; Híbrido e OpenSpec ajustado em 19/10/2026, com os mesmos critérios.

**Como funciona o modo híbrido.** O `CLAUDE.md` e o `openspec/config.yaml` da branch dividem o trabalho: o OpenSpec cuida do planejamento (`/opsx:explore`, `/opsx:propose`) e do acompanhamento das tarefas (`tasks.md`, `/opsx:archive`). O Superpowers entra na execução: TDD em cada tarefa, um commit por tarefa com o checkbox marcado no mesmo commit, revisão de código ao fim de cada seção do `tasks.md` e verificação antes do arquivamento. Problemas de spec achados na revisão passam por `/opsx:update` antes da correção.

**Como funciona o OpenSpec ajustado.** Usa só o OpenSpec, sem nenhuma skill do Superpowers. O `openspec/config.yaml` leva as mesmas restrições de projeto do híbrido e acrescenta regras às operações: no `apply`, teste antes do código, um commit por tarefa com o checkbox marcado e no máximo 12 tarefas; nas specs, cenários de interação só entre ações sobre a mesma entidade na mesma tela, sem duplo clique, falha de rede ou concorrência entre abas. O `CLAUDE.md` fixa um fluxo de sete passos: explore, propose, apply, verificação final (suítes, build, lint, `npm audit`), `/opsx:verify`, `/code-review` da branch inteira em esforço `medium`, um commit de correção por achado e só então o archive.

> **Ressalva sobre a comparação.** O híbrido e o OpenSpec ajustado não partiram do zero como os dois primeiros. O `openspec/config.yaml` deles traz, como restrições de projeto, lições da primeira rodada desta revisão: credenciais por variável de ambiente, limite nos campos de texto, enums sem valores numéricos, Postgres só em `127.0.0.1` e cenários de interação entre ações sobre a mesma entidade, como "editar enquanto conclui". Parte da vantagem nos critérios 3, 7 e 9 vem dessas regras, e não só do processo. Ainda assim, o processo foi o que transformou as regras em cenários, testes e código. O OpenSpec ajustado também se beneficiou das recomendações feitas ao híbrido (nomes acessíveis nos botões, API no README, um commit por achado), o que pesa nos critérios 1, 9 e 10.

## Como a revisão foi feita

- Leitura integral do backend (Java/Spring Boot), do frontend (React/TypeScript), das migrations, do `docker-compose.yml`, dos READMEs e dos documentos de especificação de cada branch.
- Execução local das suítes, com Docker Desktop ativo:

  | Verificação | OpenSpec | Superpowers | Híbrido | OpenSpec ajustado |
  | --- | --- | --- | --- | --- |
  | `./mvnw verify` (unitários + Testcontainers) | ✅ 81 testes, 0 falhas | ✅ 58 testes, 0 falhas | ✅ 125 testes, 0 falhas | ✅ 70 testes, 0 falhas |
  | `npm test` (Vitest) | ✅ 38 testes | ✅ 38 testes | ✅ 61 testes | ✅ 27 testes |
  | `tsc` + `npm run build` | ✅ | ✅ | ✅ | ✅ |
  | Lint | ✅ `oxlint` sem avisos | não há lint configurado | ✅ `eslint` sem avisos | ✅ `oxlint` sem avisos |
  | `npm audit` | ✅ 0 vulnerabilidades | ❌ 4 (2 críticas, 1 alta, 1 moderada) | ✅ 0 vulnerabilidades | ✅ 0 vulnerabilidades |

- Testes de reprodução escritos à parte (não versionados) confirmaram o bug de frontend do OpenSpec e o defeito de janela curta do OpenSpec ajustado, ambos descritos em [Corretude](#3-corretude).
- No híbrido e no OpenSpec ajustado, o histórico foi conferido commit a commit: se cada tarefa tem commit próprio que marca o checkbox no `tasks.md` e, no ajustado, se cada commit de correção cita o achado que corrige.

## Resumo das notas

| # | Critério | OpenSpec | Superpowers | Híbrido | OpenSpec ajustado |
| --- | --- | :---: | :---: | :---: | :---: |
| 1 | Documentação | **9** | 8 | **9** | **9** |
| 2 | Aderência aos requisitos | **9** | **9** | **9** | **9** |
| 3 | Corretude (bugs encontrados) | 7 | 9 | **10** | 9 |
| 4 | Arquitetura e design | **9** | 8 | **9** | **9** |
| 5 | Qualidade e legibilidade do código | **9** | 8 | **9** | **9** |
| 6 | Testes | 8 | 8 | **9** | 8 |
| 7 | Segurança | 7 | 6 | **9** | 8 |
| 8 | Dependências e build | 8 | 6 | **9** | **9** |
| 9 | Frontend: UX e acessibilidade | **9** | 7 | 8 | 8 |
| 10 | Rastreabilidade do processo (spec → código → commits) | 6 | 9 | 9 | **10** |
| 11 | Operação e execução local | **9** | 8 | **9** | **9** |
| | **Média** | **8,2** | **7,8** | **9,0** | **8,8** |

As quatro implementações são boas e funcionam: todos os testes passam e os três requisitos (RF01 a RF03) estão cobertos. O OpenSpec entregou código, documentação e acessibilidade um pouco melhores, mas com um bug real de interface e um histórico de commits que não deixa ver como o código chegou lá. O Superpowers entregou um processo exemplar (TDD com commits atômicos) e menos bugs, mas com dependências desatualizadas e vulneráveis e um frontend menos cuidadoso. O Híbrido juntou as duas forças: specs vivas com cenários, um commit por tarefa ligado ao `tasks.md` e nenhum bug encontrado. Ele custou mais (44 commits, revisões de código por seção, quatro delas com commit de correção, e uma da branch inteira) e herdou lições da revisão anterior (ver a ressalva acima). O OpenSpec ajustado chegou perto do híbrido só com o OpenSpec: 12 tarefas com um commit cada, `/opsx:verify` e `/code-review` no fim e um commit por achado, com o código mais enxuto das quatro. Ficou um defeito de janela curta na interface (concluir enquanto o salvar está em andamento) e uma proteção menor contra ações concorrentes.

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

**OpenSpec ajustado: 9**

- O README é o mais equilibrado das quatro: tabela de módulos com versões, pré-requisitos, tabela de variáveis, como subir e parar o banco (com `-v` para apagar os dados), tabela da API com códigos de status e o formato dos erros (Problem Details com `erros: [{ campo, mensagem }]`), comandos de teste, um roteiro de verificação manual em nove passos e problemas conhecidos (`JAVA_HOME` antigo, PKIX no Maven, Docker parado).
- Specs vivas em quatro capacidades (`gestao-tarefas`, `situacao-prazo`, `produtividade-semanal`, `tela-tarefas`): 21 requisitos e 61 cenários em 335 linhas. São menores que as do híbrido porque a regra do `config.yaml` limitou os cenários de interação a ações sobre a mesma tarefa na mesma tela.
- O design (70 linhas) registra cada decisão com a alternativa descartada, por exemplo `ddl-auto=update` contra Flyway e prioridade numérica contra `CASE` na ordenação. Depois do `/code-review`, três commits atualizaram o design para descrever o código corrigido, então o artefato arquivado bate com a implementação.
- O `tasks.md` (32 linhas, 12 tarefas) cita, para cada tarefa, os cenários (`spec › requisito › cenário`) e o teste que a verifica.
- Perde um ponto pelo detalhe: o README não tem exemplo de requisição e os comandos estão só na sintaxe de shell POSIX, com uma nota para o `mvnw.cmd`; e o design não numera as decisões, o que impede citá-las no código como o híbrido faz.

## 2. Aderência aos requisitos

**OpenSpec: 9 · Superpowers: 9 · Híbrido: 9 · OpenSpec ajustado: 9**

As quatro atendem a RF01, RF02 e RF03 com decisões equivalentes. Nas três primeiras:

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

O OpenSpec ajustado segue as mesmas regras, com estas diferenças:

- O `id` é `bigint` gerado pelo banco (`identity`), e não UUID. O escopo aceita os dois.
- A conclusão é uma data (`data_conclusao date`), como no OpenSpec, e o percentual do score tem uma casa decimal, e não duas.
- Concluir e reabrir são feitos pelo `PUT` de atualização completa, com `status` obrigatório; o `POST` sempre cria `PENDENTE`. A listagem filtra por situação de prazo e por prioridade e ordena por vencimento, prioridade decrescente e `id`.
- É a única das quatro sem constraint no banco ligando `status` e `data_conclusao`. Nenhum caminho da API grava os dois incoerentes, mas o banco não protege contra isso.
- Como no híbrido, a lista não mostra a descrição nem a data de conclusão.

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

**OpenSpec ajustado: 9**

- **Defeito confirmado, de janela curta: concluir pela lista enquanto o salvar da edição está em andamento desfaz uma das duas ações.** Em `frontend/src/PaginaTarefas.tsx`, `salvar()` lê o status antes de enviar o `PUT`, e `alternarStatus()` não espera um salvar pendente, não fica desabilitado para a tarefa em edição e envia os campos da linha, ainda com o título antigo. Se a conclusão chega antes ao servidor, o `PUT` do salvar a reverte e a tarefa volta a `PENDENTE`; se chega depois, reverte o título. Para reproduzir: abra a edição, mude o título, clique em **Salvar** e, antes da resposta, marque o checkbox da mesma tarefa. Um teste de reprodução com Vitest, com o `PUT` do salvar retido, confirmou o primeiro caso. A janela é a duração da requisição, de poucos milissegundos em ambiente local, e o cenário da spec ("conclui e depois salva") funciona. A regra do `config.yaml` pede cenários de interação entre ações sobre a mesma tarefa, mas a spec só cobriu essa ordem.
- O caso que derrubou o OpenSpec original foi pego pelo `/code-review` antes do arquivamento (achado 1, commit `6cd966c`). A primeira versão já tinha o cenário "editar enquanto conclui" com teste, mas ainda revertia a conclusão quando ela estava em andamento ou quando a linha não tinha recarregado. A correção espera a conclusão pendente e usa o último status devolvido pela API, com dois testes de regressão.
- O backend não tem defeitos encontrados. A regra da `dataConclusao` (concluir grava hoje, reabrir apaga, manter o status preserva a data) está testada, inclusive "reconcluir em outro dia". Enums numéricos são rejeitados também como texto (`"prioridade": "1"`), datas fora do formato ou impossíveis (`2026-13-40`, `22/10/2026`) voltam com erro no campo, e o título perde espaços Unicode das pontas (NBSP, BOM) no mesmo critério do `trim()` do JavaScript.
- Observação, sem desconto: como no híbrido, `"titulo": 123` vira `"123"`.

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

**OpenSpec ajustado: 9**

- Pacotes por capacidade (`tarefa`, `produtividade`) mais `config` e `erro`. A situação de prazo fica dentro de `tarefa`: o enum `SituacaoPrazo` tem a função pura `calcular` e, ao lado, o predicado equivalente para o banco, e um teste parametrizado confere os quatro valores do filtro contra o cálculo.
- O serviço devolve DTOs (`TarefaResposta`), e o controller fica fino. "Hoje" tem uma única fonte, `Calendario`, sobre um `Clock` no fuso de `app.fuso-horario`.
- DTOs separados para criação (`TarefaEntrada`, sem `status`) e atualização (`TarefaAtualizacao`, com `status` obrigatório), como no OpenSpec, com as regras de título e descrição em anotações compostas (`@TituloTarefa`, `@DescricaoTarefa`).
- O score é o mais bem resolvido das quatro: uma única consulta conta as concluídas e, com `sum(case ...)`, as concluídas no prazo, num repositório próprio e de pacote do módulo de produtividade.
- Ordenação e filtros no banco por `Specification`, com a prioridade ordenada por `CASE`.
- Perde um ponto por dois pontos. Concluir pelo `PUT` completo obriga o cliente a reenviar título, descrição e vencimento só para mudar o status, e é daí que vem o defeito da seção 3; endpoints de concluir e reabrir, como no híbrido, evitariam essa classe de problema. E falta a constraint de coerência entre `status` e `data_conclusao`. Os predicados usam nomes de atributo em texto (`"dataVencimento"`), sem metamodelo, o que só falha em tempo de execução, mas os testes de integração cobrem.

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

**OpenSpec ajustado: 9**

- O código mais enxuto das quatro (~810 linhas de Java e ~740 de TypeScript), sem perder clareza. Nomes em português e consistentes (`Calendario`, `TarefaConsultas`, `ContagemSemanal`, `Textos.aparar`). Mensagens de validação são frases completas ("O título é obrigatório.").
- O tratamento de erros traduz também os erros padrão do Spring (rota inexistente, 405, 406, 415, parâmetro de tipo inválido), corrigido a partir de um aviso do `/opsx:verify`.
- Comentários explicam o porquê (`// Enums só como texto: rejeita 1 ou "1" no lugar de "MEDIA"`, o critério do `trim()` em `Textos`, o motivo de não usar `Date` em `formatarData`).
- Frontend com `oxlint` (regra de hooks como erro), TypeScript 7 no modo estrito padrão, `noUnusedLocals` e `noUnusedParameters`. O cliente HTTP é curto e converte Problem Details num `ErroApi` com o status.
- Pontos menores: as mensagens de `@NotNull` se repetem nos dois records; e o par `ultimoStatus` + `alternanciasPendentes` em `PaginaTarefas.tsx`, com refs e promessas, é correto para o cenário especificado, mas exige atenção para ler e ainda deixa o caso da seção 3.

## 6. Testes

**OpenSpec: 8 · Superpowers: 8 · Híbrido: 9 · OpenSpec ajustado: 8**

| | OpenSpec | Superpowers | Híbrido | OpenSpec ajustado |
| --- | --- | --- | --- | --- |
| Backend | 81 (75 `@Test` + 3 `@ParameterizedTest`) | 58 | 125 | 70 (6 unitários + 64 de integração) |
| Frontend | 38 | 38 | 61 | 27 |
| Tipos | unitários de domínio, `@WebMvcTest` só da validação, repositório, serviço, API completa com Testcontainers | unitários de domínio, repositório, migração, relógio, API completa e atualização com Testcontainers | unitários de domínio e de dependência entre módulos (Surefire, sem Docker); migração, CORS, erros do framework, fuso e API completa com Testcontainers (Failsafe); componentes com MSW | unitários da situação de prazo e do fuso (Surefire); migração, validação, erros do framework, listagem e score com Testcontainers (Failsafe); componentes com `fetch` simulado |

- **OpenSpec:** cobertura de backend mais ampla, com uma fatia `@WebMvcTest` que roda sem Docker (e um README que explica como rodar só esses testes). Os testes parametrizados cobrem as regras de prazo e score. Perde ponto porque a suíte do frontend não pegou o bug de reversão de status, um fluxo que combina duas ações comuns.
- **Superpowers:** a suíte foi construída por TDD, como mostra o histórico, e inclui testes de regressão para bugs achados durante o desenvolvimento, testes de CORS (origem permitida e recusada), um teste de migração Flyway e um de 100 caracteres acentuados no título. Tem menos casos no backend e não testa enum numérico.
- **Híbrido:** a maior suíte das três, com um teste por cenário da spec, citado no `tasks.md`. Os testes de integração ficam separados dos unitários (`*IT` no Failsafe), então `mvnw test` roda sem Docker. O frontend usa MSW com uma API simulada e cobre as interações: duplo clique em salvar e em concluir (contando as requisições recebidas), conflito 409 com segundo salvar, tarefa removida durante uma ação, falha de rede e cancelamento da exclusão sem `DELETE`. Há também um relógio de teste ajustável validado por teste próprio.
- O Híbrido perde um ponto porque a verificação no navegador (tarefa 6.1) não deixou artefato: o commit `df33e9b` só marca o checkbox, sem script nem registro do roteiro executado. Além disso, o teste de dependência entre módulos procura `import` no texto dos arquivos, mais frágil que uma ferramenta como ArchUnit.
- **OpenSpec ajustado:** cada cenário da spec tem teste, citado no `tasks.md`, e os nomes dos testes repetem os cenários (`reconcluirEmOutroDia`, `hojeEhDomingo`, `tituloSoComEspacosUnicode`). Um teste parametrizado confere os quatro valores do filtro de situação contra o cálculo em memória, e um relógio ajustável muda o dia dentro do teste. O `/code-review` acrescentou testes de regressão para cada achado (de 65 para 70 no backend e de 19 para 27 no frontend). Perde um ponto pela forma da suíte: só 6 testes rodam sem Docker, e regras como o arredondamento e os limites da semana são testadas só por integração; a suíte do frontend é a menor das quatro e não pegou o caso da seção 3; e a verificação manual da tarefa 4.1 ficou registrada só na mensagem do commit ("9/9 passos"), sem script que a repita.

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

**OpenSpec ajustado: 8**

- Resolve dois dos quatro problemas comuns: `descricao` limitada a 1.000 caracteres no DTO e na coluna (`varchar(1000)`), e Postgres publicado só em `127.0.0.1`.
- Credenciais só por variável de ambiente, sem valor padrão (`${DB_SENHA}`), lidas do ambiente ou do `.env` da raiz, que fica fora do versionamento. O `docker-compose.yml` falha com mensagem clara se uma variável faltar (`${DB_SENHA:?defina DB_SENHA no .env}`).
- Enums numéricos rejeitados, inclusive como texto. Sem CORS: o frontend usa o proxy do Vite.
- `npm audit` sem vulnerabilidades e Spring Boot 4.1.1.
- Perde em relação ao híbrido: não há controle de concorrência (`@Version`), então dois `PUT` sobre a mesma tarefa resultam em *last write wins*, o que também está na raiz do defeito da seção 3. A regra do `config.yaml` tirou a concorrência entre abas do MVP, mas a proteção valeria também para a mesma tela. O backend escuta em todas as interfaces (não há `server.address`), e a listagem segue sem paginação, fora do escopo declarado.

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

**OpenSpec ajustado: 9**

- O ferramental mais atual das quatro: Java 25, Spring Boot 4.1.1, React 19, Vite 8, TypeScript 7, Vitest 5, `oxlint` e PostgreSQL 17. O design registra o risco de usar o Spring Boot 4 e o caminho de volta para o 3.5.
- Faixas `^` com `package-lock.json` versionado, `build` com `tsc -b`, scripts de lint e de teste.
- `.gitattributes` na raiz com `backend/mvnw text eol=lf` e `*.cmd text eol=crlf`.
- Separação Surefire/Failsafe pelo sufixo `*IT`, que permite rodar os unitários sem Docker.
- Perde um ponto por detalhes: não há `engines` no `package.json` (o Node 24 só aparece no README), e o `package-lock.json` ficou com o nome antigo do pacote até o commit final, feito à mão.

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

**OpenSpec ajustado: 8**

- Nome acessível em cada controle da linha (`Concluir Pagar conta`, `Editar Pagar conta`, `Excluir Pagar conta`), a recomendação que o híbrido não cumpriu.
- Validação no cliente com as mesmas regras e mensagens da API, mais os erros da API junto ao campo por `aria-describedby`. Erros de campos que o formulário não exibe aparecem como mensagem geral, e falhas de rede mostram texto em português, não o "Failed to fetch" do navegador.
- Filtros por situação e por prioridade, confirmação antes de excluir, score com travessão quando não há conclusões. A lista e o score carregam em separado, então a falha do score não esconde a lista.
- Perde por três pontos. Faltam `aria-invalid` nos campos e `maxLength` igual aos limites da API. Não há estado de carregamento: a lista e o score ficam vazios até a resposta, e uma lista que falhou ao carregar não tem botão para tentar de novo. E os controles não ficam desabilitados durante a requisição, o que deixa aberta a janela do defeito da seção 3.
- Como no híbrido, a lista não mostra a descrição nem a data de conclusão, e uma tarefa criada fora do filtro ativo não aparece, sem aviso.

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

**OpenSpec ajustado: 10**

- A cadeia completa existe e é verificável, como no híbrido: cenário da spec → tarefa do `tasks.md` → commit da tarefa, que marca o checkbox no mesmo commit. As 12 tarefas têm commit próprio em Conventional Commits, em português e com o módulo como escopo (`feat(tarefa): lista tarefas com filtros e ordenação no banco`).
- Resolve o ponto fraco do híbrido: cada achado da verificação virou um commit próprio, que cita o achado no corpo ("Achado 1 do /code-review: ...", "Aviso 1 do /opsx:verify: ..."). São nove commits de código e três de design, todos antes do arquivamento. O achado 9 do `/code-review` (os commits iniciais fora do padrão) ficou sem correção por acordo, e isso está registrado.
- O design foi corrigido depois das correções de código (`fix(openspec): alinha o design às correções do code-review`), então o artefato arquivado descreve o código entregue. O arquivamento consolidou os deltas em specs vivas.
- O commit da tarefa 4.1 registra o resultado das suítes e do roteiro manual ("9/9 passos").
- Os pontos fora do padrão não vêm do fluxo: os três commits iniciais de configuração e o commit final "Fim", que só corrige o nome no `package-lock.json`, foram feitos à mão.

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

**OpenSpec ajustado: 9**

- Um único `.env` na raiz serve ao banco (`docker compose --env-file .env`) e ao backend (`spring.config.import=optional:file:../.env[.properties]`). Resolve o atrito do híbrido: nada precisa ser definido em cada terminal, e a senha continua fora do código e do repositório.
- `healthcheck` no Postgres, nome de projeto e de container explícitos, instruções para parar o banco e apagar o volume, fuso configurável (`app.fuso-horario`).
- Troubleshooting para `JAVA_HOME` apontando para JDK antigo, PKIX no Maven e Docker parado, além do roteiro manual.
- Faltam o `--wait` no `docker compose up` (o backend pode subir antes de o banco estar pronto), troubleshooting de TLS no npm e de porta 5432 ocupada, e comandos em PowerShell para quem usa Windows. O `spring.config.import` com caminho relativo exige subir o backend de dentro de `backend/`.

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

**OpenSpec ajustado**

1. Fechar a janela de "concluir enquanto salva": desabilitar o checkbox e o excluir da tarefa enquanto o salvar dela está em andamento, ou fazer `alternarStatus()` esperar o salvar pendente. Acrescentar o cenário na ordem inversa à spec `tela-tarefas` e o teste de regressão.
2. Trocar a mudança de status pelo `PUT` completo por `POST /api/tarefas/{id}/concluir` e `/reabrir`, para que concluir não reenvie título e descrição, e considerar `@Version`.
3. Criar a constraint de coerência entre `status` e `data_conclusao` numa migração `V2`.
4. No frontend, acrescentar `aria-invalid`, `maxLength`, estado de carregamento e um "Tentar novamente" quando a lista falha.
5. Versionar o roteiro da tarefa 4.1 como script Playwright e acrescentar `--wait` ao `docker compose up` do README.

**OpenSpec e Superpowers**

- Limitar o tamanho de `descricao` (DTO e coluna), publicar o Postgres só em `127.0.0.1` e considerar `@Version` na entidade.

## Conclusão

| | OpenSpec | Superpowers | Híbrido | OpenSpec ajustado |
| --- | --- | --- | --- | --- |
| Média | 8,2 | 7,8 | **9,0** | 8,8 |
| Ponto mais forte | Specs vivas com cenários e código e UX mais refinados | Processo disciplinado (TDD, commits atômicos) e menos bugs | Spec viva ligada a tarefas, testes e commits; nenhum bug encontrado; tratamento completo de ações concorrentes | Rastreabilidade completa até o achado de revisão, código mais enxuto, ferramental atual e README com a API |
| Ponto mais fraco | Bug de reversão de status na UI e implementação num único commit | Dependências vulneráveis e frontend menos acessível | Botões da lista sem nome acessível, README sem descrição da API e custo maior de processo | Defeito de janela curta ao concluir durante o salvar, sem `@Version` nem endpoints próprios de status, suíte de frontend menor |

Entre OpenSpec e Superpowers a diferença é pequena e vem de fatores diferentes. O OpenSpec produziu um **produto** ligeiramente melhor e uma documentação de requisitos mais útil a longo prazo, porque as specs continuam válidas depois da entrega. O Superpowers produziu um **processo** mais auditável, e esse processo pegou um bug que escapou ao OpenSpec.

O Híbrido confirma a hipótese da primeira rodada: a spec viva do OpenSpec com a disciplina de TDD e de commits do Superpowers deu o melhor resultado nos dois eixos. Os cenários de interação ("editar enquanto conclui", duplo clique, tarefa excluída em outra aba) viraram testes antes do código, e por isso a classe de bug que derrubou o OpenSpec não apareceu. O `tasks.md` cumpriu o papel do plano do Superpowers sem duplicar o código, e o arquivamento deixou specs vivas no lugar de um plano que envelhece.

A comparação tem dois limites. Primeiro, o Híbrido começou com as lições desta revisão escritas como restrições no `openspec/config.yaml`, então parte da vantagem em segurança e corretude não pode ser atribuída só ao fluxo. Uma comparação justa pediria uma nova rodada dos frameworks puros com as mesmas restrições. Segundo, o resultado custou mais: 44 commits, quatro rodadas de correção pós-revisão de seção mais a da branch inteira, e ~3.400 linhas de teste para ~2.300 de código. Para um MVP desse porte o custo se paga na confiança. Num time, vale ajustar a granularidade, por exemplo revisando por seção só as seções com regra de negócio.

O OpenSpec ajustado testou se o OpenSpec sozinho chega lá quando o `config.yaml` e o `CLAUDE.md` impõem a disciplina que no híbrido vinha do Superpowers. Chegou perto, com 8,8 contra 9,0. As regras de `apply` (teste antes, um commit por tarefa) e o fluxo final (`/opsx:verify`, `/code-review`, um commit por achado) deram a melhor rastreabilidade das quatro e pegaram nove problemas antes do arquivamento, inclusive a variante de "editar enquanto conclui" que derrubou o primeiro OpenSpec. O custo foi menor que o do híbrido: 30 commits, uma revisão em vez de cinco e ~1.650 linhas de teste para ~1.550 de código.

A diferença para o híbrido está no que a regra de specs deixou de fora. Ao limitar os cenários de interação a ações sobre a mesma tarefa na mesma tela e excluir concorrência, a spec cobriu "conclui e depois salva", mas não "salva e conclui antes da resposta", e o design não trouxe as proteções do híbrido (controles desabilitados, endpoints de status, `@Version`) que fecham essa classe de problema sem precisar de um cenário para cada ordem. A lição para a próxima rodada: em vez de limitar a lista de cenários, pedir no design uma decisão explícita sobre ações concorrentes na mesma entidade. Valem para este resultado as mesmas ressalvas do híbrido: ele herdou as lições das rodadas anteriores, e quem revisou é o mesmo modelo que implementou.
