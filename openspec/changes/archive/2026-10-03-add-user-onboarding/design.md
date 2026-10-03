# Design

## Context

Projeto novo: o repositório tem só o esboço (`docs/sistema_cadastro_esboco.md`) e a estrutura do OpenSpec. A motivação está em proposal.md e os requisitos em `specs/user-registration` e `specs/account-activation`.

Restrições:

- Backend em Java, banco Postgres em Docker, frontend em React, comunicação via REST.
- Front, back e banco como módulos separados, integrados num monolito modular.
- Execução apenas local, mas com envio real de e-mail pelo Gmail.
- O código passará por revisão humana, então a legibilidade tem prioridade sobre esperteza.

Esta change também cria a estrutura que as changes `add-authentication` e `add-user-profile` vão reutilizar.

## Goals / Non-Goals

**Goals:**
- Um único comando de build (`mvnw verify`) que compila e testa os três módulos.
- Um único executável que serve a SPA e a API na mesma origem.
- Fronteiras de domínio no backend verificadas por teste, para as próximas changes se encaixarem sem acoplamento.
- Testes de integração com Postgres real e SMTP falso, sem depender do Gmail.

**Non-Goals:**
- Autenticação, sessão, CSRF e autorização de rotas, que ficam para `add-authentication`. Nesta change, a API é toda pública.
- Deploy, imagem Docker da aplicação, HTTPS e observabilidade.
- Internacionalização: as mensagens são só em português.
- Rate limiting e proteção contra abuso do formulário.

## Decisions

### D1. Maven multi-módulo com três módulos

```
demo-sdd/
  pom.xml              parent (Java 21, Spring Boot 3.x BOM)
  mvnw / .mvn/         Maven Wrapper (não exige Maven instalado)
  database/            jar: db/migration/*.sql (Flyway) + docker-compose.yml
  frontend/            React + Vite + TS; o build do Vite vai direto para target/classes/META-INF/resources
  backend/             Spring Boot; depende de database e frontend (jars)
  .env.example         variáveis SMTP e do banco (o .env real fica no .gitignore)
```

- **Por quê:** os três módulos são de fato separados (cada um com build e testes próprios), mas o `backend` reúne tudo num jar executável. É o "monolito modular" pedido.
- **Alternativa descartada:** um repositório com front solto (`npm run dev` com proxy) e back separado. Seriam dois processos e CORS, e deixaria de ser um monolito.
- **Alternativa descartada:** migrações dentro do backend. O banco deixaria de ser um módulo independente.

O frontend entra no Maven via `frontend-maven-plugin`, que instala Node e npm localmente em `frontend/node` (ignorado pelo git, preservado entre `mvn clean`) e roda `npm ci`, `npm test` e `npm run build`. Isso evita exigir Node instalado. Para desenvolvimento do front, `npm run dev` usa o proxy do Vite apontando `/api` para o backend.

### D2. Fronteiras de domínio no backend com Spring Modulith

Pacotes de primeiro nível sob `br.com.demosdd`:

- `registration`: cadastro, validações e unicidade;
- `activation`: tokens, envio de e-mail e ativação;
- `user`: entidade `User` e repositório, compartilhados pelos dois módulos acima e, depois, por `auth` e `profile`;
- `shared`: tratamento de erros da API e utilitários de validação.

Um teste `ApplicationModules.verify()` quebra o build se houver dependência cíclica ou acesso a pacote interno de outro módulo.

- **Comunicação entre módulos:** `registration` publica o evento `UserRegistered`, e `activation` escuta, gera o token e envia o e-mail.
- **Evento síncrono, na mesma transação:** o listener é síncrono e transacional, não usa `@TransactionalEventListener(AFTER_COMMIT)` nem é assíncrono. Assim, uma falha no envio de e-mail desfaz o cadastro, como exige a spec "Cadastro só é concluído com o e-mail enviado".
- **Alternativa descartada:** `registration` chamar `activation` diretamente. Funciona, mas acopla o cadastro ao mecanismo de ativação. O evento deixa `activation` substituível sem tocar no cadastro.

### D3. Modelo de dados

```
users                                   activation_tokens
-------------------------------------   ------------------------------------
id            uuid PK                   id          uuid PK
name          varchar(150)              user_id     uuid FK -> users ON DELETE CASCADE
cpf           varchar(11) UNIQUE        token_hash  varchar(64) UNIQUE (SHA-256 hex)
email         varchar(254) UNIQUE       expires_at  timestamptz
birth_date    date                      used_at     timestamptz NULL
password_hash varchar(100)              created_at  timestamptz
phone         varchar(11)
cep           varchar(8)
street        varchar(200)
number        varchar(20)
complement    varchar(100) NULL
district      varchar(100)
city          varchar(100)
state         varchar(2)
status        varchar(20)  CHECK IN ('PENDENTE','ATIVO')
created_at    timestamptz
activated_at  timestamptz NULL
```

