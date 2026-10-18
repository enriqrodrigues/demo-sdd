# Revisão de código: OpenSpec × Superpowers

Revisão das duas implementações do **To-Do Inteligente (MVP)** descrito em [`escopo_inicial.md`](escopo_inicial.md):

| Branch | Framework de SDD | Commits | Código (main + testes) |
| --- | --- | --- | --- |
| `release/demo-openspec` | OpenSpec | 4 | ~2.300 linhas de código + ~1.870 de testes |
| `release/demo-superpowers` | Superpowers | 18 | ~1.800 linhas de código + ~1.300 de testes |

Revisor: Claude (Opus 5.5), em 18/10/2026.

## Como a revisão foi feita

- Leitura integral do backend (Java/Spring Boot), do frontend (React/TypeScript), das migrations, do `docker-compose.yml`, dos READMEs e dos documentos de especificação de cada branch.
- Execução local das suítes, com Docker Desktop ativo:

  | Verificação | OpenSpec | Superpowers |
  | --- | --- | --- |
  | `./mvnw verify` (unitários + Testcontainers) | ✅ 81 testes, 0 falhas | ✅ 58 testes, 0 falhas |
  | `npm test` (Vitest) | ✅ 38 testes | ✅ 38 testes |
  | `tsc` + `npm run build` | ✅ | ✅ |
  | Lint | ✅ `oxlint` sem avisos | não há lint configurado |
  | `npm audit` | ✅ 0 vulnerabilidades | ❌ 4 (2 críticas, 1 alta, 1 moderada) |

