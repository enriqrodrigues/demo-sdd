# Coluna "Conclusão" na lista de tarefas — Design

- **Data:** 2026-10-17
- **Origem:** pedido do usuário: "Adicione uma nova coluna no frontend para exibir a data de conclusão da tarefa. Caso a tarefa ainda não tenha sido concluída, exibir somente '-'."
- **Branch:** `release/demo-superpowers`
- **Altera:** a §7 de [`2026-10-16-todo-inteligente-design.md`](2026-10-16-todo-inteligente-design.md), item `TarefaLista.tsx`.

> **Nota de rastreabilidade:** esta spec foi escrita depois da implementação. O pedido entrou pelo brainstorming e foi classificado como "tarefa rápida e clara", então foi implementado direto. Esta spec e o plano correspondente registram a mudança a posteriori.

## 1. Objetivo

Mostrar na lista quando cada tarefa foi concluída. O usuário vê a data de conclusão ao lado do vencimento e compara as duas sem abrir a tarefa.

**Sucesso:** cada linha da lista mostra a data de conclusão em `dd/mm/aaaa`, ou `-` se a tarefa não está concluída.

## 2. Decisões

| # | Decisão | Origem |
|---|---------|--------|
| C1 | A lista ganha uma coluna com a data de conclusão da tarefa. | usuário |
| C2 | Tarefa sem data de conclusão (`dataConclusao == null`, ou seja, `PENDENTE`) mostra exatamente `-`. | usuário |
| C3 | O cabeçalho da coluna é `Conclusão`. | decisão do agente |
| C4 | A coluna fica logo depois de `Vencimento` e antes de `Situação`. A ordem final é Título, Prioridade, Vencimento, Conclusão, Situação, Ações. | decisão do agente |
| C5 | O formato é `dd/mm/aaaa`, feito por `formatarData` sobre a string ISO, igual ao vencimento. Não passa por `Date`, para não deslocar o fuso. | decisão do agente, por consistência com a §7 e o Review Focus 2 do plano original |

A §7 da spec original passa a ser lida assim: "TarefaLista.tsx: título, prioridade, vencimento (dd/mm/aaaa), conclusão (dd/mm/aaaa ou `-`), selo e ações".

## 3. Escopo

- **Só frontend.** A API já devolve `dataConclusao` em `TarefaResponse` (`"YYYY-MM-DD"` ou `null`), e o tipo `Tarefa` em `tipos.ts` já tem `dataConclusao: string | null`.
- **Fora do escopo:**
  - mudanças no backend ou no contrato REST;
  - ordenação ou filtro pela data de conclusão;
  - exibir a data de conclusão no formulário ou no score.
- **Anti-objetivo:** o frontend não calcula nem infere a data de conclusão. Ele só exibe o que o servidor manda (D5 da spec original).

## 4. Comportamento

| `status` | `dataConclusao` | célula "Conclusão" |
|---|---|---|
| `PENDENTE` | `null` | `-` |
| `CONCLUIDA` | `"2026-10-15"` | `15/10/2026` |

Ao reabrir uma tarefa, o servidor limpa a `dataConclusao`. Quando a lista recarrega, a célula volta a mostrar `-`.

## 5. Testes

Frontend (Vitest + Testing Library), em `TarefaLista.test.tsx`:
- o cabeçalho `Conclusão` existe;
- a linha de uma tarefa pendente tem uma célula `-`;
- a linha de uma tarefa concluída em `2026-10-15` tem uma célula `15/10/2026`.

## 6. Critérios de pronto

- `npm test` e `npm run build` passam no frontend.
- A spec, o plano e o código estão commitados no branch.
