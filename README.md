*Este repositório é um teste de experimentação dos frameworks de SDD: OpenSpec e Superpowers.*
*Abaixo apresento o conceito, trago uma introdução de ambos os frameworks e comento sobre as condições e os resultados dos testes.*
*Atualizado em 20/10/2026.*


## SDD - SPEC-DRIVEN DEVELOPMENT

O **SDD (Spec-Driven Development)** já é uma das abordagens mais importantes quando falamos sobre trabalhar com Inteligência Artificial e agentes de código para o desenvolvimento de software.

### O QUE É O SDD?

Em resumo, o **Spec-Driven Development**, ou Desenvolvimento Orientado por Especificação, é uma metodologia onde a **especificação técnica detalhada** (documentos de requisitos, contratos de API, comportamentos esperados e critérios de aceite) é tratada como a **fonte primária da verdade** (e não apenas o código em si).

Antes de escrever qualquer linha de código, você escreve uma **especificação rigorosa e estruturada**. O código passa a ser um artefato gerado, validado ou testado diretamente a partir dessa especificação.

### POR QUE O SSD É IMPORTANTE?

Com a proliferação de assistentes de IA e ferramentas de geração de código, o gargalo do desenvolvimento mudou. Escrever código ficou mais rápido e fácil, o problema agora é garantir que o sistema desenvolvido faça exatamente o que o negócio precisa. O desafio é entregar um software funcional, com qualidade e segurança.

### VANTAGENS DE USAR SDD:

* **Menos "Vibe Coding"**: Evita a geração de código caótico ou baseado em palpites superficiais de IA. O Vibe Coding continua existindo, mas ganha estrutura para projetos reais.

* **Alinhamento de Contexto** (Prompt & Context Engineering): As IAs funcionam muito melhor quando recebem especificações claras, limitadas e estruturadas (em Markdown, JSON Schema, OpenAPI, etc.) do que quando recebem pedidos vagos como "crie uma tela de login".

* **Redução de Alucinações**: Ao definir contratos rígidos (as specs), você restringe o espaço de busca da IA e garante que o código gerado siga padrões de arquitetura e segurança pré-estabelecidos.

* **Escala e Segurança**: Facilita a divisão de grandes demandas em tarefas menores e gerenciáveis, reduzindo o retrabalho e o consumo excessivo de tokens em ferramentas de IA.

* **Documentação Viva**: A especificação deixa de ser um documento estático empoeirado no portal da empresa e passa a ser o artefato que aciona pipelines de CI/CD e geração de código.

### COMO USAR O SDD?

O fluxo básico do Spec-Driven Development pode ser dividido em três grandes etapas: **Especificar, Planejar e Implementar**. E cada uma dessas etapas contém passos menores que podem ser opcionais ou não a depender do contexto.

* **Especificar**: Aqui é onde o problema, os objetivos, o escopo e as regras de negócio são descritos de forma clara, geralmente em linguagem natural detalhada (como um PRD ou documento de requisitos). Esta etapa também pode incluir passos para **exploração** e **refinamento** da ideia antes de escrever a **especificação**.

* **Planejar**: Aqui o desenvolvedor (ou equipe) valida a especificação, **alinha** decisões técnicas e de arquitetura e **divide** a construção do software em tarefas menores que deverão ser executadas uma a uma para alcançar o resultado final. O resultado desta etapa é o backlog de tarefas a ser implementadas. Em geral, nenhuma linha de código é escrita até esta etapa.

* **Implementar**: Agora sim, depois de definir o que será construído e como será construído, partimos para **implementação** em si. O código é escrito seguindo estritamente as tarefas divididas no planejamento. E cada tarefa concluída é **revisada** e **validada** conforme o que foi acordado a partir da especificação.

### FRAMEWORKS DE SDD

Já existem diversos frameworks e bibliotecas voltados para a construção de software usando SDD: GitHub Spec-kit; OpenSpec; Superpowers; BMAD Method, etc.

