Escopo inicial para o **Gerenciador de Tarefas com Alerta de Vencimento e Pontuação de Produtividade** desenhado para validar fluxo de desenvolvimento usando sdd.

---

## Escopo Inicial: To-Do Inteligente (MVP)

### 1. Visão Geral do Projeto

Um aplicativo minimalista de gerenciamento de tarefas pessoais que vai além de um CRUD tradicional, incorporando **regras de negócio dinâmicas no backend** (cálculo automático de status de prazo e pontuação de produtividade semanal) para testar validações, testes unitários e o ciclo completo de entrega.

### 2. Definições Tecnológicas

* **Arquitetura:** Monolíto Modular com módulos independentes de Backend, Frontend e Banco de Dados.
* **Stack:** Backend em **Java** (Spring Boot); Banco de Dados **PostgreSQL** (isolado em container Docker); Frontend em **React**.
* **Comunicação:** API REST JSON pura, com tratamento de erros padronizado.

---

### 3. Requisitos Funcionais e Regras de Negócio

#### RF01 - Cadastro e Gestão de Tarefas (CRUD)

* O usuário deve poder criar, listar, atualizar e excluir tarefas.
* **Campos da Tarefa:**
* `id`: Identificador único (UUID ou Long gerado automaticamente).
* `titulo`: Obrigatório, máximo de 100 caracteres.
* `descricao`: Opcional, texto livre.
* `dataVencimento`: Data obrigatória (formato `YYYY-MM-DD`).
* `prioridade`: Enum (`BAIXA`, `MEDIA`, `ALTA`).
* `status`: Enum (`PENDENTE`, `CONCLUIDA`).



#### RF02 - Lógica de Prazos Dinâmica (Calculada pelo Backend)

* O backend não armazena o estado de "Atrasado" fixo no banco; ele calcula em tempo de execução (ou via propriedades transitórias/DTOs) com base na `dataVencimento` e no `status` atual:
* Se `status == PENDENTE` e a data atual for maior que a `dataVencimento` $\rightarrow$ **Atrasada**.
* Se `status == PENDENTE` e a data for igual a hoje $\rightarrow$ **Para Hoje**.
* Se `status == CONCLUIDA` $\rightarrow$ **Concluída**.



#### RF03 - Cálculo de Score de Produtividade

* O sistema deve disponibilizar um endpoint analítico que retorna um indicador de desempenho simples:
* Percentual de tarefas concluídas no prazo versus o total de tarefas finalizadas na semana corrente.


---
