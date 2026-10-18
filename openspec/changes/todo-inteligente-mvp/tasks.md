# Tasks

## 1. Infraestrutura e esqueleto

- [ ] 1.1 Criar `database/docker-compose.yml` com PostgreSQL (banco, usuário e senha de desenvolvimento, porta 5432, volume nomeado) e verificar com `docker compose -f database/docker-compose.yml up -d` que o container fica saudável
- [ ] 1.2 Gerar o projeto `backend/` (Spring Boot 3.5, Java 21, Maven Wrapper) com Web, Data JPA, Validation, Flyway, PostgreSQL e Testcontainers, e verificar que `./mvnw -q -DskipTests package` compila
- [ ] 1.3 Configurar `application.yml` (datasource local, `ddl-auto=validate`, `app.fuso=America/Sao_Paulo`) e o bean `Clock` com o fuso da propriedade; verificar com um teste de contexto que sobe com Testcontainers e injeta o `Clock` no fuso esperado
- [ ] 1.4 Gerar o projeto `frontend/` (Vite + React + TypeScript) com Vitest e React Testing Library e proxy de `/api` para `http://localhost:8080`; verificar que `npm run build` e `npm test` passam

## 2. Regras de domínio (deadline-status e transições de status)

- [ ] 2.1 Criar os enums `Prioridade`, `StatusTarefa` e `SituacaoPrazo` e a função pura de cálculo da situação de prazo; verificar com testes unitários que cobrem cada scenario de `deadline-status`, incluindo concluída com atraso e a virada de dia em UTC
- [ ] 2.2 Criar a entidade `Tarefa` com `alterarStatus(novoStatus, hoje)` aplicando as regras de `dataConclusao` (concluir, manter, reabrir e concluir de novo); verificar com testes unitários que cobrem cada scenario de "Controle da data de conclusão"
- [ ] 2.3 Criar a migração `V1__criar_tabela_tarefa.sql` (com `CHECK` de enums e de consistência entre status e data de conclusão) e o `TarefaRepository`; verificar com teste de integração (Testcontainers) que o Flyway aplica a migração e que salvar e ler uma tarefa preserva todos os campos

## 3. API de tarefas (task-management)

- [ ] 3.1 Implementar o tratamento global de erros com ProblemDetail (validação com `errors`, JSON malformado, data e enum inválidos com o campo identificado, UUID inválido, `404` de tarefa inexistente e `500` genérico); verificar com `@WebMvcTest` cobrindo os scenarios de "Formato padronizado de erro"
- [ ] 3.2 Implementar os DTOs de criação, atualização e resposta (trim do título, validações, campos do sistema ignorados, `situacaoPrazo` calculada a partir do `Clock`); verificar com `@WebMvcTest` os scenarios de validação de título, vencimento, prioridade e status
- [ ] 3.3 Implementar o serviço e o controller de `POST`, `GET` (lista ordenada e por id), `PUT` e `DELETE` em `/api/tarefas`; verificar com teste de integração (Testcontainers e `Clock` fixo) os scenarios de criação, listagem e ordenação, consulta, atualização, conclusão, reabertura e exclusão
- [ ] 3.4 Verificar no teste de integração que a `situacaoPrazo` de uma tarefa inalterada muda entre consultas quando o `Clock` avança (scenario "Situação muda com a passagem do tempo")

## 4. Score de produtividade (productivity-score)

- [ ] 4.1 Implementar o cálculo puro da semana (segunda a domingo) e do percentual (HALF_UP, 2 casas, `null` sem tarefas concluídas); verificar com testes unitários os scenarios de "Definição da semana corrente" e "Cálculo do percentual"
- [ ] 4.2 Adicionar ao repositório as consultas de contagem (concluídas na semana e concluídas no prazo) e expor `GET /api/produtividade/score`; verificar com teste de integração os scenarios de "Tarefas consideradas no score" (no prazo, com atraso, pendentes, semana anterior e reaberta) e o formato da resposta

## 5. Frontend (task-web-ui)

- [ ] 5.1 Implementar `api.ts` (listar, criar, atualizar, excluir, score) com conversão de ProblemDetail em erros por campo e erro genérico; verificar com testes Vitest usando `fetch` simulado
- [ ] 5.2 Implementar `ListaTarefas` com selo de situação, controle para concluir e reabrir e exclusão com confirmação; verificar com testes de componente os scenarios de lista, lista vazia, concluir, reabrir e excluir (confirmada e cancelada)
- [ ] 5.3 Implementar `FormularioTarefa` para criar e editar, com mensagens de erro junto aos campos e preservação dos dados digitados; verificar com testes de componente os scenarios de criação, edição e erro de validação
- [ ] 5.4 Implementar `CardScore` e montar o `App` com recarga da lista e do score após cada alteração e mensagem de falha de comunicação; verificar com testes os scenarios de score com dados, score sem dados e API indisponível

## 6. Documentação e verificação integrada

- [ ] 6.1 Escrever `README.md` na raiz com pré-requisitos, como subir banco, backend e frontend, como rodar os testes e a seção de troubleshooting (Docker Desktop parado, certificados TLS interceptados por antivírus); verificar seguindo os comandos do README do zero
- [ ] 6.2 Rodar todos os testes (`./mvnw verify` e `npm test`) e fazer um teste manual de ponta a ponta no navegador: criar, editar, concluir, reabrir e excluir tarefas, conferindo selos e score; verificar que tudo passa e que `openspec validate todo-inteligente-mvp --strict` continua válido