Apesar de todos serem baseados no mesmo fluxo de trabalho “Especificar, Planejar e Implementar”, cada uma dessas ferramentas possui suas próprias características. E não existe uma opção melhor que a outra. Cada uma vai atender um determinado cenário ou necessidade de projeto. Inclusive, em alguns casos, é possível combinar funções de ferramentas diferentes aproveitando as habilidades que melhor atendem cada etapa do processo.

O Framework não concorre com os agentes de codificação (como o Claude Code ou Codex). Ele existe para estruturar o processo. O Agente continua executando, e o framework define o fluxo, os artefatos e a forma de trabalhar.


---

## TESTE - SUPERPOWERS | OPENSPEC

*A seguir trago um pouco sobre as diferenças entre os dois frameworks e comento sobre os testes realizados com as duas ferramentas. Observação: Utilizei o Claude Code com Opus 5.5 em todos os testes.*

### INSTALAÇÃO E CONFIGURAÇÃO

* #### Superpowers

A instalação varia de acordo com o ambiente de integração (harness). Para a maioria dos casos (incluindo o Claude Code), o Superpowers está disponível no marketplace oficial de plugins do Claude. 

Se utilizar mais de um ambiente de integração, é necessário instalar separadamente para cada um deles.

Detalhes no link oficial: https://github.com/obra/superpowers

* #### Openspec

O OpenSpec é instalado globalmente no sistema operacional e, após a instalação, é necessário inicilizar o framework no projeto onde deseja utiliza-lo.

Para inicializa-lo, acesse o diretório raiz do projeto e execute o comando ```openspec init``` no terminal.

Ele pedirá para informar o ambiente de integração que será utilizado (pode selecionar mais de um). Após essa configuração, a estrutura de pastas e arquivos necessários será criada e as skills do framework ficarão disponíveis no projeto.

Detalhes no link oficial: https://openspec.dev/

### ESTRUTURA DE ARQUIVOS

* #### Superpowers

O Superpowers não cria nenhuma estrutura no projeto durante a instalação: as skills ficam no plugin, fora do repositório. Os arquivos só aparecem quando as skills são usadas, todos no diretório `docs/superpowers`:

```
docs/superpowers/
├── specs/
│   └── <data><tema>-design.md
└── plans/
    └── <data><tema>.md
```

* **`specs/<data>-<tema>-design.md`**: o documento de design descreve o problema, a arquitetura, o modelo de dados, as regras de negócio, a API, a estratégia de testes, o que fica fora do escopo e os critérios de pronto. O agente só segue para o planejamento depois que o usuário aprova o documento.

* **`plans/<data>-<tema>.md`**: o plano de implementação é gerado a partir do design. Traz as restrições globais (versões, convenções, comandos) e as tarefas divididas em passos com caixas de seleção (`- [ ]`). Cada passo diz o teste a escrever, o código esperado e o commit a fazer.


* #### OpenSpec

O OpenSpec cria a sua estrutura no `openspec init` e depois a mantém com os próprios comandos. Os arquivos ficam em dois lugares:

```
.claude/                          ← integração com o Claude Code (gerada no init)
├── commands/opsx/                  explore, propose, apply, sync, update, archive...
└── skills/openspec-*/SKILL.md      as instruções que cada comando segue
```
* **`.claude/commands` e `.claude/skills`**: os comandos `/opsx:*` e as skills que o agente executa. São gerados para cada ambiente de integração escolhido no `init` e ficam versionados junto com o projeto.

```
openspec/
├── config.yaml                        ← contexto e regras do projeto
├── specs/                             ← specs vivas: o comportamento atual do sistema
│   └── <funcionalidade>/
│       └── spec.md
└── changes/                           ← uma pasta por mudança
    ├── <nome-da-mudanca>/             ← mudança em andamento
    │   ├── .openspec.yaml
    │   ├── proposal.md
    │   ├── design.md
    │   ├── tasks.md
    │   └── specs/
    │       └── <funcionalidade>/
    │           └── spec.md
    └── archive/                       ← histórico das mudanças concluídas
        └── <data>-<nome-da-mudanca>/  ← mudança implementada e arquivada (mesmos arquivos)
```
* **`config.yaml`**: define o esquema de trabalho e o contexto do projeto, regras por artefato e orientações por operação. É onde o time coloca as restrições que o agente não consegue deduzir lendo o código.

