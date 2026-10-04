# Análise técnica do projeto — branch `develop/demo_openspec`

> Data: 04/10/2026 · Escopo: todo o código da branch (backend Spring Boot, frontend React, migrações Flyway, testes).
> Base: leitura integral do código de produção, amostragem dos testes, inspeção do bytecode do `spring-security-crypto` 6.5.11 e execução da suíte completa (`mvnw verify`).

## Resumo das notas

| Critério | Nota (1–5) | Síntese |
|---|:---:|---|
| 1. Arquitetura e Design | **4** | Monolito modular com fronteiras verificadas por teste; o envio síncrono do e-mail dentro da transação e a sessão em memória são as principais limitações. |
| 2. Qualidade do Código | **4** | Código claro, idiomático e com boa suíte de testes de integração; faltam medição de cobertura, análise estática e um tratamento genérico de exceções. Há um bug real com senhas acima de 72 bytes. |
| 3. Segurança | **4** | Fundamentos corretos (BCrypt, sessão com CSRF, sem SQL Injection, tokens com hash, proteção contra IDOR); falta proteção contra abuso (rate limiting) nos endpoints públicos, e isso precisa ser resolvido antes de produção. |

---

## 1. Arquitetura e Design — nota 4

### Estrutura

```
demo-sdd (pom agregador)
├── database/   migrações Flyway (V1) + docker-compose do Postgres
├── frontend/   SPA React + Vite, empacotada em META-INF/resources do jar
└── backend/    Spring Boot 3.5 / Java 21, serve a API e a SPA
    └── br.com.demosdd
        ├── registration/  cadastro (RF01–RF03, RN03)
        ├── activation/    tokens e e-mail de ativação (RF04, RF05, RN02)
        ├── auth/          login, /me (RF06, RN04)
        ├── profile/       consulta/edição de contato (RF07, RN01)
        ├── user/          entidade User/Address + repositório
        └── shared/        segurança, validação, formato de erro, relógio
```

### Pontos fortes

- **Organização por funcionalidade, não por camada técnica.** Cada módulo contém seu controller, service, DTOs e repositório. Controllers e services são *package-private*, então só o necessário fica exposto.
- **Fronteiras verificadas automaticamente.** O [ModularityTest.java](backend/src/test/java/br/com/demosdd/ModularityTest.java) usa Spring Modulith (`modules.verify()`) e ArchUnit para garantir que não há ciclos e que `shared` não depende de módulos de domínio. `profile` declara explicitamente suas dependências permitidas ([package-info.java](backend/src/main/java/br/com/demosdd/profile/package-info.java)).
- **Desacoplamento por evento e por porta.** O cadastro publica `UserRegistered` e o módulo de ativação reage ([UserRegisteredListener.java](backend/src/main/java/br/com/demosdd/activation/UserRegisteredListener.java)). A regra RN03 consulta a ativação por uma interface (`ActivationLinkChecker`) definida no próprio cadastro: inversão de dependência aplicada corretamente.
- **Separação controller → service → repository respeitada.** Controllers são finos. A regra de negócio fica nos services e no domínio (`User.activate()` valida a transição de estado). DTOs (`record`) separam o contrato HTTP da entidade JPA.
- **Banco versionado** com Flyway e `ddl-auto: validate`. O schema é a fonte da verdade e as constraints únicas (`uk_users_cpf`, `uk_users_email`) garantem a unicidade mesmo sob concorrência.
- **Formato de erro único** (RFC 9457 `ProblemDetail` com `code` estável), aplicado também às recusas da camada de segurança ([ProblemDetailSecurityHandler.java](backend/src/main/java/br/com/demosdd/shared/security/ProblemDetailSecurityHandler.java)).
- **Relógio injetável** (`Clock`), inclusive no `@PastOrPresent`, o que torna as regras de expiração testáveis de forma determinística.
- **Rastreabilidade:** specs OpenSpec em `openspec/specs/` e Javadoc referenciando RF/RN/decisões de design.