- **`varchar` em vez de `char`:** os campos de tamanho fixo usam `varchar(n)`, porque `char` completa com espaços e não bate com o mapeamento `String` do Hibernate na validação do schema. O tamanho exato é garantido pela validação da aplicação.
- **Formato dos dados gravados:** e-mail em minúsculas, então o índice único simples garante a comparação sem distinção de maiúsculas. CPF, telefone e CEP só com dígitos.
- **Tabela de tokens separada:** permite que a funcionalidade futura de reenvio emita um novo token sem mexer em `users`.
- **Unicidade garantida também pelo banco:** as constraints UNIQUE seguram cadastros simultâneos. Uma violação de constraint vira a mesma resposta de conflito da verificação feita antes de gravar.

### D4. Regras de unicidade e substituição

O fluxo, numa única transação:

1. Busca usuários com o mesmo e-mail ou o mesmo CPF.
2. Se algum deles estiver `ATIVO`, ou `PENDENTE` com token válido, recusa com 409. A resposta lista o campo em conflito: `email` e/ou `cpf`.
3. Se todos estiverem `PENDENTE` com token expirado, remove esses usuários (o cascade apaga os tokens), executa um `flush` e grava o novo usuário.

Esse fluxo atende a spec "Substituição de cadastro pendente expirado" sem job agendado. Os pendentes expirados que ninguém reaproveita continuam no banco, o que é aceitável numa demo local.

Para testar expiração sem esperar 24h, o relógio é injetado via `java.time.Clock`.

### D5. Token de ativação

- **Geração:** 32 bytes de `SecureRandom`, codificados em Base64 URL-safe sem padding (43 caracteres). O banco guarda só o SHA-256 em hex.
- **Por que SHA-256 e não BCrypt:** o token tem alta entropia, então um hash rápido basta, e permite busca indexada por igualdade.
- **Validade:** 24h. O valor fica configurável em `app.activation.token-ttl=PT24H`.
- **Uso único:** `used_at` é preenchido na ativação. Um uso posterior é recusado com o motivo "já utilizado".

### D6. Contrato REST

Todas as respostas usam JSON. Os erros seguem o formato `ProblemDetail` (RFC 9457) do Spring, com extensões:

```json
{ "type": "about:blank", "title": "Dados inválidos", "status": 400,
  "code": "VALIDATION_ERROR",
  "errors": [ { "field": "cpf", "message": "CPF inválido" } ] }
```

| Endpoint | Sucesso | Erros (`code`) |
|---|---|---|
| `POST /api/registrations` | 201, `{ "email": "..." }` | 400 `VALIDATION_ERROR`; 409 `ALREADY_REGISTERED` (com `errors[]` por campo); 409 `PENDING_ACTIVATION`; 503 `EMAIL_UNAVAILABLE` |
| `POST /api/activations` body `{ "token": "..." }` | 200 | 400 `INVALID_TOKEN`; 410 `TOKEN_EXPIRED`; 409 `TOKEN_ALREADY_USED` |

- **`PENDING_ACTIVATION`:** quando o conflito é com um pendente de token válido, o código é próprio, para o front mostrar "verifique seu e-mail".
- **Ativação por `POST`:** um `GET` em `/ativar?token=...` só carrega a SPA. A página mostra o botão "Ativar minha conta", e é o clique que envia o `POST`. Isso atende a spec "Ativação exige ação do usuário".
- **Alternativa descartada:** a página enviar o `POST` sozinha ao carregar. Seria um clique a menos, mas um verificador de e-mail que executa JavaScript ativaria a conta sem o usuário.

### D7. Validação

**Backend:**
- Bean Validation no DTO, com anotações próprias: `@Cpf`, `@StrongPassword`, `@Phone`, `@Cep`, `@Uf`.
- `@Phone` aceita 10 dígitos (fixo) ou 11 (celular), sem regras extras sobre DDD ou primeiro dígito, para manter a regra simples e alinhada à spec.
- Normalização (trim, minúsculas no e-mail, só dígitos em CPF, telefone e CEP) feita antes de validar, num passo explícito do serviço.
- Todos os erros de campo voltam juntos.

**Frontend:**
- Regras espelhadas num módulo TypeScript puro (`validation.ts`), testado com Vitest usando os mesmos exemplos das specs.
- A validação roda ao sair de cada campo, e a cada tecla depois que o campo já mostrou erro.
- Um checklist ao vivo mostra os critérios de força da senha.
- A máscara de telefone se adapta à quantidade de dígitos: `(00) 0000-0000` para fixo e `(00) 00000-0000` para celular.
- Os erros 400 e 409 do servidor aparecem nos campos correspondentes.

**Alternativa descartada:** gerar as regras do front a partir do back, por exemplo via OpenAPI. Isso é complexidade demais para cinco validadores. A duplicação é intencional e coberta por testes dos dois lados.

