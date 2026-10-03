# Proposal

## Why

O projeto parte do esboço funcional em `docs/sistema_cadastro_esboco.md` e ainda não tem código nem specs. Esta change entrega a primeira fatia funcionando de ponta a ponta: o visitante se cadastra, recebe um e-mail real e ativa a conta. Essa fatia é a base de que login (RF06) e perfil (RF07) dependem, e já roda e é testável localmente por si só.

## What Changes

- Cria a estrutura do monolito modular em Maven multi-módulo, com três módulos integrados num único executável:
  - `database`: Postgres via Docker e migrações Flyway;
  - `backend`: Java 21 e Spring Boot 3, com API REST;
  - `frontend`: React, Vite e TypeScript, servido pelo backend.
- **RF01**: formulário web de cadastro com nome, CPF, e-mail, data de nascimento, senha, telefone e endereço (CEP, logradouro, número, complemento opcional, bairro, cidade, UF).
- **RF02**: validação ao vivo no formulário e revalidação no servidor, que é quem decide:
  - CPF com dígitos verificadores;
  - e-mail em formato válido;
  - data de nascimento não futura;
  - senha forte;
  - telefone fixo (10 dígitos) ou celular (11 dígitos), sempre com DDD;
  - CEP com 8 dígitos;
  - UF válida;
  - campos obrigatórios preenchidos.
- **RF03**: o usuário é gravado com status `PENDENTE` e a senha é guardada como hash.
- **RN03**: e-mail e CPF são únicos. Um cadastro pendente com link expirado é substituído por um novo cadastro com os mesmos dados.
- **RF04**: envio real, via SMTP do Gmail, de um e-mail com link de ativação exclusivo para o endereço cadastrado.
- **RF05 / RN02**: ativação pelo link, que é de uso único e expira em 24 horas. Ao ativar, o status muda para `ATIVO`.
- Testes automatizados no backend (com Postgres real via Testcontainers e SMTP falso) e no frontend.

### Fora do escopo

- Ficam para as próximas changes deste plano:
  - login/logout e RN04: change `add-authentication`;
  - perfil e RN01: change `add-user-profile`.
- Ficam para uma fase posterior de melhorias:
  - reenvio do e-mail de ativação;
  - recuperação e troca de senha;
  - painel administrativo;
  - consulta de CEP em serviço externo.

## Capabilities

### New Capabilities
- `user-registration`: cadastro de um novo usuário (campos, regras de validação, unicidade de e-mail e CPF, gravação como pendente e tratamento de cadastros pendentes expirados).
- `account-activation`: emissão e envio do link de ativação por e-mail, validade e uso único do link, e transição da conta de `PENDENTE` para `ATIVO`.

### Modified Capabilities
<!-- Nenhuma: o projeto ainda não tem specs. -->

## Impact

- **Código**: projeto novo. Cria o parent `pom.xml` e os módulos `database/`, `backend/` e `frontend/`.
- **APIs**:
  - `POST /api/registrations`: cadastro;
  - `POST /api/activations`: ativação.
- **Dependências**:
  - backend: Spring Boot (Web, Validation, Data JPA, Mail, Security apenas para o BCrypt), Spring Modulith, Flyway, PostgreSQL;
  - testes do backend: Testcontainers e GreenMail;
  - frontend: React, Vite e Vitest.
- **Infraestrutura local**:
  - Docker, para rodar o Postgres e os testes;
  - conta Gmail com senha de app, com as credenciais num `.env` que não vai para o git.