### Riscos e pontos de melhoria

| # | Severidade | Ponto | Detalhe |
|---|---|---|---|
| A1 | Média | **SMTP síncrono dentro da transação do banco** | [RegistrationService.java](backend/src/main/java/br/com/demosdd/registration/RegistrationService.java) grava o usuário, publica o evento e o listener envia o e-mail na mesma transação. A conexão JDBC fica presa enquanto o SMTP responde (timeouts de até 10 s × 3). Sob carga, ou com o Gmail lento, o pool do Hikari (10 conexões por padrão) se esgota. É uma escolha consciente ("cadastro só conclui com e-mail enviado"), mas o padrão *transactional outbox* (Spring Modulith já oferece *Event Publication Registry*) daria a mesma garantia sem acoplar a disponibilidade do banco à do SMTP. |
| A2 | Baixa | **Sessão em memória** | Não escala horizontalmente e se perde a cada reinício (o próprio `application.yml` registra isso). Para mais de uma instância: Spring Session JDBC/Redis. |
| A3 | Baixa | **Acesso direto ao repositório no controller** | [AuthController.java:47](backend/src/main/java/br/com/demosdd/auth/AuthController.java#L47) usa `UserRepository` diretamente em `/me`, enquanto o restante passa por services. É uma inconsistência pequena. |
| A4 | Baixa | **Entidade atravessando a fronteira do service** | `RegistrationService.register()` e `ActivationService.activate()` devolvem a entidade `User` ao controller. Hoje só o e-mail é lido, mas o padrão abre espaço para vazamento acidental de campos (ex.: `passwordHash`) em respostas futuras. |
| A5 | Baixa | **Dependência de mensagem do driver** | `fieldFromConstraint` decide entre CPF e e-mail procurando `uk_users_cpf` no texto da exceção do Postgres. Funciona, mas é frágil a mudanças de driver ou de idioma da mensagem. |

---

## 2. Qualidade do Código — nota 4

### Pontos fortes

- **Legibilidade alta e Java moderno:** `record` para DTOs, construtores compactos fazendo a normalização (`InputNormalizer`) antes da validação, injeção por construtor, imutabilidade, pattern matching com `instanceof`. Nomes claros e coerentes entre backend e frontend.
- **Validação declarativa e reutilizável:** anotações próprias (`@Cpf`, `@Phone`, `@Cep`, `@Uf`, `@StrongPassword`) compartilhadas por cadastro e perfil. A política de senha conta *code points* (correto para Unicode) e devolve todos os critérios não atendidos.
- **Tratamento de exceções consistente:** `ApiException` com status, código, título e erros por campo, e um `@RestControllerAdvice` que também converte JSON malformado e formatos inválidos em respostas 400 legíveis.
- **Comentários úteis:** explicam o *porquê* (ex.: o `dummyHash` para igualar o tempo de resposta, o motivo do `SpaCsrfTokenRequestHandler`), não o *o quê*.
- **Frontend tipado e simples:** cliente HTTP centralizado ([api.ts](frontend/src/api.ts)) com resultado discriminado (`ApiResult<T>`), sem estado global desnecessário e sem `dangerouslySetInnerHTML`.

### Testes

| Camada | Arquivos | Casos | Natureza |
|---|---|---|---|
| Backend (JUnit 5) | 19 classes | 119 (98 métodos `@Test`, mais casos parametrizados) | Integração com **Postgres real (Testcontainers)**, **SMTP falso (GreenMail)** e relógio controlável; unitários para validadores, normalização, token e entidade; teste de migração Flyway; teste de modularidade; teste HTTP real do cookie de sessão. |
| Frontend (Vitest + Testing Library) | 8 arquivos | 102 | Páginas (cadastro, login, ativação, home, perfil), cliente da API (CSRF, erros), máscaras e validação. |

Resultado da execução de `mvnw verify` nesta análise: **BUILD SUCCESS**: backend com 119 testes (0 falhas, 0 erros, 0 ignorados) e frontend com 102 testes em 8 arquivos, todos passando.

Os testes são orientados a cenários das specs (nomes como `emailComCaixaDiferenteEEspacosAutentica`) e cobrem os caminhos de erro: token expirado/usado, conta pendente, CSRF ausente, campos imutáveis no perfil, conflitos de unicidade. O caminho de corrida entre dois cadastros simultâneos (captura de `DataIntegrityViolationException`) não tem teste dedicado.

### Riscos e pontos de melhoria

| # | Severidade | Ponto | Detalhe |
|---|---|---|---|
| Q1 | **Média (bug)** | **Senha válida com mais de 72 bytes gera erro 500 no cadastro** | A política aceita até 64 *caracteres*, mas o BCrypt do Spring Security 6.5.11 lança `IllegalArgumentException("password cannot be more than 72 bytes")` no `encode` (confirmado no bytecode de `BCrypt.hashpw`). Uma senha de 40 caracteres com acentos (ex.: `Ááááá…1!`, 2 bytes por letra em UTF-8) passa na validação e quebra em `passwordEncoder.encode()`. Sem handler genérico, o usuário recebe a página de erro padrão em vez de um `ProblemDetail`. **Correção:** validar também `password.getBytes(UTF_8).length <= 72` na `PasswordPolicy` (e no frontend), ou adotar Argon2 (`Argon2PasswordEncoder`), que não tem esse limite. Adicionar um teste com senha multibyte. |
| Q2 | Média | **Sem handler genérico para exceções inesperadas** | `ApiExceptionHandler` não trata `Exception`/`RuntimeException`. Erros não previstos (como Q1, ou o `IllegalStateException` de token sem usuário) saem no formato do `BasicErrorController` e não em `ProblemDetail`, e o frontend cai na mensagem genérica. Convém adicionar um `@ExceptionHandler(Exception.class)` que registre o erro em log e devolva 500 com `code: INTERNAL_ERROR`. |
| Q3 | Média | **Cobertura não é medida** | Não há JaCoCo no `pom.xml` nem `vitest --coverage`. A suíte parece abrangente, mas não há número nem *quality gate* para evitar regressão de cobertura. |
| Q4 | Baixa | **Sem análise estática ou lint** | Não há Checkstyle/SpotBugs/Error Prone no backend nem ESLint no frontend. O código hoje é consistente por disciplina, não por ferramenta. |
| Q5 | Baixa | **Sem controle de concorrência otimista** | `User` e `ActivationToken` não têm `@Version`. Duas ativações simultâneas com o mesmo token podem passar pelas verificações. O efeito é benigno (a conta fica ativa), mas duas edições concorrentes de perfil se sobrescrevem silenciosamente (*last write wins*). |
| Q6 | Baixa | **Logs com dado pessoal** | `UserRegisteredListener` registra o e-mail do usuário em `WARN`. Pela LGPD, prefira mascarar (`m***@exemplo.com`) ou registrar o id do usuário. |

---

## 3. Segurança — nota 4

### Checklist das vulnerabilidades típicas de sistemas de cadastro

| Item | Situação | Evidência |
|---|:---:|---|
| Hash de senha forte (bcrypt/argon2) | ✅ (com ressalva Q1) | `BCryptPasswordEncoder` (custo padrão 10) em [PasswordEncoderConfig.java](backend/src/main/java/br/com/demosdd/shared/security/PasswordEncoderConfig.java). A senha nunca é logada nem devolvida. |
| Política de senha forte | ✅ | 8–64 caracteres, maiúscula, minúscula, dígito e especial; validada no backend e no frontend. |
| SQL Injection | ✅ | Só consultas derivadas do Spring Data JPA (`findByEmail`, `findByEmailOrCpf`, …), todas parametrizadas. Não há SQL concatenado, `@Query` nativa ou `JdbcTemplate` no código de produção. |
| NoSQL Injection | N/A | Não há banco NoSQL. |
| Validação e sanitização de entrada | ✅ | Bean Validation em todos os DTOs, com tamanhos máximos alinhados às colunas; normalização remove apenas máscaras, e qualquer outro caractere faz a validação falhar. `@Valid` presente nos endpoints com corpo. |
| XSS | ✅ | React escapa por padrão e não há `dangerouslySetInnerHTML`/`innerHTML`. No e-mail HTML, o nome passa por `HtmlUtils.htmlEscape`. |
| Mass assignment / alteração de campos imutáveis | ✅ | `ProfileUpdateRequest` declara nome, CPF, e-mail e nascimento com `@Null`, então qualquer valor enviado é recusado (RN01). |
| IDOR (acesso a dados de outro usuário) | ✅ | `/api/profile` não recebe id: usa sempre o principal da sessão. |
| Sessão | ✅ | Sessão no servidor, cookie `HttpOnly` + `SameSite=Lax`, rastreamento só por cookie (sem `;jsessionid` na URL), expiração de 30 min por inatividade, **troca do id de sessão no login** (anti *session fixation*), logout invalida a sessão e apaga o cookie. |
| CSRF | ✅ | Padrão SPA (cookie `XSRF-TOKEN` + cabeçalho `X-XSRF-TOKEN`), exigido inclusive no login (protege contra *login CSRF*). O token é renovado no login. |
| JWT | N/A (escolha adequada) | Não usa JWT. Para uma SPA servida na mesma origem, sessão + cookie `HttpOnly` é a opção mais segura: permite revogação imediata no logout e não expõe token ao JavaScript. |
| Enumeração de usuários no login | ✅ | Mensagem única "E-mail ou senha inválidos"; `dummyHash` iguala o tempo quando o e-mail não existe; "conta pendente" só aparece para quem acertou a senha. |
| Token de ativação | ✅ | 256 bits de `SecureRandom`, só o SHA-256 é gravado, uso único, expira em 24 h; abrir o link (GET) não ativa a conta, só o POST ativa (evita ativação por *scanners* de link). |
| Segredos fora do repositório | ✅ | `.env` no `.gitignore`; `.env.example` sem valores reais. |
| Rate limiting / força bruta | ❌ | Ver S1. |
| Cookie `Secure` / HTTPS | ⚠️ | Ver S4. |

### Riscos identificados (por prioridade)

| # | Severidade | Risco | Detalhe e recomendação |
|---|---|---|---|
| S1 | **Alta** | **Sem limite de tentativas nos endpoints públicos** | `POST /api/auth/login` aceita tentativas ilimitadas (força bruta e *credential stuffing*). `POST /api/registrations` envia um e-mail real pela conta Gmail para **qualquer endereço informado**, sem limite nem CAPTCHA: um atacante consegue usar o sistema para *e-mail bombing* contra terceiros, levar a conta Gmail a ser suspensa por spam e, combinado com A1, esgotar o pool de conexões. **Recomendação (urgente antes de produção):** rate limiting por IP e por e-mail (Bucket4j, ou no proxy/API gateway), bloqueio progressivo ou *backoff* após N falhas de login, e CAPTCHA no cadastro. |
| S2 | Média | **Enumeração de e-mail e CPF pelo cadastro** | O cadastro responde "E-mail já cadastrado", "CPF já cadastrado" ou "aguardando ativação". Isso decorre da RN03 e da UX pedida, mas permite descobrir se um CPF (dado pessoal sensível pela LGPD) está na base. Com S1 sem mitigação, a enumeração em massa é viável. Mitigar com rate limiting; se o requisito permitir, responder de forma neutra ("Se os dados forem válidos, você receberá um e-mail") e avisar o titular por e-mail. |
| S3 | Média | **"Sequestro" de CPF** | Nada vincula o CPF ao titular. Alguém pode cadastrar o CPF de outra pessoa com o próprio e-mail, ativar a conta, e o titular real fica permanentemente impedido de se cadastrar (RN03), sem fluxo de contestação. Mesmo sem ativar, um cadastro pendente bloqueia o CPF por 24 h e pode ser renovado indefinidamente. É risco de negócio: avaliar validação do CPF junto a uma fonte oficial ou um processo de suporte/contestação. |
| S4 | Média (configuração) | **Cookie de sessão sem `Secure` por padrão; sem HSTS explícito** | `SESSION_COOKIE_SECURE` tem `false` como padrão, e o cookie `XSRF-TOKEN` não tem `Secure`. É razoável para desenvolvimento local, mas é fácil esquecer em produção. Recomendação: perfil `prod` com `secure: true` por padrão, TLS obrigatório, `server.forward-headers-strategy` atrás de proxy, e HSTS (o Spring envia HSTS apenas em requisições HTTPS). |
| S5 | Média | **Erro 500 com senha > 72 bytes** | Ver Q1. Além do bug funcional, gera erro não tratado em endpoint público. |
| S6 | Baixa | **Ausência de Content-Security-Policy** | Os cabeçalhos padrão do Spring Security estão ativos (`X-Content-Type-Options`, `X-Frame-Options: DENY`, `Cache-Control`), mas não há CSP. Como a SPA não carrega recursos de terceiros, uma CSP restritiva (`default-src 'self'`) é simples de adicionar e reduz o impacto de qualquer XSS futuro. |
| S7 | Baixa | **Sem troca/recuperação de senha e sem invalidação de outras sessões** | Não existe forma de trocar uma senha comprometida (o esboço lista "Esqueci minha senha" como recurso opcional). Também não há limite de sessões simultâneas nem tempo máximo absoluto de sessão (só inatividade). |
| S8 | Baixa | **Custo do BCrypt no padrão (10)** | Aceitável, mas a recomendação atual da OWASP é custo ≥ 10, e o ideal é calibrar para ~250 ms no hardware de produção (geralmente 12). Alternativa moderna: Argon2id, que também resolve Q1. |
| S9 | Baixa | **Credenciais padrão do banco** | `application.yml` tem `demosdd/demosdd` como valores padrão. Em produção, a ausência da variável de ambiente deveria fazer a aplicação falhar ao subir, não usar um padrão. |
| S10 | Info | **Sem varredura de dependências** | Não há OWASP Dependency-Check, `npm audit` no build nem Dependabot/Renovate configurados. |

### O que verificar com urgência

1. **S1:** implementar rate limiting no login e no cadastro, e CAPTCHA no cadastro.
2. **Q1/S5:** limitar a senha a 72 bytes UTF-8 (ou migrar para Argon2id) e adicionar um teste com senha multibyte.
3. **S4:** criar um perfil de produção com cookies `Secure`, HTTPS e HSTS, sem credenciais padrão.
4. **Q2:** adicionar handler genérico de exceções no formato `ProblemDetail`.
5. **S2/S3:** decidir com o negócio como tratar enumeração e o uso indevido de CPF de terceiros.

---

## Conclusão

O projeto está acima da média para um sistema de cadastro. A arquitetura modular é verificada por testes, o código é limpo e rastreável até os requisitos, a suíte de integração roda contra infraestrutura real, e os fundamentos de segurança (hash de senha, sessão, CSRF, ausência de injeção, tokens de ativação, proteção contra IDOR e enumeração no login) foram implementados corretamente e com atenção a detalhes que costumam ser esquecidos, como o tempo de resposta constante, a troca do id de sessão e o CSRF no login.

O que impede notas máximas é a **ausência de proteção contra abuso dos endpoints públicos** (S1), que é o risco mais urgente; um **bug real na fronteira de 72 bytes do BCrypt** (Q1), que nenhum teste captura; e a falta de **ferramentas de qualidade automatizadas** (cobertura, lint, análise estática e de dependências).
