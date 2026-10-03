# Proposal

## Why

Depois de entrar na plataforma (change `add-authentication`), o usuário não tem como consultar nem corrigir os próprios dados. Esta change entrega o RF07 (área de perfil para visualizar e atualizar as informações permitidas) e a RN01 (dados críticos não podem ser alterados depois do cadastro). Ela fecha os requisitos funcionais e as regras de negócio do esboço.

Conforme definido com o usuário:

- **Imutáveis:** nome, CPF, e-mail e data de nascimento.
- **Editáveis:** telefone e endereço (CEP, logradouro, número, complemento, bairro, cidade, UF), todos obrigatórios exceto o complemento.
- **Troca de senha:** não faz parte do perfil e fica para a fase de melhorias, junto com a recuperação de senha.

## What Changes

- **RF07, visualização:**
  - página `/perfil`, acessível só ao usuário autenticado e a partir da área interna;
  - exibe todos os dados do próprio usuário;
  - os dados imutáveis aparecem como somente leitura.
- **RF07, edição:**
  - formulário já preenchido com telefone e endereço, com as mesmas máscaras e a mesma validação em tempo real do cadastro;
  - o servidor revalida com as mesmas regras (telefone fixo ou celular com DDD, CEP de 8 dígitos, UF válida, obrigatórios, tamanhos máximos);
  - ao salvar, o usuário recebe confirmação e os dados ficam gravados.
- **RN01:**
  - a interface não oferece edição de nome, CPF, e-mail e data de nascimento;
  - a API recusa qualquer tentativa de alterá-los, apontando o campo, e não grava nada.
- **Isolamento:** o perfil é sempre o do usuário da sessão. Não existe forma de consultar ou alterar o perfil de outra pessoa.

### Fora do escopo

Ficam para a fase de melhorias:

- troca de senha;
- troca de e-mail com nova confirmação;
- foto de perfil;
- consulta de CEP em serviço externo;
- histórico de alterações.

## Capabilities

### New Capabilities
- `user-profile`: visualização dos dados do usuário autenticado, edição de telefone e endereço com validação, imutabilidade de nome, CPF, e-mail e data de nascimento (RN01), e isolamento do perfil ao próprio usuário.

### Modified Capabilities
<!-- Nenhuma. O link "Meu perfil" na área interna é um detalhe de interface e não
     altera os requisitos da capability authentication (que, além disso, ainda está
     na change add-authentication, sem spec principal). -->

## Impact

- **Dependência:** exige a change `add-authentication` implementada (sessão, `AuthenticatedUser`, `RequireAuth`, área interna e envio do token CSRF). A ordem de implementação e de arquivamento é: primeiro `add-authentication`, depois esta.
- **Backend:**
  - novo módulo `profile`;
  - a entidade `User` ganha um método para atualizar só telefone e endereço; os campos imutáveis continuam sem forma de alteração.
- **APIs novas:**
  - `GET /api/profile`;
  - `PUT /api/profile`.
- **Frontend:**
  - página `/perfil`;
  - link "Meu perfil" na área interna;
  - as regras de telefone e endereço do `validation.ts` passam a ser compartilhadas entre o cadastro e o perfil.
- **Banco:** sem migração; as colunas já existem.
