# Tasks

> **Pré-requisito:** a change `add-authentication` precisa estar implementada (e, de preferência, arquivada) antes de começar. Antes do grupo 1, confira se os nomes usados aqui (`AuthenticatedUser`, `RequireAuth`, `request` no `api.ts`, `HomePage`) batem com o código entregue. Se não baterem, ajuste os artefatos com `/opsx:update`.
>
> Cada grupo termina com um commit próprio, e o `./mvnw verify` precisa passar ao fim de cada grupo.

## 1. Perfil no backend

- [x] 1.1 Adicionar `User.updateContact(phone, address)`, que altera só o telefone e o endereço. Verificar com um teste unitário: nome, CPF, e-mail e data de nascimento permanecem iguais depois da chamada.
- [x] 1.2 Criar o módulo `profile` com `ProfileResponse` e `GET /api/profile` (usuário carregado pelo `id` do `AuthenticatedUser`). Confirmar que `AuthenticatedUser` é API pública do módulo `auth` (design D1). Verificar com testes de integração: 200 com todos os dados do usuário logado; 401 sem sessão.
- [x] 1.3 Criar o `ProfileUpdateRequest`: campos editáveis normalizados e validados como no cadastro, e `name`, `cpf`, `email` e `birthDate` como `Object` com `@Null` (design D3). Criar também o `PUT /api/profile` transacional. Verificar com testes de integração:
  - alteração válida (telefone `(21) 3456-7890` e CEP `20040-020` gravados como `2134567890` e `20040020`, com o perfil devolvido atualizado);
  - remoção do complemento;
  - telefone `(21) 3456-789` recusado sem gravar nada;
  - cidade vazia recusada.
- [x] 1.4 Cobrir a RN01 e o isolamento. Verificar com testes de integração:
  - o PUT com novo e-mail recebe 400 com `errors[]` apontando `email`, e nada é gravado;
  - o PUT com nome, CPF e data de nascimento aponta os três campos;
  - dois usuários em sessões diferentes veem e alteram só os próprios dados;
  - o PUT sem sessão recebe 401, e sem token CSRF recebe 403.
- [x] 1.5 Verificar as fronteiras: o `ApplicationModules.verify()` passa com `profile` dependendo de `user`, `shared` e `auth`, sem ciclos.

## 2. Validação compartilhada e API (frontend)

- [x] 2.1 Generalizar `validateField`, `validateForm` e `useFormValidation` para um subconjunto de campos e valores iniciais, e exportar `CONTACT_FIELDS`. Verificar que os testes existentes do cadastro continuam passando sem alteração, e acrescentar testes de `validateForm` só com os campos de contato.
- [x] 2.2 Adicionar `getProfile()` e `updateProfile(values)` ao `api.ts`. Verificar com testes Vitest: 200, 400 com `errors[]` por campo, 401 e envio do cabeçalho CSRF no PUT.

## 3. Página de perfil (frontend)

- [x] 3.1 Implementar a `/perfil` dentro de `RequireAuth`: dados pessoais somente leitura (CPF com máscara, data em `dd/mm/aaaa`) e formulário de contato e endereço preenchido com máscaras, validação em tempo real e select de UF. Verificar com testes React Testing Library:
  - nome, CPF, e-mail e data de nascimento não são campos editáveis;
  - o formulário vem preenchido e formatado;
  - CEP com 7 dígitos mostra erro ao sair do campo.
- [x] 3.2 Implementar salvar e descartar. Verificar com testes React Testing Library:
  - sucesso mostra "Dados atualizados com sucesso." e os valores devolvidos;
  - erros do servidor aparecem nos campos;
  - descartar restaura os valores carregados;
  - 401 ao salvar leva a `/login`;
  - sem sessão, `/perfil` leva a `/login`.
- [x] 3.3 Adicionar o link "Meu perfil" na área interna e "Voltar ao início" no perfil. Verificar com testes React Testing Library.

## 4. Documentação e verificação integrada

- [ ] 4.1 Atualizar o README com a página de perfil, os endpoints `GET` e `PUT /api/profile` e a regra de campos imutáveis. Verificar executando os exemplos `curl` documentados (com sessão e token CSRF) contra a aplicação rodando.
- [ ] 4.2 Rodar o `./mvnw verify` completo. Verificar que termina com BUILD SUCCESS e sem testes ignorados.
- [ ] 4.3 Fazer o teste manual no navegador:
  - entrar, abrir o perfil, alterar telefone (fixo e celular) e endereço, salvar e recarregar;
  - remover o complemento;
  - tentar salvar com CEP inválido;
  - confirmar que nome, CPF, e-mail e data de nascimento não são editáveis.

  Registrar o resultado no PR.
