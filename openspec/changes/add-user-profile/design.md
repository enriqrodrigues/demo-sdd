# Design

## Context

Esta change parte do código entregue por `add-user-onboarding` e do que a change `add-authentication` vai entregar, conforme o design dela:

- **Sessão e principal:** o principal `AuthenticatedUser(id, name, email)` fica na sessão; o entry point responde 401 `UNAUTHENTICATED` para `/api/**` sem sessão; toda requisição que altera estado exige o token CSRF.
- **Frontend:** o `api.ts` tem um `request` genérico que envia o cabeçalho `X-XSRF-TOKEN`; existem `RequireAuth`, `/inicio` (`HomePage`) e `/login`.
- **Reutilização do cadastro:**
  - no backend, `InputNormalizer` e os validadores `@Phone`, `@Cep` e `@Uf` (em `shared.validation`), e o formato de erro `ProblemDetail` com `errors[]`;
  - no frontend, `validation.ts` (`validateField`, máscaras e `UFS`), `useFormValidation` e `FormField`.
- **Dados:** a entidade `User` tem `Address` embutido; nome, CPF, e-mail e data de nascimento não têm setters. A tabela `users` já tem todas as colunas, então não há migração.

A motivação está em proposal.md e os requisitos em `specs/user-profile`.

## Goals / Non-Goals

**Goals:**
- RN01 garantida em duas camadas: a API recusa campos imutáveis, e o domínio não oferece forma de alterá-los.
- As mesmas regras de validação do cadastro, sem reescrevê-las.
- O perfil derivado sempre da sessão: não existe id de usuário na URL nem no corpo da requisição.

**Non-Goals:**
- Controle de concorrência (edição simultânea em duas abas): vale a última gravação.
- Auditoria e histórico de alterações.
- Troca de senha e troca de e-mail.

## Decisions

### D1. Módulo `profile` no backend

- **Pacote novo:** `br.com.demosdd.profile`, com `ProfileController`, `ProfileService`, `ProfileResponse` e `ProfileUpdateRequest`.
- **Dependências:** `user` (entidade e repositório), `shared` (validação e erros) e `auth` (o `AuthenticatedUser`, injetado com `@AuthenticationPrincipal`).
- **Pré-requisito em `auth`:** o `AuthenticatedUser` precisa estar no pacote base do módulo, para ser API pública no Spring Modulith. Se a implementação da autenticação o colocar num subpacote, ele deve ser movido ou exposto com `@NamedInterface`.
- **Alternativa descartada:** colocar o perfil dentro de `auth` ou `user`. O perfil é uma capability própria (spec `user-profile`), e um módulo separado mantém `user` como modelo puro e `auth` restrito à sessão.

### D2. RN01 no domínio

`User` ganha um único método de alteração:

```java
public void updateContact(String phone, Address address)
```

Ele troca o telefone e o endereço inteiro (o `Address` é tratado como valor). Nome, CPF, e-mail e data de nascimento continuam sem setters, então nenhum código consegue alterá-los por engano, nem nas próximas changes.

### D3. RN01 na API: campos imutáveis são recusados, não ignorados

O `ProfileUpdateRequest` declara os campos editáveis e também `name`, `cpf`, `email` e `birthDate` como `Object`, anotados com `@Null(message = "Este campo não pode ser alterado")`.

- **Como funciona:** se algum deles vier preenchido, a validação padrão devolve 400 `VALIDATION_ERROR` com um item em `errors[]` por campo, no mesmo formato do cadastro, e nada é gravado.
- **Por que `Object`:** qualquer valor (texto, número, data em qualquer formato) é recusado pela mesma regra, sem erro de desserialização.
- **Valor nulo:** um campo imutável enviado como `null` não representa tentativa de alteração e é aceito.
- **Outros campos desconhecidos:** são ignorados, que é o padrão do Jackson no Spring Boot.

**Alternativa descartada:** ignorar silenciosamente os campos imutáveis. Seria mais tolerante, mas esconderia do cliente que a alteração não aconteceu, e a spec pede que a tentativa seja recusada indicando o campo.

### D4. Validação dos campos editáveis

O `ProfileUpdateRequest` repete as anotações do `RegistrationRequest` para telefone e endereço:

- `@NotBlank` e `@Phone`;
- `@Cep` e `@Uf`;
- `@Size` com os mesmos limites;
- complemento opcional.

O construtor compacto aplica o mesmo `InputNormalizer` (trim, remoção de máscaras, UF em maiúsculas, complemento vazio vira `null`).

**Alternativa descartada:** extrair um record `AddressInput` compartilhado e aninhado. Isso mudaria o formato JSON do cadastro, que hoje é plano, só para reaproveitar seis anotações. A repetição fica explícita, e os testes dos dois endpoints garantem as mesmas regras.

### D5. Contrato REST

