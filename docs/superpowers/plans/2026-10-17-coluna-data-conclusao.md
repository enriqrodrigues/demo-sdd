# Coluna "Conclusão" na lista de tarefas: Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Nota de rastreabilidade:** este plano foi escrito depois da implementação. As mudanças da Task 1 já estão na working tree, ainda sem commit. Os passos de RED e GREEN foram executados e estão marcados como feitos; falta o commit.

**Goal:** mostrar na `TarefaLista` uma coluna com a data de conclusão de cada tarefa, ou `-` quando ela não foi concluída.

**Architecture:** mudança só de apresentação no componente `TarefaLista`. Reaproveita `formatarData`, de `formatacao.ts`, e o campo `dataConclusao`, que a API já devolve.

**Tech Stack:** React 18.3.1, TypeScript 5.8.3, Vitest 3.2.4, Testing Library.

**Spec:** [`docs/superpowers/specs/2026-10-17-coluna-data-conclusao-design.md`](../specs/2026-10-17-coluna-data-conclusao-design.md). Ela altera a §7 de [`2026-10-16-todo-inteligente-design.md`](../specs/2026-10-16-todo-inteligente-design.md).

## Global Constraints

- O cabeçalho é exatamente `Conclusão`, entre `Vencimento` e `Situação` (C3, C4).
- Sem data de conclusão, a célula mostra exatamente `-`, um hífen ASCII (C2).
- A data sai em `dd/mm/aaaa`, via `formatarData(iso)`. Nunca use `new Date(...)` (C5).
- O frontend não calcula a data de conclusão; só exibe `tarefa.dataConclusao`.
- Não muda nada no backend nem em `tipos.ts`.
- Commits usam `git commit -F -` com heredoc e terminam com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Deslocamento de fuso.** `dataConclusao = "2026-10-15"` precisa aparecer como `15/10/2026`, e não como 14/10. O teste da Task 1 fixa esse valor.
2. **Pendente com `null`.** A célula mostra `-`, e não `""`, `null` ou `Invalid Date`. Teste na Task 1.
3. **Linha certa.** A data aparece na linha da própria tarefa, e não em outra linha. O teste da Task 1 verifica cada linha com `within`.
4. **Reabrir.** Depois de reabrir, o servidor devolve `dataConclusao: null` e a célula volta a `-`. Esse caso cai na mesma regra do item 2, então não tem teste próprio.

---

### Task 1: Coluna "Conclusão" na `TarefaLista`

**Files:**
- Modify: `frontend/src/componentes/TarefaLista.tsx`: um `<th>` no `thead` e um `<td>` em cada linha do `tbody`.
- Test: `frontend/src/componentes/TarefaLista.test.tsx`

**Interfaces:**
- Consumes: `Tarefa.dataConclusao: string | null` (`src/tipos.ts`); `formatarData(iso: string): string` (`src/formatacao.ts`).
- Produces: nada novo. As props de `TarefaLista` não mudam.

- [x] **Step 1: Escrever o teste que vai falhar**

Em `TarefaLista.test.tsx`, importe `within` de `@testing-library/react` e use as fixtures `pendente` e `concluida` que já existem no arquivo. A `concluida` tem `dataConclusao: '2026-10-15'`.

```tsx
it('mostra a data de conclusão formatada ou "-" se não concluída', () => {
  renderizar([pendente, concluida]);

  expect(screen.getByRole('columnheader', { name: 'Conclusão' })).toBeInTheDocument();
  const [, linhaPendente, linhaConcluida] = screen.getAllByRole('row');
  expect(within(linhaPendente).getByRole('cell', { name: '-' })).toBeInTheDocument();
  expect(within(linhaConcluida).getByRole('cell', { name: '15/10/2026' })).toBeInTheDocument();
});
```

- [x] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npx vitest run src/componentes/TarefaLista`
Expected: 1 falha, `Unable to find an accessible element with the role "columnheader" and name "Conclusão"`.

- [x] **Step 3: Implementar**

Em `TarefaLista.tsx`, adicione `<th>Conclusão</th>` depois de `<th>Vencimento</th>`. Em cada linha, depois da célula do vencimento, adicione uma célula com `formatarData(tarefa.dataConclusao)` quando `dataConclusao` não for `null`, ou `'-'` quando for.

- [x] **Step 4: Rodar a suíte e o build**

Run: `cd frontend && npm test && npm run build`
Expected: `Tests 38 passed (38)`, e o build termina com `✓ built`.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-10-17-coluna-data-conclusao-design.md \
        docs/superpowers/plans/2026-10-17-coluna-data-conclusao.md \
        frontend/src/componentes/TarefaLista.tsx frontend/src/componentes/TarefaLista.test.tsx
git commit -F - <<'EOF'
feat: coluna com a data de conclusão na lista de tarefas

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```
