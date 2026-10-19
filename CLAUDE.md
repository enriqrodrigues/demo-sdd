## Fluxo de desenvolvimento (OpenSpec + Superpowers)

- Planejamento é do OpenSpec: use /opsx:explore e /opsx:propose. Não use
  superpowers:brainstorming nem superpowers:writing-plans, e não crie
  arquivos em docs/superpowers/.
- Implementação via /opsx:apply, aplicando em cada tarefa
  superpowers:test-driven-development (cenários da spec viram testes antes
  do código) e fazendo um commit por tarefa (Conventional Commits, em
  português), com o checkbox do tasks.md marcado no mesmo commit.
- Ao concluir cada seção do tasks.md, rode superpowers:requesting-code-review
  sobre os commits da seção. Problema de spec → /opsx:update antes de corrigir.
- Antes do /opsx:archive: revisão da branch inteira e
  superpowers:verification-before-completion (testes, build, lint, npm audit).
