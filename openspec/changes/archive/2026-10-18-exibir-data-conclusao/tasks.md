# Tasks

## 1. Exibição da data de conclusão na lista

- [x] 1.1 Em `frontend/src/ListaTarefas.test.tsx`, adicionar testes para os cenários de `task-web-ui`: tarefa `CONCLUIDA` com `dataConclusao` `2026-10-17` exibe "Concluída em 17/10/2026" ao lado do vencimento; tarefa `PENDENTE` não exibe nenhum texto "Concluída em"; ao re-renderizar uma tarefa que passou de `CONCLUIDA` para `PENDENTE` (`dataConclusao` `null`), o texto desaparece. Verificar que os novos testes falham com `npm test` em `frontend/`
- [x] 1.2 Em `frontend/src/ListaTarefas.tsx`, renderizar dentro de `tarefa-detalhes`, após "Vence em …", o texto `Concluída em {formatarData(dataConclusao)}` somente quando a tarefa estiver `CONCLUIDA` e tiver `dataConclusao`. Verificar que `npm test` passa por completo em `frontend/`
- [x] 1.3 Verificar `npm run lint` e `npm run build` sem erros em `frontend/`

## 2. Verificação integrada

- [x] 2.1 Com backend e frontend rodando, concluir uma tarefa pela lista e confirmar que aparece "Concluída em" com a data de hoje; reabri-la e confirmar que o texto some
