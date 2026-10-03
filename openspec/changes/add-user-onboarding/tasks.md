# Tasks

> Cada grupo termina com um commit próprio. A mensagem segue o padrão `tipo(escopo): descrição` e cita os requisitos (ex.: `feat(registration): RF02 validação de CPF`).

## 1. Estrutura do monolito modular

- [x] 1.1 Criar o parent `pom.xml` (Java 21, BOM do Spring Boot 3.x, módulos `database`, `frontend` e `backend`) e o Maven Wrapper. Verificar com `./mvnw -v`, que deve rodar sem Maven instalado.
- [x] 1.2 Criar o módulo `database` com `docker-compose.yml` (Postgres 16, porta 5432, credenciais lidas do `.env`) e o pacote de migrações `db/migration` vazio. Verificar que `docker compose -f database/docker-compose.yml up -d` sobe o banco, e que `docker compose ... ps` o mostra saudável.
- [x] 1.3 Criar o módulo `frontend` (Vite, React 19, TypeScript, React Router e Vitest), com uma página placeholder e o `frontend-maven-plugin` gerando o build em `META-INF/resources`. Verificar com `./mvnw -pl frontend package`, que deve gerar um jar contendo `index.html`.
- [x] 1.4 Criar o módulo `backend` (Spring Boot: Web, Validation, Data JPA, Mail, Flyway, PostgreSQL, Spring Modulith e `spring-security-crypto`) dependendo de `database` e `frontend`. Incluir o forward da SPA (rotas fora de `/api/**` vão para `index.html`). Verificar com `./mvnw package`, depois `java -jar` e `http://localhost:8080`, que deve mostrar o placeholder.
- [x] 1.5 Adicionar `.env.example` (variáveis do banco, `MAIL_USERNAME`, `MAIL_PASSWORD` e `APP_BASE_URL`), o `.gitignore` (com `.env`, `target/`, `node_modules/`) e a importação do `.env` no `application.yml`. Verificar que `git status` não lista o `.env`.
- [x] 1.6 Configurar a base de testes do backend (Testcontainers Postgres, GreenMail, `Clock` injetável) e um teste `ApplicationModules.verify()`. Verificar com `./mvnw verify`, que deve passar com o teste de modularidade.

## 2. Modelo de dados

- [x] 2.1 Criar a migração `V1__create_users_and_activation_tokens.sql` com as tabelas, constraints UNIQUE e CHECK e FK com cascade do design D3. Verificar com o teste de integração que sobe o contexto e confere que o Flyway aplicou a V1.
- [x] 2.2 Criar no módulo `user` a entidade `User` com o enum `UserStatus` (`PENDENTE`, `ATIVO`) e o repositório, com buscas por e-mail ou CPF. Verificar com um teste de repositório que grava e lê um usuário e confirma a violação de unicidade de e-mail e de CPF.

## 3. Validação e normalização (backend)

- [x] 3.1 Implementar a normalização: trim, e-mail em minúsculas, só dígitos em CPF, telefone e CEP, e campo só com espaços tratado como vazio. Verificar com testes unitários cobrindo os exemplos das specs (` Maria@Exemplo.com `, `529.982.247-25`, `(11) 98765-4321`, `01310-100`).
- [x] 3.2 Implementar o validador `@Cpf`. Verificar com testes unitários: `52998224725` aceito; `52998224726` e `11111111111` recusados; tamanho diferente de 11 recusado.
- [x] 3.3 Implementar o validador `@StrongPassword` (8 a 64 caracteres, maiúscula, minúscula, dígito e especial, com mensagem por critério). Verificar com testes unitários: `Segura@123` aceita; `Segura1234` recusa por falta de especial; `Se@1` recusa por tamanho.
- [x] 3.4 Implementar os validadores `@Phone` (10 ou 11 dígitos), `@Cep` e `@Uf` e a regra de data de nascimento não futura (usando o `Clock`). Verificar com testes unitários que cobrem os cenários das specs: `11987654321` e `1134567890` aceitos, 9 e 12 dígitos recusados, a data de hoje aceita e amanhã recusada.
- [x] 3.5 Criar o DTO de cadastro com as anotações, o tratamento global de erros em `ProblemDetail` (`code` + `errors[]`) e o formato de erro do design D6. Verificar com um teste MockMvc de payload inválido, que deve retornar 400 `VALIDATION_ERROR` listando todos os campos inválidos.

## 4. Cadastro e unicidade (backend)

- [x] 4.1 Implementar `POST /api/registrations` no módulo `registration`: normaliza, valida, grava como `PENDENTE` com hash BCrypt e publica `UserRegistered`. Verificar com um teste de integração: retorna 201, o usuário fica gravado `PENDENTE` e o `password_hash` é diferente da senha.
- [x] 4.2 Implementar as regras de conflito do design D4: 409 `ALREADY_REGISTERED` por campo para e-mail ou CPF de usuário `ATIVO` (com comparação de e-mail sem distinção de caixa), e 409 `PENDING_ACTIVATION` para pendente com token válido. Converter violação de constraint em 409. Verificar com testes de integração, um por cenário de "Unicidade de e-mail e CPF".
- [x] 4.3 Implementar a substituição de pendentes expirados. Verificar com testes de integração (avançando o `Clock`): recadastro após expiração aceito, com o antigo removido e novo e-mail enviado; conflito misto (pendente expirado + CPF ativo) recusado, com o pendente mantido.
- [x] 4.4 Verificar a fronteira de modularidade após os grupos 2 a 4. `ApplicationModules.verify()` deve passar sem `registration` depender de pacotes internos de `activation`.