### D8. E-mail

- **Envio:** `spring-boot-starter-mail` com SMTP do Gmail (`smtp.gmail.com:587`, STARTTLS). As variáveis `MAIL_USERNAME` e `MAIL_PASSWORD` (senha de app) e `APP_BASE_URL` (padrão `http://localhost:8080`) vêm do `.env`. O `docker compose` e a aplicação leem o mesmo `.env`; a aplicação faz isso via `spring.config.import=optional:file:.env[.properties]`.
- **Formato:** texto simples mais HTML mínimo, com link para `${APP_BASE_URL}/ativar?token=<token>` e o aviso de validade de 24h.
- **Envio síncrono:** uma `MailException` vira 503 `EMAIL_UNAVAILABLE` e faz rollback.
- **Testes:** GreenMail como SMTP embutido, verificando o destinatário e extraindo o link do corpo para seguir o fluxo de ativação.

### D9. Frontend

React 19, Vite, TypeScript e React Router, sem biblioteca de UI. A versão é a atual no momento da implementação; o plano original previa React 18. CSS próprio, simples e responsivo.

- **Rotas:**
  - `/cadastro`: formulário;
  - `/cadastro/sucesso`: aviso "verifique seu e-mail";
  - `/ativar`: confirmação e resultado da ativação;
  - `/`: redireciona para `/cadastro` nesta change.
- **Formulário:** estado local com `useState` e um hook `useFormValidation`. Não usa React Hook Form, para manter o código fácil de revisar.
- **SPA servida pelo Spring:** um controller encaminha para `index.html` toda rota que não seja `/api/**` nem arquivo estático.
- **Testes:** React Testing Library cobre a renderização do formulário, os erros nos campos, o checklist de senha e os três resultados da ativação, com `fetch` mockado.

### D10. Estratégia de testes

| Nível | Ferramenta | Cobre |
|---|---|---|
| Unidade (back) | JUnit 5 + AssertJ | validadores (CPF, senha, telefone, CEP, UF), normalização, geração e hash do token |
| Integração (back) | `@SpringBootTest` + Testcontainers Postgres + GreenMail + MockMvc | cada cenário das duas specs pela API, inclusive substituição de pendente expirado e falha de SMTP |
| Arquitetura | Spring Modulith `verify()` | fronteiras entre módulos |
| Unidade (front) | Vitest | `validation.ts` |
| Componente (front) | Vitest + React Testing Library | páginas de cadastro e ativação |

Cada cenário das specs vira ao menos um teste com nome rastreável, por exemplo `deveRejeitarCpfComDigitoVerificadorIncorreto`.

## Risks / Trade-offs

- **Gmail bloqueia ou limita o envio, ou a senha de app está ausente.** → O cadastro devolve 503 com mensagem clara, e os testes automatizados não dependem do Gmail. O README documenta como gerar a senha de app.
- **O envio síncrono deixa o `POST /api/registrations` lento (1 a 3 s).** → Aceitável localmente. Em troca, a consistência é simples: não há usuário sem e-mail. O front mostra estado de carregamento.
- **E-mail enviado mas commit falha (janela pequena).** → O usuário recebe um link inválido e pode se cadastrar de novo. Esse risco é aceito.
- **A resposta 409 por campo revela se um e-mail ou CPF já está cadastrado.** → É exigido pela RN03 e aceitável numa demo local. Fica registrado para revisão se o sistema evoluir.
- **Regras duplicadas no front e no back podem divergir.** → Os dois lados são testados com os mesmos exemplos das specs, e o back é sempre quem decide.
- **Testcontainers exige Docker rodando para `mvnw verify`.** → É a mesma exigência do banco local, documentada no README.
- **Primeiro build lento,** porque o `frontend-maven-plugin` baixa o Node. → Acontece uma vez só; depois fica em cache em `frontend/node`.
- **Antivírus com inspeção de TLS (ex.: Avast Web/Mail Shield) quebra o Maven e o SMTP,** porque a JDK não confia no certificado raiz do antivírus. → O README documenta como usar o repositório de certificados do Windows (`-Djavax.net.ssl.trustStoreType=Windows-ROOT`). A configuração não vai para o repositório, porque é específica da máquina.
- **Pendentes expirados não reaproveitados acumulam no banco.** → Impacto irrelevante localmente. Uma limpeza agendada pode entrar junto com as melhorias.

## Migration Plan

Não se aplica: é um projeto novo. Para rodar localmente:

1. `cp .env.example .env` e preencher as credenciais do Gmail.
2. `docker compose -f database/docker-compose.yml up -d`.
3. `./mvnw verify` para build e testes.
4. `java -jar backend/target/backend.jar`, e acessar `http://localhost:8080`.

O Flyway aplica as migrações ao subir a aplicação.

## Open Questions

Nenhuma no momento.