* **`specs/<funcionalidade>/spec.md`**: as specs vivas, uma por funcionalidade do sistema. Cada requisito é escrito em linguagem normativa (SHALL) e tem cenários no formato WHEN/THEN. Representam o que o sistema faz hoje.

* **`changes/<nome-da-mudanca>/`**: uma mudança em andamento. Tudo o que é preciso para implementar a mudança fica aqui enquanto ela não for arquivada. Pode haver mais de uma mudança em andamento ao mesmo tempo, cada uma na sua pasta.

* **`changes/archive/<data>-<nome-da-mudanca>/`**: uma mudança concluída. Depois de aplicar as specs delta às specs vivas, o Openspec arquiva a mudança movendo a pasta para cá e põe a data do arquivamento no início do nome. Os arquivos não são mais alterados e servem de histórico: por que a mudança foi feita, que decisões foram tomadas e quais tarefas foram executadas.

Cada pasta de mudança, em andamento ou arquivada, tem os mesmos arquivos:

* `proposal.md`: o porquê da mudança, o que entra e o que fica fora do escopo;
* `design.md`: as decisões técnicas e as alternativas descartadas;
* `tasks.md`: a lista de tarefas com caixas de seleção, cada uma citando os cenários que cobre;
* `specs/<funcionalidade>/spec.md`: as **specs delta**, que descrevem só o que muda nas specs vivas, em seções `ADDED`, `MODIFIED` e `REMOVED Requirements`;
* `.openspec.yaml`: metadados da mudança (esquema e data de criação).

### FLUXO BÁSICO DE TRABALHO

* #### Superpowers

No Superpowers o fluxo é conduzido pelas skills, que o próprio agente aciona conforme o contexto da conversa. Não é preciso chamar comandos: ao pedir uma nova funcionalidade, o agente começa pelo brainstorming e segue a sequência abaixo.

1. **Brainstorming** (`brainstorming`): o agente explora a ideia fazendo perguntas, uma de cada vez, propõe abordagens com os prós e contras de cada uma e apresenta o design em partes para validação. No fim, grava o documento de design em `docs/superpowers/specs/` e espera a aprovação do usuário.
2. **Plano** (`writing-plans`): a partir do design aprovado, o agente escreve o plano de implementação em `docs/superpowers/plans/`, com tarefas pequenas, cada uma com o teste, o código e o commit esperados.
3. **Execução** (`subagent-driven-development` ou `executing-plans`): o plano é executado tarefa a tarefa. No modo com subagentes, cada tarefa vai para um subagente novo e passa por revisão antes da próxima. Em todas as tarefas vale o TDD (`test-driven-development`): primeiro o teste que falha, depois o código que o faz passar.
4. **Revisão e verificação** (`requesting-code-review` e `verification-before-completion`): o código é revisado contra o plano, e o agente só declara uma tarefa concluída depois de rodar os testes e conferir o resultado.
5. **Finalização** (`finishing-a-development-branch`): com tudo pronto, o agente apresenta as opções para integrar o trabalho (merge, pull request, manter a branch ou descartar).

* #### OpenSpec

No OpenSpec o fluxo é conduzido pelo usuário, que chama cada etapa com um comando `/opsx:*`. Cada comando lê e atualiza os artefatos da mudança em `openspec/changes/`.