## 5. Ativação e e-mail (backend)

- [x] 5.1 Implementar no módulo `activation` a geração do token (32 bytes de `SecureRandom`, Base64 URL-safe), o hash SHA-256 e a gravação com `expires_at` = agora + `app.activation.token-ttl`. Verificar com testes unitários: tokens distintos, hash determinístico e token puro nunca gravado.
- [x] 5.2 Implementar o listener síncrono de `UserRegistered`, que gera o token e envia o e-mail (texto e HTML, link `${APP_BASE_URL}/ativar?token=...`, aviso de 24h). Verificar com um teste de integração via GreenMail: e-mail entregue ao endereço cadastrado, com o link e o aviso de validade.
- [x] 5.3 Fazer uma falha de SMTP desfazer o cadastro com 503 `EMAIL_UNAVAILABLE`. Verificar com um teste de integração com GreenMail parado ou com o `JavaMailSender` falhando: 503 retornado e nenhum usuário gravado.
- [x] 5.4 Implementar `POST /api/activations`: token válido marca `used_at`, muda o status para `ATIVO` e preenche `activated_at`. Recusas: 400 `INVALID_TOKEN`, 410 `TOKEN_EXPIRED` (conta continua `PENDENTE`) e 409 `TOKEN_ALREADY_USED`. Verificar com testes de integração, um por cenário da spec `account-activation` (inclusive 23h aceito e mais de 24h recusado via `Clock`, e o token de um usuário não ativando outro).
- [x] 5.5 Verificar que um `GET /ativar?token=...` devolve só a SPA e não altera o usuário nem o token. Fazer isso com um teste de integração do cenário "Link apenas aberto".
- [x] 5.6 Escrever o teste de fluxo completo no backend: cadastro, e-mail no GreenMail, extração do link e ativação via API. Verificar que o usuário termina `ATIVO`.

## 6. Frontend: cadastro

- [x] 6.1 Implementar `validation.ts` espelhando as regras do backend (CPF, e-mail, data, senha por critério, telefone, CEP, UF e obrigatórios). Verificar com testes Vitest usando os mesmos exemplos das specs.
- [x] 6.2 Implementar o cliente da API (`api.ts`), que trata `ProblemDetail` e mapeia `errors[]` para os campos. Verificar com testes Vitest e `fetch` mockado, cobrindo 201, 400, 409 por campo, 409 `PENDING_ACTIVATION` e 503.
- [x] 6.3 Implementar a página `/cadastro`: formulário com máscaras de CPF, telefone (adaptando-se a fixo ou celular) e CEP, select de UF, complemento marcado como opcional, validação ao sair do campo e a cada tecla depois do primeiro erro, checklist de senha ao vivo e estado de carregamento. Verificar com testes React Testing Library: CPF inválido mostra erro ao sair do campo; checklist destaca o especial pendente; submit bloqueado com erros.
- [x] 6.4 Exibir os erros do servidor nos campos e as mensagens gerais (pendente de ativação, serviço de e-mail indisponível), e redirecionar para `/cadastro/sucesso` com o e-mail informado. Verificar com testes React Testing Library cobrindo cada resposta.

## 7. Frontend: ativação

- [x] 7.1 Implementar a página `/ativar`: lê o `token` da URL, mostra o botão "Ativar minha conta" (sem chamada automática) e, após o clique, exibe sucesso, link expirado (com orientação para novo cadastro), link já utilizado ou link inválido. Verificar com testes React Testing Library: nenhuma chamada antes do clique e uma mensagem para cada resultado.
- [x] 7.2 Ajustar a rota `/` para redirecionar a `/cadastro` e tratar `/ativar` sem token como link inválido. Verificar com testes React Testing Library.

## 8. Documentação e verificação integrada

- [ ] 8.1 Escrever o `README.md` com pré-requisitos (JDK 21, Docker), geração da senha de app do Gmail, preenchimento do `.env`, os comandos de execução e testes, as rotas e o desenvolvimento do front com `npm run dev`. Verificar seguindo o README do zero, num clone limpo, até a aplicação no ar.
- [ ] 8.2 Rodar `./mvnw verify` completo (front + back + modularidade). Verificar que termina com BUILD SUCCESS e sem testes ignorados.
- [ ] 8.3 Fazer o teste manual ponta a ponta com Gmail real: cadastrar com um e-mail próprio, receber o e-mail, clicar no link, confirmar e ver a conta `ATIVO` no banco (`select status from users`). Repetir o mesmo link e ver "já utilizado". Registrar o resultado no PR.