- Um teste de reprodução escrito à parte (não versionado) confirmou o bug de frontend descrito em [Corretude](#3-corretude).

## Resumo das notas

| # | Critério | OpenSpec | Superpowers |
| --- | --- | :---: | :---: |
| 1 | Documentação | **9** | 8 |
| 2 | Aderência aos requisitos | 9 | 9 |
| 3 | Corretude (bugs encontrados) | 7 | **9** |
| 4 | Arquitetura e design | **9** | 8 |
| 5 | Qualidade e legibilidade do código | **9** | 8 |
| 6 | Testes | 8 | 8 |
| 7 | Segurança | **7** | 6 |
| 8 | Dependências e build | **8** | 6 |
| 9 | Frontend: UX e acessibilidade | **9** | 7 |
| 10 | Rastreabilidade do processo (spec → código → commits) | 6 | **9** |
| 11 | Operação e execução local | **9** | 8 |
| | **Média** | **8,2** | **7,8** |

As duas implementações são boas e funcionam: todos os testes passam e os três requisitos (RF01 a RF03) estão cobertos. O OpenSpec entregou código, documentação e acessibilidade um pouco melhores, mas com um bug real de interface e um histórico de commits que não deixa ver como o código chegou lá. O Superpowers entregou um processo exemplar (TDD com commits atômicos) e menos bugs, mas com dependências desatualizadas e vulneráveis e um frontend menos cuidadoso.

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

## 2. Aderência aos requisitos

**OpenSpec: 9 · Superpowers: 9**

As duas atendem a RF01, RF02 e RF03 com decisões equivalentes:

- UUID gerado na aplicação, título obrigatório com até 100 caracteres (com `strip()` antes da validação), `dataVencimento` obrigatória, enums com `CHECK` no banco.
- A situação de prazo é calculada a cada resposta e nunca persistida. As duas acrescentaram `NO_PRAZO` para pendentes com vencimento futuro, uma lacuna do escopo que ambas resolveram do mesmo jeito.
- O score considera as tarefas concluídas na semana corrente (segunda a domingo), com a data de conclusão comparada ao vencimento, percentual com duas casas e `null` quando não há conclusões.
- O "hoje" vem de um `Clock` no fuso `America/Sao_Paulo`, e os testes usam um relógio ajustável.
- As duas criaram a coluna `data_conclusao` com uma constraint que garante coerência com o `status`. Sem ela o RF03 não seria calculável.

Diferenças de interpretação, nenhuma errada:

- Ordenação: o OpenSpec ordena por vencimento e depois prioridade (ALTA → BAIXA). O Superpowers põe as pendentes antes das concluídas, depois ordena por vencimento e título.
- O Superpowers aceita `status: CONCLUIDA` já no `POST`; o OpenSpec ignora `status` na criação.

## 3. Corretude

**OpenSpec: 7**

- **Bug confirmado: salvar uma edição reverte uma conclusão feita na lista.** Em `frontend/src/App.tsx`, `salvar()` envia `status: emEdicao.status`, e `alternarStatus()` não atualiza `emEdicao`. Para reproduzir: clique em **Editar** numa tarefa pendente, marque o checkbox de concluir a mesma tarefa na lista e clique em **Salvar alterações**. O `PUT` sai com `status: PENDENTE`, a tarefa é reaberta e a `dataConclusao` é apagada, o que também altera o score. Um teste de reprodução com Vitest confirmou o comportamento. A correção é a mesma da outra branch: atualizar `emEdicao` com a resposta do `PUT` de alternância.
- Detalhe menor: em `excluir()`, o formulário em edição é fechado antes da chamada à API, então ele se perde mesmo quando a exclusão falha.
- O backend não tem defeitos encontrados. A transição em `Tarefa.alterarStatus` (concluir, manter concluída preservando a data, reabrir) está correta e testada.

**Superpowers: 9**

- O mesmo cenário foi tratado explicitamente (commit `2faa06c`, *"mantém o formulário de edição sincronizado após concluir/reabrir"*), com teste de regressão em `App.test.tsx`.
- **Defeito menor:** o Jackson aceita números para enums por padrão, então `"prioridade": 1` é gravado como `MEDIA`. O OpenSpec bloqueia isso com `fail-on-numbers-for-enums: true` em `application.yml`.
- Erros de enum ou data inválidos no corpo voltam só com `detail` ("Valor inválido para o campo 'prioridade'"), sem a lista `erros`. O formulário mostra isso como erro geral, e não junto ao campo.

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

## 6. Testes

**OpenSpec: 8 · Superpowers: 8**

| | OpenSpec | Superpowers |
| --- | --- | --- |
| Backend | 81 (75 `@Test` + 3 `@ParameterizedTest`) | 58 |
| Frontend | 38 | 38 |
| Tipos | unitários de domínio, `@WebMvcTest` só da validação, repositório, serviço, API completa com Testcontainers | unitários de domínio, repositório, migração, relógio, API completa e atualização com Testcontainers |

- **OpenSpec:** cobertura de backend mais ampla, com uma fatia `@WebMvcTest` que roda sem Docker (e um README que explica como rodar só esses testes). Os testes parametrizados cobrem as regras de prazo e score. Perde ponto porque a suíte do frontend não pegou o bug de reversão de status, um fluxo que combina duas ações comuns.
- **Superpowers:** a suíte foi construída por TDD, como mostra o histórico, e inclui testes de regressão para bugs achados durante o desenvolvimento, testes de CORS (origem permitida e recusada), um teste de migração Flyway e um de 100 caracteres acentuados no título. Tem menos casos no backend e não testa enum numérico.

## 7. Segurança

Autenticação e autorização estão fora do escopo do MVP nas duas branches, e por isso não entraram na nota. Também nas duas: o JPA e as queries parametrizadas evitam SQL injection, o React escapa o conteúdo (não há `dangerouslySetInnerHTML`) e os erros 500 devolvem uma mensagem genérica, com o detalhe só no log.

Problemas comuns às duas:

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

## 8. Dependências e build

**OpenSpec: 8**

- Versões atuais (React 19, Vite 8, TypeScript 6, Vitest 5, Spring Boot 3.5.16), faixas `^` com `package-lock.json` versionado, `build` com `tsc -b`, script de lint.
- `.gitignore` separado por módulo. Não há `.gitattributes`, o que deixa o `mvnw` sujeito a conversão de fim de linha no Windows.
- PostgreSQL 17.

**Superpowers: 6**

- Versões fixadas exatamente (bom para reprodutibilidade), mas defasadas: React 18, Vite 6.3.5, Spring Boot 3.5.7. Isso causou as vulnerabilidades da seção anterior.
- Bom cuidado de infraestrutura: `.gitattributes` com `mvnw text eol=lf` e `*.cmd eol=crlf`, e Testcontainers fixado com justificativa.
- Sem linter. PostgreSQL 16.

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

## 10. Rastreabilidade do processo

**OpenSpec: 6**

- Os artefatos de spec são excelentes: proposta → design → deltas → tarefas → arquivamento → spec viva.
- O histórico do git não acompanha: a implementação inteira está num único commit, *"Implementacao completa"* (71 arquivos, ~6.800 linhas). Não dá para revisar por partes, bisectar ou ver em que ordem as decisões foram tomadas. O `tasks.md` marca as tarefas como feitas, mas sem vínculo com commits.

**Superpowers: 9**

- 18 commits atômicos em Conventional Commits, em português, um por tarefa do plano (`feat: entidade Tarefa…`, `fix: formulário exibe erros…`, `docs: corrige os blocos de comandos do README`). Os commits de `fix` mostram a revisão funcionando durante o desenvolvimento.
- Spec → plano → commits têm correspondência direta.
- Perde um ponto pelo volume do plano, que tende a envelhecer.

## 11. Operação e execução local

**OpenSpec: 9**

- O README cobre os problemas reais do ambiente (Docker Desktop parado, Avast/Web Shield interceptando TLS, porta ocupada) com comandos para PowerShell.
- Fuso configurável (`app.fuso`), `healthcheck` no Postgres, nome de container explícito, instruções para derrubar o banco e apagar o volume.

**Superpowers: 8**

- Variáveis de ambiente para o banco, `healthcheck`, troubleshooting curto, mas suficiente.
- Comandos de teste em subshell (`(cd backend && ./mvnw verify)`), que funcionam em sequência a partir da raiz.

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

**Ambas**

- Limitar o tamanho de `descricao` (DTO e coluna), publicar o Postgres só em `127.0.0.1` e considerar `@Version` na entidade.

## Conclusão

| | OpenSpec | Superpowers |
| --- | --- | --- |
| Média | **8,2** | **7,8** |
| Ponto mais forte | Specs vivas com cenários e código e UX mais refinados | Processo disciplinado (TDD, commits atômicos) e menos bugs |
| Ponto mais fraco | Bug de reversão de status na UI e implementação num único commit | Dependências vulneráveis e frontend menos acessível |

A diferença é pequena e vem de fatores diferentes. O OpenSpec produziu um **produto** ligeiramente melhor e uma documentação de requisitos mais útil a longo prazo, porque as specs continuam válidas depois da entrega. O Superpowers produziu um **processo** mais auditável, e esse processo pegou um bug que escapou ao OpenSpec. Para um time, a combinação ideal seria a spec viva do OpenSpec com a disciplina de TDD e de commits do Superpowers.