1. **Explorar** (`/opsx:explore`, opcional): uma conversa para entender o problema, investigar o código e comparar alternativas, sem criar artefatos nem escrever código.
2. **Propor** (`/opsx:propose`): cria a pasta da mudança com a proposta, as specs delta, o design e a lista de tarefas. O usuário revisa os artefatos antes de seguir.
3. **Implementar** (`/opsx:apply`): o agente executa as tarefas do `tasks.md` e marca cada uma como concluída. Se algo muda no meio do caminho, o `/opsx:update` ajusta os artefatos antes de continuar.
4. **Arquivar** (`/opsx:archive`): com as tarefas concluídas, as specs delta são aplicadas às specs vivas e a mudança vai para `changes/archive/`.

O OpenSpec não impõe uma forma de implementar: TDD, commits por tarefa ou revisão de código não fazem parte do fluxo padrão. Quando o time quer essas práticas, elas precisam ser descritas como regras no `config.yaml` (ou no arquivo de instruções do agente, como o `CLAUDE.md`).

### TESTE PRÁTICO

#### Como o teste foi feito

Os frameworks foram testados com o mesmo escopo, descrito em [`escopo_inicial.md`](escopo_inicial.md): o **To-Do Inteligente**, um gerenciador de tarefas pessoais com CRUD (RF01), situação de prazo calculada pelo backend (RF02) e score semanal de produtividade (RF03), usando Spring Boot, PostgreSQL em Docker e React.

Cada implementação foi feita do zero, numa branch própria e numa sessão nova do Claude Code, partindo só do escopo inicial. O agente não tinha acesso ao código das outras branches. Quando o escopo deixava alguma regra em aberto, a dúvida foi levada ao usuário em vez de deixar o agente decidir sozinho.

Foram três rodadas:

* **Superpowers**: o fluxo completo do Superpowers, do brainstorming à finalização da branch, com a execução feita por subagentes.
* **OpenSpec**: o fluxo padrão do OpenSpec (explore, propose, apply e archive), sem regras adicionais no `config.yaml`.
* **Híbrido (OpenSpec + Superpowers)**: o OpenSpec cuida do planejamento e do acompanhamento das tarefas (`/opsx:explore`, `/opsx:propose`, `tasks.md` e `/opsx:archive`), e o Superpowers entra na execução, com TDD em cada tarefa, um commit por tarefa e revisão de código ao fim de cada seção do `tasks.md`. As regras dessa divisão estão no `CLAUDE.md` e no `openspec/config.yaml` da branch.

Ao final, as implementações foram revisadas pelo Claude com os mesmos 11 critérios (documentação, aderência aos requisitos, corretude, arquitetura, qualidade do código, testes, segurança, dependências, frontend, rastreabilidade do processo e execução local). A revisão completa está em [`revisao-claude.md`](revisao-claude.md).

#### Onde conferir o resultado

O resultado de cada rodada está na sua branch, com o código, os testes e os artefatos gerados pelo framework. O `README.md` de cada branch explica como subir o banco, rodar o backend e o frontend e executar os testes.

| Branch | Framework | Commits | Testes (backend + frontend) | Nota da revisão |
| --- | --- | :---: | :---: | :---: |
| [`release/demo-superpowers`](../../tree/release/demo-superpowers) | Superpowers | 18 | 58 + 38 | 7,8 |
| [`release/demo-openspec`](../../tree/release/demo-openspec) | OpenSpec | 4 | 81 + 38 | 8,2 |
| [`release/demo-combinado-openspec-superpowers`](../../tree/release/demo-combinado-openspec-superpowers) | Híbrido | 44 | 125 + 61 | 9,0 |

Para testar localmente, basta trocar de branch (`git checkout <branch>`) e seguir o README.


### AVALIAÇÃO PESSOAL

*O objetivo deste teste não foi identificar qual framework é melhor, mas sim entender um pouco sobre como funcionam e como utiliza-los.*
*A avaliação técnica qualitativa gerada pelo code-review do Claude Code está em [`revisao-claude.md`](revisao-claude.md).*
*Nesta última sessão quero expor algumas considerações e impressões pessoais que tive durante o experimento.*

#### Fluxo de Trabalho