| Endpoint | Sucesso | Erros (`code`) |
|---|---|---|
| `GET /api/profile` | 200 `ProfileResponse` | 401 `UNAUTHENTICATED` |
| `PUT /api/profile` `{ phone, cep, street, number, complement, district, city, state }` | 200 `ProfileResponse` atualizado | 400 `VALIDATION_ERROR` (inclui campos imutáveis); 401 `UNAUTHENTICATED`; 403 `CSRF_INVALID` |

- **`ProfileResponse`:** `{ name, cpf, email, birthDate, phone, cep, street, number, complement, district, city, state }`, com CPF, telefone e CEP só com dígitos. A formatação é responsabilidade do frontend, com as máscaras existentes.
- **Por que `PUT`:** o cliente sempre envia o conjunto completo de campos editáveis, e todos os obrigatórios precisam vir. Isso evita a ambiguidade de "campo ausente = manter" de um `PATCH`.
- **Identificação do usuário:** o `ProfileService` carrega o usuário pelo `id` do `AuthenticatedUser`, chama `updateContact` e grava, numa única transação. Não existe id na rota, o que atende a spec "Perfil restrito ao próprio usuário".
- **Rota protegida:** pela regra geral do design D5 da autenticação ("demais `/api/**` exigem autenticação"), `/api/profile` já nasce protegido, sem mudança na `SecurityConfig`.

### D6. Frontend

- **`validation.ts`:**
  - `validateField` e `validateForm` passam a aceitar qualquer subconjunto de campos (`Partial<Record<FieldName, string>>` e uma lista de campos);
  - nova exportação `CONTACT_FIELDS`, com telefone e endereço.

  As regras não mudam, e os testes existentes do cadastro garantem isso.
- **`useFormValidation`:** passa a receber a lista de campos e os valores iniciais (hoje são fixos no cadastro).
- **`api.ts`:** novas funções `getProfile()` e `updateProfile(values)`, sobre o `request` com CSRF.
- **Página `/perfil` (`ProfilePage`, dentro de `RequireAuth`):**
  - **Dados pessoais**, somente leitura (`<dl>`): nome, CPF com máscara, e-mail e data de nascimento em `dd/mm/aaaa`;
  - **Contato e endereço**, formulário preenchido com os valores atuais e as mesmas máscaras, o mesmo `FormField`, a validação em tempo real e o select de UF;
  - botões "Salvar alterações" e "Descartar alterações" (restaura os valores carregados);
  - sucesso: mensagem `role="status"` "Dados atualizados com sucesso." e formulário com os valores devolvidos pelo servidor;
  - erros do servidor nos campos;
  - 401 ao salvar (sessão expirada): vai para `/login`.
- **Navegação:** link "Meu perfil" na `HomePage` (`/inicio`) e link "Voltar ao início" no perfil.

### D7. Estratégia de testes

| Nível | Cobre |
|---|---|
| Unidade (back) | `User.updateContact` altera só telefone e endereço |
| Integração (back) | cada cenário da spec: GET com e sem sessão; PUT válido (valores normalizados gravados); remoção do complemento; telefone inválido e cidade vazia (nada gravado); campos imutáveis recusados (um e vários); isolamento entre dois usuários; PUT sem sessão (401) e sem CSRF (403) |
| Arquitetura | `ApplicationModules.verify()` com o módulo `profile` |
| Front | `validation.ts` com subconjunto de campos; `api.ts` (`getProfile`, `updateProfile`); `ProfilePage` (somente leitura, formulário preenchido e formatado, validação ao sair do campo, sucesso, erros do servidor, descartar, 401 leva a `/login`); link na área interna |

Os testes de integração usam um helper de login (sessão autenticada via `POST /api/auth/login`) criado pela change de autenticação, ou o `user()`/`authentication()` do `spring-security-test` com um `AuthenticatedUser`.

## Risks / Trade-offs

- **Dependência de uma change ainda não implementada.** Nomes como `AuthenticatedUser`, `RequireAuth`, `request` e `HomePage` vêm do design da `add-authentication`. → Se a implementação dela mudar algo, ajustar este design e as tarefas com `/opsx:update` antes de implementar. Implementar e arquivar sempre na ordem: autenticação, depois perfil.
- **Anotações de validação repetidas entre cadastro e perfil.** → Os testes de integração dos dois endpoints usam os mesmos exemplos das specs. Uma mudança de regra exige alterar os dois DTOs, o que deixa a mudança visível na revisão.
- **Duas abas editando ao mesmo tempo: a última gravação vence.** → Aceitável para um perfil pessoal; controle otimista (`@Version`) pode entrar se for necessário.
- **A recusa de campos imutáveis depende de o cliente enviá-los.** → O frontend nunca os envia, e o domínio não tem setters (D2), então nem um bug no DTO consegue alterá-los.

## Migration Plan

Sem migração de banco. Requer a change `add-authentication` implementada antes. O README ganha a seção de perfil.