Ambos possuem um fluxo básico de trabalho bem definido que pode ser seguido por qualquer desenvolvedor menos experiente. A principal diferença entre eles está no controle do fluxo.

No Superpowers, o agente aciona as skills sozinho e conduz a conversa, fazendo perguntas e pedindo aprovação a cada etapa. A execução se torna mais fluida, porém isso pode ser um problema se a janela de contexto ficar muito grande.

No OpenSpec, o usuário chama cada etapa com um comando, e o ritmo fica com ele. A execução é mais controlada.

#### Especificação

Acredito que a construção e manutenção da especificação é o ponto fraco do superpowers. Ele gera um documento de especificação e de planejamento por demanda. Após a aprovação, esses documentos não são alterados e envelhecem com o código. Se novas alterações são feitas na mesma funcionalidade, outros documentos serão gerados e será necessário ler o histórico completo para entender o seu comportamento.

O OpenSpec gera uma documentação mais estruturada seperando a especificação atualizada e a especificação de cada mudança. Então, se uma funcionalidade é alterada várias vezes, além de existir o histórico dessas alterações. Há a especificação principal que é atualizada a cada mudança arquivada, com requisitos e cenários que continuam válidos depois da entrega. Essa especificação atual tende a ser uma fonte de consulta mais rápida.

Ainda em relação à especificação, observei que o Superpowers nem sempre segue à risca os princípios do SDD. Quando o usuário invoca a skill de brainstorming, ele classifica a tarefa em três tamanhos de trabalho: Tarefa rápida e clara; Mudança pequena; Projeto. Tarefas classificadas como "rápida e clara" não geram perguntas e nem documentação. Ele simplesmente pula a etapa de especificação e planejamento e parte direto para implementação. Não é um defeito, é um comportamento proposital. Isso pode trazer problemas a longo prazo, pois perde-se a rastreabilidade dessas mudanças e a especifiação fica divergente da implementação.

#### Implementação

Nesse ponto, o Superpowers apresentou uma vantagem: Ele impõe um ciclo de implementação rigoroso com TDD, commits pequenos, revisão de código e verificação antes de declarar uma tarefa concluída. 

O resultado é um processo mais auditável e que permitiu pegar um bug durante o desenvolvimento (Esse mesmo bug passou pelo Openspec).

O OpenSpec também entregou um ótimo resultado de código e um frontend até mais  cuidadoso, mas acabou deixando passar um bug de interface que escapou aos testes.

Além disso, toda a implementação do OpenSpec ficou num único commit, o que dificulta revisar o código por partes ou entender a ordem das decisões.

#### Combinando os Frameworks

Considerando que o Openspec foi melhor nas etapas de especificação e planejamento e que o Superpowers foi melhor na implementação das tarefas, decidi fazer um terceiro teste combinando recursos dos dois frameworks.

O 'híbrido' juntou as forças dos dois: as specs vivas do OpenSpec com a disciplina de execução do Superpowers. 

Teve a melhor nota e nenhum bug encontrado na revisão, mas custou mais (44 commits, revisões por seção e mais linhas de teste do que de código). 

Foi o teste mais demorado. Tanto o Superpowers quando o Openspec puros demoraram cerca de 1 hora no fluxo completo. O Híbrido demorou cerca de 2 horas e meia.

O consumo de tokens também foi mais elevado (não medi os valores exatos). O Superpowers e o Openspec tiveram consumos parecidos com cerca de 30% dos tokens da sessão para cada um. O teste híbrido passou dos 70% de tokens da sessão.

#### Conclusão

Os frameworks devem ajudar a conduzir o fluxo de trabalho, mas é fundamental entender os princípios do SDD para aplica-los (ou até combina-los) de forma correta.

Não dá para apontar qual ferramenta é melhor, mas quanto mais conhecermos os princípios do método e o funcionamento do frameworks, melhores poderão ser os resultados.

E no final, o que importa é entregar o software funcionando, e nisso todos os testes obtiveram sucesso.

