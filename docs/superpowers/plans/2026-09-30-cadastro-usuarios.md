# Sistema de Cadastro de Usuários — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Objetivo:** Implementar o cadastro de usuários (RF01–RF07, RN01–RN04) como monolito modular: backend Java/Spring, frontend React e PostgreSQL, com envio real do e-mail de ativação pelo Gmail.

**Arquitetura:** Maven multi-módulo (`database`, `frontend`, `backend`). O build gera um único JAR, que serve a SPA React e a API REST `/api/**`. O backend é dividido em módulos de domínio do Spring Modulith (`usuario`, `cadastro`, `ativacao`, `autenticacao`, `perfil`, `shared`). O cadastro e o envio de e-mail se comunicam pelo evento `UsuarioCadastrado`, tratado depois do commit. A autenticação usa sessão no servidor com cookie e CSRF.

**Stack:** Java 21, Spring Boot 4.1.1, Spring Modulith 2.1.1, Spring Security 7, Flyway, PostgreSQL 17, React 19, Vite 8, TypeScript 5.9, React Router 7, React Hook Form 7, Zod 4, JUnit 5, Testcontainers 2, GreenMail 2.1, Vitest 5, Testing Library, MSW 2.

**Spec:** [docs/superpowers/specs/2026-09-30-cadastro-usuarios-design.md](../specs/2026-09-30-cadastro-usuarios-design.md)

## Restrições globais

- Java **21** (o Maven da máquina roda com o JDK 21). Spring Boot **4.1.1**, Spring Modulith **2.1.1**, frontend-maven-plugin **2.0.2**, GreenMail **2.1.14**. As demais versões Java vêm do BOM do Boot.
- Node **v24.21.0** (instalado pelo frontend-maven-plugin). Versões npm: react/react-dom ^19.3.0, react-router ^7.18.4, react-hook-form ^7.89.0, @hookform/resolvers ^5.9.1, zod ^4.6.5, vite ^8.3.1, @vitejs/plugin-react ^6.1.1, typescript ^5.9.3, vitest ^5.0.3, jsdom ^30.1.1, msw ^2.15.0, @testing-library/react ^16.3.3, @testing-library/user-event ^14.6.7, @testing-library/jest-dom ^7.0.1, @types/react ^19.3.0, @types/react-dom ^19.3.0.
- Pacote raiz Java: `br.com.demo.cadastro`. Cada subpacote direto é um módulo do Modulith, e as classes internas ficam package-private.
- Identificadores, mensagens e textos de interface em português, com as mensagens exatamente como na seção 6 da spec. Mensagem de obrigatório: `Campo obrigatório`.
- O esquema do banco vem só do Flyway: `spring.jpa.hibernate.ddl-auto=none`.
- Os erros da API seguem `application/problem+json` com a propriedade `codigo` e, quando houver, `erros: [{campo, mensagem}]`.
- Os testes de integração exigem o Docker ativo (Testcontainers). Nenhum teste acessa o Gmail; o e-mail é testado com GreenMail.
- Credenciais só em `.env`, que fica no `.gitignore`. Nunca commitar segredos.
- Toda mensagem de commit termina com a linha `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Os comandos de commit abaixo omitem essa linha por brevidade, mas ela é obrigatória.
- Comandos são executados a partir da raiz do repositório.

## Foco da revisão

As entradas e falhas abaixo não aparecem como requisito na spec, mas são as que mais tendem a afetar um usuário real. Cada uma tem um teste na tarefa responsável.

1. **Senha com mais de 72 bytes.** O BCrypt rejeita senhas assim, o que geraria `500`. O esperado é `400` com mensagem própria no cadastro e `401` no login. Testes nas Tarefas 3, 6, 8 e 11.
2. **Link de ativação disparado duas vezes.** No React StrictMode, o efeito roda duas vezes, e a segunda chamada mostraria "já utilizado" para quem acabou de ativar. O esperado é uma única chamada à API. Teste na Tarefa 13.
3. **E-mail com maiúsculas ou espaços.** `Maria@Teste.local ` no cadastro e `maria@teste.local` no login precisam ser a mesma conta, e a unicidade não pode depender da caixa. Testes nas Tarefas 4 e 8.
4. **JSON malformado ou data impossível** (`"2020-13-45"`). O esperado é `400 VALIDACAO`, nunca `500`. Testes nas Tarefas 2 e 6.
5. **Cadastro concorrente com o mesmo e-mail ou CPF.** Duas requisições podem passar juntas pela checagem prévia. A violação da constraint `UNIQUE` precisa virar `409` com o campo certo, nunca `500`. Teste na Tarefa 4.

---

### Tarefa 1: Esqueleto do monolito modular e módulo `database`

**Arquivos:**
- Criar: `pom.xml`, `.gitignore`, `.env.example`
- Criar: `database/pom.xml`, `database/docker-compose.yml`
- Criar: `database/src/main/resources/db/migration/V1__cria_usuario.sql`, `V2__cria_token_ativacao.sql`, `V3__cria_event_publication.sql`
- Criar: `backend/pom.xml`, `backend/src/main/java/br/com/demo/cadastro/CadastroApplication.java`, `backend/src/main/resources/application.yml`
- Teste: `backend/src/test/resources/application-test.yml`, `backend/src/test/java/br/com/demo/cadastro/support/TestcontainersConfig.java`, `backend/src/test/java/br/com/demo/cadastro/support/IntegrationTest.java`, `backend/src/test/java/br/com/demo/cadastro/MigrationsTest.java`, `backend/src/test/java/br/com/demo/cadastro/ModularidadeTest.java`

**Interfaces:**
- Produz: a classe `CadastroApplication`; a classe base de testes `support.IntegrationTest`, com os campos protegidos `MockMvc mvc`, `JdbcTemplate jdbc` e `static GreenMailExtension greenMail` (SMTP em `localhost:3025`); o perfil Spring `test`; as tabelas `usuario`, `token_ativacao` e `event_publication`.

- [ ] **Passo 1: Criar o POM raiz, o `.gitignore` e o `.env.example`**

`pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
    <relativePath/>
  </parent>

  <groupId>br.com.demo.cadastro</groupId>
  <artifactId>cadastro</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>Cadastro de Usuários</name>

  <modules>
    <module>database</module>
    <module>backend</module>
  </modules>

  <properties>
    <java.version>21</java.version>
    <spring-modulith.version>2.1.1</spring-modulith.version>
    <greenmail.version>2.1.14</greenmail.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-bom</artifactId>
        <version>${spring-modulith.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

`.gitignore`:
```gitignore
target/
.env
frontend/node/
frontend/node_modules/
*.log
.idea/
```

`.env.example`:
```properties
# Banco de dados (os valores padrão funcionam com database/docker-compose.yml)
DB_URL=jdbc:postgresql://localhost:5432/cadastro
DB_USERNAME=cadastro
DB_PASSWORD=cadastro

# Gmail: conta remetente e senha de app (https://myaccount.google.com/apppasswords), sem espaços
MAIL_USERNAME=seu.email@gmail.com
MAIL_PASSWORD=suasenhadeapp

# Base do link de ativação e validade do token (ISO-8601)
APP_BASE_URL=http://localhost:8080
APP_ATIVACAO_EXPIRACAO=PT24H
```

- [ ] **Passo 2: Criar o módulo `database`**

`database/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>br.com.demo.cadastro</groupId>
    <artifactId>cadastro</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>database</artifactId>
  <name>Cadastro - Database</name>
  <description>Migrations Flyway e infraestrutura local do PostgreSQL</description>
</project>
```

`database/docker-compose.yml`:
```yaml
services:
  postgres:
    image: postgres:17-alpine
    container_name: cadastro-postgres
    environment:
      POSTGRES_DB: cadastro
      POSTGRES_USER: ${DB_USERNAME:-cadastro}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-cadastro}
    ports:
      - "5432:5432"
    volumes:
      - cadastro-dados:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-cadastro} -d cadastro"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  cadastro-dados:
```

`database/src/main/resources/db/migration/V1__cria_usuario.sql`:
```sql
CREATE TABLE usuario (
    id              UUID         PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    cpf             CHAR(11)     NOT NULL,
    email           VARCHAR(254) NOT NULL,
    data_nascimento DATE         NOT NULL,
    senha_hash      VARCHAR(100) NOT NULL,
    telefone        VARCHAR(11)  NOT NULL,
    cep             CHAR(8)      NOT NULL,
    logradouro      VARCHAR(200) NOT NULL,
    numero          VARCHAR(10)  NOT NULL,
    complemento     VARCHAR(100),
    bairro          VARCHAR(100) NOT NULL,
    cidade          VARCHAR(100) NOT NULL,
    uf              CHAR(2)      NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL,
    atualizado_em   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cpf UNIQUE (cpf),
    CONSTRAINT ck_usuario_status CHECK (status IN ('PENDENTE_ATIVACAO', 'ATIVO'))
);
```

`database/src/main/resources/db/migration/V2__cria_token_ativacao.sql`:
```sql
CREATE TABLE token_ativacao (
    id         UUID        PRIMARY KEY,
    usuario_id UUID        NOT NULL REFERENCES usuario (id),
    token_hash CHAR(64)    NOT NULL,
    expira_em  TIMESTAMPTZ NOT NULL,
    usado_em   TIMESTAMPTZ,
    criado_em  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_token_ativacao_hash UNIQUE (token_hash)
);

CREATE INDEX ix_token_ativacao_usuario ON token_ativacao (usuario_id);
```

`database/src/main/resources/db/migration/V3__cria_event_publication.sql` (esquema v2 do Spring Modulith 2.1.1, copiado de `spring-modulith-events-jdbc`):
```sql
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
```

- [ ] **Passo 3: Criar o POM do backend**

`backend/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>br.com.demo.cadastro</groupId>
    <artifactId>cadastro</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>backend</artifactId>
  <name>Cadastro - Backend</name>

  <dependencies>
    <dependency>
      <groupId>br.com.demo.cadastro</groupId>
      <artifactId>database</artifactId>
      <version>${project.version}</version>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-starter-jpa</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-testcontainers</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-postgresql</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.icegreen</groupId>
      <artifactId>greenmail-junit5</artifactId>
      <version>${greenmail.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.awaitility</groupId>
      <artifactId>awaitility</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <finalName>backend-${project.version}</finalName>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Passo 4: Escrever os testes que falham e a infraestrutura de teste**

`backend/src/test/resources/application-test.yml`:
```yaml
spring:
  mail:
    host: localhost
    port: 3025
    username: cadastro@teste.local
    password: teste
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
      mail.smtp.starttls.required: false

app:
  base-url: http://localhost:8080
  ativacao:
    expiracao: PT24H
```

`backend/src/test/java/br/com/demo/cadastro/support/TestcontainersConfig.java`:
```java
package br.com.demo.cadastro.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/support/IntegrationTest.java`:
```java
package br.com.demo.cadastro.support;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    @RegisterExtension
    protected static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication())
            .withPerMethodLifecycle(false);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;
}
```

`backend/src/test/java/br/com/demo/cadastro/MigrationsTest.java`:
```java
package br.com.demo.cadastro;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.demo.cadastro.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationsTest extends IntegrationTest {

    @Test
    void aplicaTodasAsMigrationsEmBancoVazio() {
        List<String> versoes = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(versoes).containsExactly("1", "2", "3");

        List<String> tabelas = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tabelas).contains("usuario", "token_ativacao", "event_publication");
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/ModularidadeTest.java`:
```java
package br.com.demo.cadastro;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadeTest {

    @Test
    void respeitaAsFronteirasDosModulos() {
        ApplicationModules.of(CadastroApplication.class).verify();
    }
}
```

- [ ] **Passo 5: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am verify`
Esperado: FALHA de compilação, `cannot find symbol: class CadastroApplication`.

- [ ] **Passo 6: Criar a aplicação e a configuração**

`backend/src/main/java/br/com/demo/cadastro/CadastroApplication.java`:
```java
package br.com.demo.cadastro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CadastroApplication {

    public static void main(String[] args) {
        SpringApplication.run(CadastroApplication.class, args);
    }
}
```

`backend/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: cadastro
  config:
    import: "optional:file:.env[.properties],optional:file:../.env[.properties]"
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/cadastro}
    username: ${DB_USERNAME:cadastro}
    password: ${DB_PASSWORD:cadastro}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
  jackson:
    deserialization:
      fail-on-unknown-properties: false
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
  modulith:
    events:
      republish-outstanding-events-on-restart: true

server:
  servlet:
    session:
      cookie:
        http-only: true
        same-site: lax

app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
  ativacao:
    expiracao: ${APP_ATIVACAO_EXPIRACAO:PT24H}
```

- [ ] **Passo 7: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am verify` (com o Docker ativo)
Esperado: BUILD SUCCESS; `MigrationsTest` e `ModularidadeTest` passam.

- [ ] **Passo 8: Commit**

```bash
git add pom.xml .gitignore .env.example database backend
git commit -m "build: cria monolito modular com módulos database e backend"
```

---

### Tarefa 2: `shared` — erros padronizados e relógio

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/shared/NegocioException.java`, `ErroCampo.java`, `Mensagens.java`, `ApiExceptionHandler.java`, `RelogioConfig.java`
- Teste: `backend/src/test/java/br/com/demo/cadastro/shared/ApiExceptionHandlerTest.java`

**Interfaces:**
- Produz:
  - `public class NegocioException extends RuntimeException`, com os construtores `(HttpStatus status, String codigo, String mensagem)` e `(HttpStatus status, String codigo, String mensagem, String campo)` e os getters `getStatus()`, `getCodigo()`, `getCampo()` (o campo pode ser `null`).
  - `public record ErroCampo(String campo, String mensagem)`.
  - `public final class Mensagens { public static final String OBRIGATORIO = "Campo obrigatório"; }`.
  - Um bean `java.time.Clock` (UTC).
  - Mapeamento de exceções: `MethodArgumentNotValidException` → `400 VALIDACAO` com uma mensagem por campo (o obrigatório tem prioridade); `HttpMessageNotReadableException` → `400 VALIDACAO` com o detalhe "Corpo da requisição inválido"; `NegocioException` → o status dela, com `erros` só quando há campo.

- [ ] **Passo 1: Escrever o teste que falha**

`backend/src/test/java/br/com/demo/cadastro/shared/ApiExceptionHandlerTest.java`:
```java
package br.com.demo.cadastro.shared;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorFalso())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void validacaoRetornaUmErroPorCampoPriorizandoObrigatorio() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","interno":{"valor":""}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros.length()").value(2))
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')].mensagem").value(contains("Campo obrigatório")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'interno.valor')].mensagem")
                        .value(contains("Campo obrigatório")));
    }

    @Test
    void corpoMalformadoRetornaValidacao() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido"));
    }

    @Test
    void negocioComCampoIncluiErroDoCampo() throws Exception {
        mvc.perform(post("/teste/negocio-com-campo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"))
                .andExpect(jsonPath("$.detail").value("Já existe"))
                .andExpect(jsonPath("$.erros[0].campo").value("email"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Já existe"));
    }

    @Test
    void negocioSemCampoNaoTemListaDeErros() throws Exception {
        mvc.perform(post("/teste/negocio-sem-campo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NEGADO"))
                .andExpect(jsonPath("$.erros").doesNotExist());
    }

    @RestController
    static class ControladorFalso {

        record Interno(@NotBlank(message = Mensagens.OBRIGATORIO) String valor) {}

        record Entrada(
                @NotBlank(message = Mensagens.OBRIGATORIO) @Size(min = 3, message = "Muito curto") String nome,
                @NotNull(message = Mensagens.OBRIGATORIO) @Valid Interno interno) {}

        @PostMapping("/teste/validacao")
        void validar(@Valid @RequestBody Entrada entrada) {}

        @PostMapping("/teste/negocio-com-campo")
        void negocioComCampo() {
            throw new NegocioException(HttpStatus.CONFLICT, "DUPLICADO", "Já existe", "email");
        }

        @PostMapping("/teste/negocio-sem-campo")
        void negocioSemCampo() {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "NEGADO", "Negado");
        }
    }
}
```

- [ ] **Passo 2: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest=ApiExceptionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA de compilação; `NegocioException`, `Mensagens` e `ApiExceptionHandler` não existem.

- [ ] **Passo 3: Implementar**

`backend/src/main/java/br/com/demo/cadastro/shared/NegocioException.java`:
```java
package br.com.demo.cadastro.shared;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final String campo;

    public NegocioException(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, null);
    }

    public NegocioException(HttpStatus status, String codigo, String mensagem, String campo) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.campo = campo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCampo() {
        return campo;
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/shared/ErroCampo.java`:
```java
package br.com.demo.cadastro.shared;

public record ErroCampo(String campo, String mensagem) {}
```

`backend/src/main/java/br/com/demo/cadastro/shared/Mensagens.java`:
```java
package br.com.demo.cadastro.shared;

public final class Mensagens {

    public static final String OBRIGATORIO = "Campo obrigatório";

    private Mensagens() {}
}
```

`backend/src/main/java/br/com/demo/cadastro/shared/ApiExceptionHandler.java`:
```java
package br.com.demo.cadastro.shared;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Set<String> CODIGOS_OBRIGATORIO = Set.of("NotBlank", "NotNull");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException ex) {
        Map<String, FieldError> porCampo = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            porCampo.merge(erro.getField(), erro,
                    (atual, novo) -> CODIGOS_OBRIGATORIO.contains(novo.getCode()) ? novo : atual);
        }
        List<ErroCampo> erros = porCampo.values().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return problema(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados inválidos", erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> corpoInvalido(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "VALIDACAO", "Corpo da requisição inválido", List.of());
    }

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ProblemDetail> negocio(NegocioException ex) {
        List<ErroCampo> erros = ex.getCampo() == null
                ? List.of()
                : List.of(new ErroCampo(ex.getCampo(), ex.getMessage()));
        return problema(ex.getStatus(), ex.getCodigo(), ex.getMessage(), erros);
    }

    private ResponseEntity<ProblemDetail> problema(
            HttpStatus status, String codigo, String detalhe, List<ErroCampo> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setProperty("codigo", codigo);
        if (!erros.isEmpty()) {
            problema.setProperty("erros", erros);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/shared/RelogioConfig.java`:
```java
package br.com.demo.cadastro.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RelogioConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

- [ ] **Passo 4: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest=ApiExceptionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA (4 testes). Se `$.codigo` sair aninhado em `$.properties`, o mixin de `ProblemDetail` não foi aplicado no setup standalone. Nesse caso, troque para `MockMvcBuilders.standaloneSetup(...).setMessageConverters(new JacksonJsonHttpMessageConverter())` e rode de novo.

- [ ] **Passo 5: Commit**

```bash
git add backend/src
git commit -m "feat(shared): padroniza erros da API em problem+json"
```

---

### Tarefa 3: `usuario` — regras de validação

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/usuario/Cpf.java`, `CpfValido.java`, `CpfValidator.java`, `Senha.java`, `SenhaForte.java`, `SenhaForteValidator.java`, `Uf.java`, `EnderecoDados.java`, `NovoUsuario.java`
- Teste: `backend/src/test/java/br/com/demo/cadastro/usuario/CpfTest.java`, `SenhaTest.java`, `NovoUsuarioValidacaoTest.java`

**Interfaces:**
- Consome: `Mensagens.OBRIGATORIO` (Tarefa 2).
- Produz:
  - `Cpf.isValido(String) : boolean` — exige 11 dígitos, sem máscara.
  - `Senha.isForte(String)` e `Senha.excedeLimite(String)` — o limite é de 72 bytes em UTF-8. Constantes `Senha.MENSAGEM_FRACA` e `Senha.MENSAGEM_LONGA`.
  - As anotações `@CpfValido` e `@SenhaForte`, e a constante `Uf.REGEX`.
  - `public record EnderecoDados(String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf)`.
  - `public record NovoUsuario(String nome, String cpf, String email, LocalDate dataNascimento, String senha, String telefone, EnderecoDados endereco)`.
  - Os dois records são anotados com Bean Validation.

- [ ] **Passo 1: Escrever os testes que falham**

`backend/src/test/java/br/com/demo/cadastro/usuario/CpfTest.java`:
```java
package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735"})
    void aceitaCpfComDigitosVerificadoresCorretos(String cpf) {
        assertThat(Cpf.isValido(cpf)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "52998224724",    // DV errado
            "11111111111",    // sequência repetida
            "5299822472",     // 10 dígitos
            "529982247250",   // 12 dígitos
            "529.982.247-25", // com máscara
            "5299822472a"
    })
    void rejeitaCpfInvalido(String cpf) {
        assertThat(Cpf.isValido(cpf)).isFalse();
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/usuario/SenhaTest.java`:
```java
package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SenhaTest {

    @ParameterizedTest
    @ValueSource(strings = {"Senha@123", "Abcdef1!", "Çãoção9#X"})
    void aceitaSenhaForte(String senha) {
        assertThat(Senha.isForte(senha)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Sen@1", "senha@123", "SENHA@123", "Senha@abc", "Senha1234"})
    void rejeitaSenhaFraca(String senha) {
        assertThat(Senha.isForte(senha)).isFalse();
    }

    @Test
    void limiteEhContadoEmBytesUtf8() {
        assertThat(Senha.excedeLimite("Aa1!" + "x".repeat(68))).isFalse(); // 72 bytes
        assertThat(Senha.excedeLimite("Aa1!" + "x".repeat(69))).isTrue();  // 73 bytes
        assertThat(Senha.excedeLimite("Aa1!" + "é".repeat(35))).isTrue();  // 74 bytes, 39 caracteres
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/usuario/NovoUsuarioValidacaoTest.java`:
```java
package br.com.demo.cadastro.usuario;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.demo.cadastro.shared.Mensagens;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NovoUsuarioValidacaoTest {

    private static Validator validator;

    @BeforeAll
    static void criarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static EnderecoDados enderecoValido() {
        return new EnderecoDados("01310100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP");
    }

    private static Map<String, Set<String>> mensagens(Object alvo) {
        return validator.validate(alvo).stream().collect(groupingBy(
                v -> v.getPropertyPath().toString(), mapping(ConstraintViolation::getMessage, toSet())));
    }

    @Test
    void dadosValidosSemComplementoNaoGeramViolacoes() {
        var usuario = new NovoUsuario("Maria da Silva", "52998224725", "maria@teste.local",
                LocalDate.of(1990, 5, 20), "Senha@123", "11987654321", enderecoValido());
        assertThat(validator.validate(usuario)).isEmpty();
    }

    @Test
    void camposAusentesSaoObrigatorios() {
        var usuario = new NovoUsuario(null, null, null, null, null, null, null);
        assertThat(mensagens(usuario))
                .containsEntry("nome", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("cpf", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("email", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("dataNascimento", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("senha", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("telefone", Set.of(Mensagens.OBRIGATORIO))
                .containsEntry("endereco", Set.of(Mensagens.OBRIGATORIO));
    }

    @Test
    void reportaCadaRegraComAMensagemDaSpec() {
        var usuario = new NovoUsuario("Maria", "12345678900", "invalido", LocalDate.now().plusDays(1),
                "fraca", "123", new EnderecoDados("1", "Rua", "1", null, "Centro", "Cidade", "XX"));
        assertThat(mensagens(usuario))
                .containsEntry("cpf", Set.of("CPF inválido"))
                .containsEntry("email", Set.of("E-mail inválido"))
                .containsEntry("dataNascimento", Set.of("Data de nascimento inválida"))
                .containsEntry("senha", Set.of(Senha.MENSAGEM_FRACA))
                .containsEntry("telefone", Set.of("Telefone inválido"))
                .containsEntry("endereco.cep", Set.of("CEP inválido"))
                .containsEntry("endereco.uf", Set.of("UF inválida"));
    }

    @Test
    void enderecoAusenteNosCamposObrigatorios() {
        var endereco = new EnderecoDados(null, null, null, null, null, null, null);
        assertThat(mensagens(endereco)).containsOnlyKeys("cep", "logradouro", "numero", "bairro", "cidade", "uf");
    }

    @Test
    void senhaAcimaDe72BytesTemMensagemPropria() {
        var usuario = new NovoUsuario("Maria da Silva", "52998224725", "maria@teste.local",
                LocalDate.of(1990, 5, 20), "Aa1!" + "é".repeat(40), "11987654321", enderecoValido());
        assertThat(mensagens(usuario)).containsEntry("senha", Set.of(Senha.MENSAGEM_LONGA));
    }
}
```

- [ ] **Passo 2: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest='CpfTest,SenhaTest,NovoUsuarioValidacaoTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA de compilação; `Cpf`, `Senha`, `NovoUsuario` e `EnderecoDados` não existem.

- [ ] **Passo 3: Implementar**

`backend/src/main/java/br/com/demo/cadastro/usuario/Cpf.java`:
```java
package br.com.demo.cadastro.usuario;

public final class Cpf {

    private Cpf() {}

    public static boolean isValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digito(cpf, 9) == cpf.charAt(9) - '0' && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digito(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/CpfValido.java`:
```java
package br.com.demo.cadastro.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfValido {

    String message() default "CPF inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/CpfValidator.java`:
```java
package br.com.demo.cadastro.usuario;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.isEmpty() || Cpf.isValido(valor);
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/Senha.java`:
```java
package br.com.demo.cadastro.usuario;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Senha {

    public static final int LIMITE_BYTES = 72;
    public static final String MENSAGEM_FRACA =
            "A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial";
    public static final String MENSAGEM_LONGA = "A senha é longa demais";

    private static final Pattern FORTE =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private Senha() {}

    public static boolean isForte(String senha) {
        return senha != null && FORTE.matcher(senha).matches();
    }

    public static boolean excedeLimite(String senha) {
        return senha != null && senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES;
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/SenhaForte.java`:
```java
package br.com.demo.cadastro.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

    String message() default Senha.MENSAGEM_FRACA;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/SenhaForteValidator.java`:
```java
package br.com.demo.cadastro.usuario;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isEmpty()) {
            return true;
        }
        if (Senha.excedeLimite(valor)) {
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(Senha.MENSAGEM_LONGA).addConstraintViolation();
            return false;
        }
        return Senha.isForte(valor);
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/Uf.java`:
```java
package br.com.demo.cadastro.usuario;

public final class Uf {

    public static final String REGEX =
            "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO";

    private Uf() {}
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/EnderecoDados.java`:
```java
package br.com.demo.cadastro.usuario;

import br.com.demo.cadastro.shared.Mensagens;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoDados(
        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = "\\d{8}", message = "CEP inválido")
        String cep,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 200, message = "Máximo de 200 caracteres")
        String logradouro,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 10, message = "Máximo de 10 caracteres")
        String numero,

        @Size(max = 100, message = "Máximo de 100 caracteres")
        String complemento,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 100, message = "Máximo de 100 caracteres")
        String bairro,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 100, message = "Máximo de 100 caracteres")
        String cidade,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = Uf.REGEX, message = "UF inválida")
        String uf) {}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/NovoUsuario.java`:
```java
package br.com.demo.cadastro.usuario;

import br.com.demo.cadastro.shared.Mensagens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NovoUsuario(
        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Size(max = 150, message = "Máximo de 150 caracteres")
        String nome,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @CpfValido
        String cpf,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Email(message = "E-mail inválido")
        @Size(max = 254, message = "Máximo de 254 caracteres")
        String email,

        @NotNull(message = Mensagens.OBRIGATORIO)
        @Past(message = "Data de nascimento inválida")
        LocalDate dataNascimento,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @SenhaForte
        String senha,

        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
        String telefone,

        @NotNull(message = Mensagens.OBRIGATORIO)
        @Valid
        EnderecoDados endereco) {}
```

- [ ] **Passo 4: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest='CpfTest,SenhaTest,NovoUsuarioValidacaoTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 5: Commit**

```bash
git add backend/src
git commit -m "feat(usuario): adiciona regras de validação de CPF, senha e endereço"
```

---
### Tarefa 4: `usuario` — entidade, persistência e serviço

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/usuario/StatusUsuario.java`, `Endereco.java`, `Usuario.java`, `UsuarioRepository.java`, `UsuarioConfig.java`, `UsuarioService.java`
- Modificar: `backend/src/main/java/br/com/demo/cadastro/usuario/EnderecoDados.java` (adicionar `paraEndereco()` e `de(Endereco)`)
- Teste: `backend/src/test/java/br/com/demo/cadastro/support/DadosTeste.java`, `backend/src/test/java/br/com/demo/cadastro/usuario/UsuarioServiceTest.java`, `backend/src/test/java/br/com/demo/cadastro/usuario/TraducaoViolacaoTest.java`

**Interfaces:**
- Consome: `NegocioException` e o bean `Clock` (Tarefa 2); `NovoUsuario`, `EnderecoDados` e `Senha` (Tarefa 3).
- Produz:
  - `public enum StatusUsuario { PENDENTE_ATIVACAO, ATIVO }`.
  - `public class Endereco` (`@Embeddable`), com os getters `getCep()`, `getLogradouro()`, `getNumero()`, `getComplemento()`, `getBairro()`, `getCidade()`, `getUf()`.
  - `public class Usuario` (`@Entity`), com os getters `getId(): UUID`, `getNome()`, `getCpf()`, `getEmail()`, `getDataNascimento(): LocalDate`, `getTelefone()`, `getEndereco(): Endereco`, `getStatus(): StatusUsuario`, `getCriadoEm()`, `getAtualizadoEm(): Instant` e o método `isAtivo()`. As mutações são package-private.
  - `public class UsuarioService`, com os métodos:
    - `Usuario cadastrar(NovoUsuario)` — lança `NegocioException(409, EMAIL_JA_CADASTRADO | CPF_JA_CADASTRADO, campo email | cpf)`.
    - `Optional<Usuario> buscarPorId(UUID)` e `Optional<Usuario> buscarPorEmail(String)` — este normaliza o e-mail.
    - `boolean senhaConfere(Usuario, String)` — retorna `false` para senha nula ou acima de 72 bytes.
    - `void ativar(UUID)`.
    - `Usuario atualizarContato(UUID, String telefone, EnderecoDados)`.
  - `EnderecoDados.paraEndereco(): Endereco` e `EnderecoDados.de(Endereco): EnderecoDados`.
  - Os auxiliares de teste `DadosTeste.SENHA`, `emailUnico()`, `cpfValido()` e `novoUsuario(email, cpf)`.

- [ ] **Passo 1: Criar os dados de teste**

`backend/src/test/java/br/com/demo/cadastro/support/DadosTeste.java`:
```java
package br.com.demo.cadastro.support;

import br.com.demo.cadastro.usuario.EnderecoDados;
import br.com.demo.cadastro.usuario.NovoUsuario;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DadosTeste {

    public static final String SENHA = "Senha@123";

    private DadosTeste() {}

    public static String emailUnico() {
        return "u" + UUID.randomUUID().toString().substring(0, 12) + "@teste.local";
    }

    public static String cpfValido() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (todosIguais(d));
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder cpf = new StringBuilder();
        for (int digito : d) {
            cpf.append(digito);
        }
        return cpf.toString();
    }

    public static NovoUsuario novoUsuario(String email, String cpf) {
        return new NovoUsuario("Maria da Silva", cpf, email, LocalDate.of(1990, 5, 20), SENHA, "11987654321",
                new EnderecoDados("01310100", "Avenida Paulista", "1000", "Apto 12", "Bela Vista", "São Paulo", "SP"));
    }

    private static boolean todosIguais(int[] d) {
        for (int i = 1; i < 9; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }

    private static int digito(int[] d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += d[i] * (quantidade + 1 - i);
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
```

- [ ] **Passo 2: Escrever os testes que falham**

`backend/src/test/java/br/com/demo/cadastro/usuario/UsuarioServiceTest.java`:
```java
package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class UsuarioServiceTest extends IntegrationTest {

    @Autowired
    UsuarioService servico;

    @Test
    void cadastraComoPendenteComSenhaCriptografadaEEmailNormalizado() {
        String email = DadosTeste.emailUnico();

        Usuario usuario = servico.cadastrar(
                DadosTeste.novoUsuario("  " + email.toUpperCase() + " ", DadosTeste.cpfValido()));

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getEmail()).isEqualTo(email);
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.PENDENTE_ATIVACAO);
        String hash = jdbc.queryForObject("select senha_hash from usuario where id = ?", String.class, usuario.getId());
        assertThat(hash).startsWith("$2").isNotEqualTo(DadosTeste.SENHA);
        assertThat(servico.senhaConfere(usuario, DadosTeste.SENHA)).isTrue();
        assertThat(servico.senhaConfere(usuario, "Outra@123")).isFalse();
        assertThat(servico.senhaConfere(usuario, "Aa1!" + "é".repeat(40))).isFalse();
    }

    @Test
    void rejeitaEmailDuplicadoIgnorandoMaiusculas() {
        String email = DadosTeste.emailUnico();
        servico.cadastrar(DadosTeste.novoUsuario(email, DadosTeste.cpfValido()));

        assertThatThrownBy(() -> servico.cadastrar(DadosTeste.novoUsuario(email.toUpperCase(), DadosTeste.cpfValido())))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("EMAIL_JA_CADASTRADO");
                    assertThat(e.getCampo()).isEqualTo("email");
                });
    }

    @Test
    void rejeitaCpfDuplicado() {
        String cpf = DadosTeste.cpfValido();
        servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), cpf));

        assertThatThrownBy(() -> servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), cpf)))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("CPF_JA_CADASTRADO");
                    assertThat(e.getCampo()).isEqualTo("cpf");
                });
    }

    @Test
    void ativaUsuario() {
        Usuario usuario = servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), DadosTeste.cpfValido()));

        servico.ativar(usuario.getId());

        assertThat(servico.buscarPorId(usuario.getId())).get()
                .extracting(Usuario::getStatus).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    void atualizaSomenteContatoEPreservaDadosImutaveis() {
        Usuario original = servico.cadastrar(DadosTeste.novoUsuario(DadosTeste.emailUnico(), DadosTeste.cpfValido()));
        var novoEndereco = new EnderecoDados("20040002", "Rua da Assembleia", "S/N", "  ", "Centro", "Rio de Janeiro", "RJ");

        Usuario atualizado = servico.atualizarContato(original.getId(), "2133334444", novoEndereco);

        assertThat(atualizado.getTelefone()).isEqualTo("2133334444");
        assertThat(EnderecoDados.de(atualizado.getEndereco())).isEqualTo(
                new EnderecoDados("20040002", "Rua da Assembleia", "S/N", null, "Centro", "Rio de Janeiro", "RJ"));
        assertThat(atualizado.getNome()).isEqualTo(original.getNome());
        assertThat(atualizado.getCpf()).isEqualTo(original.getCpf());
        assertThat(atualizado.getEmail()).isEqualTo(original.getEmail());
        assertThat(atualizado.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(atualizado.getAtualizadoEm()).isAfterOrEqualTo(original.getAtualizadoEm());
    }

    @Test
    void buscaPorEmailIgnoraMaiusculasEEspacos() {
        String email = DadosTeste.emailUnico();
        servico.cadastrar(DadosTeste.novoUsuario(email, DadosTeste.cpfValido()));

        assertThat(servico.buscarPorEmail(" " + email.toUpperCase() + " ")).isPresent();
        assertThat(servico.buscarPorEmail(null)).isEmpty();
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/usuario/TraducaoViolacaoTest.java` (cobre a corrida entre a checagem prévia e o `INSERT`):
```java
package br.com.demo.cadastro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.demo.cadastro.shared.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class TraducaoViolacaoTest {

    private static DataIntegrityViolationException violacao(String constraint) {
        return new DataIntegrityViolationException("falha",
                new RuntimeException("duplicate key value violates unique constraint \"" + constraint + "\""));
    }

    @Test
    void violacaoDeEmailViraConflitoNoCampoEmail() {
        NegocioException e = UsuarioService.traduzirViolacao(violacao("uk_usuario_email"));
        assertThat(e.getCodigo()).isEqualTo("EMAIL_JA_CADASTRADO");
        assertThat(e.getCampo()).isEqualTo("email");
    }

    @Test
    void violacaoDeCpfViraConflitoNoCampoCpf() {
        NegocioException e = UsuarioService.traduzirViolacao(violacao("uk_usuario_cpf"));
        assertThat(e.getCodigo()).isEqualTo("CPF_JA_CADASTRADO");
        assertThat(e.getCampo()).isEqualTo("cpf");
    }
}
```

- [ ] **Passo 3: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest='UsuarioServiceTest,TraducaoViolacaoTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA de compilação; `UsuarioService`, `Usuario` e `StatusUsuario` não existem.

- [ ] **Passo 4: Implementar**

`backend/src/main/java/br/com/demo/cadastro/usuario/StatusUsuario.java`:
```java
package br.com.demo.cadastro.usuario;

public enum StatusUsuario {
    PENDENTE_ATIVACAO,
    ATIVO
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/Endereco.java`:
```java
package br.com.demo.cadastro.usuario;

import jakarta.persistence.Embeddable;

@Embeddable
public class Endereco {

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;

    protected Endereco() {}

    Endereco(String cep, String logradouro, String numero, String complemento,
             String bairro, String cidade, String uf) {
        this.cep = cep;
        this.logradouro = logradouro.trim();
        this.numero = numero.trim();
        this.complemento = complemento == null || complemento.isBlank() ? null : complemento.trim();
        this.bairro = bairro.trim();
        this.cidade = cidade.trim();
        this.uf = uf;
    }

    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/Usuario.java`:
```java
package br.com.demo.cadastro.usuario;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String cpf;
    private String email;
    private LocalDate dataNascimento;
    private String senhaHash;
    private String telefone;

    @Embedded
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    private StatusUsuario status;

    private Instant criadoEm;
    private Instant atualizadoEm;

    protected Usuario() {}

    Usuario(String nome, String cpf, String email, LocalDate dataNascimento, String senhaHash,
            String telefone, Endereco endereco, Instant agora) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.dataNascimento = dataNascimento;
        this.senhaHash = senhaHash;
        this.telefone = telefone;
        this.endereco = endereco;
        this.status = StatusUsuario.PENDENTE_ATIVACAO;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    void ativar(Instant agora) {
        if (status != StatusUsuario.ATIVO) {
            status = StatusUsuario.ATIVO;
            atualizadoEm = agora;
        }
    }

    void atualizarContato(String telefone, Endereco endereco, Instant agora) {
        this.telefone = telefone;
        this.endereco = endereco;
        this.atualizadoEm = agora;
    }

    String getSenhaHash() { return senhaHash; }

    public boolean isAtivo() { return status == StatusUsuario.ATIVO; }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getTelefone() { return telefone; }
    public Endereco getEndereco() { return endereco; }
    public StatusUsuario getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/UsuarioRepository.java`:
```java
package br.com.demo.cadastro.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    Optional<Usuario> findByEmail(String email);
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/UsuarioConfig.java`:
```java
package br.com.demo.cadastro.usuario;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class UsuarioConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/usuario/UsuarioService.java`:
```java
package br.com.demo.cadastro.usuario;

import br.com.demo.cadastro.shared.NegocioException;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final Clock clock;

    UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder, Clock clock) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional
    public Usuario cadastrar(NovoUsuario dados) {
        String email = normalizarEmail(dados.email());
        if (repositorio.existsByEmail(email)) {
            throw emailDuplicado();
        }
        if (repositorio.existsByCpf(dados.cpf())) {
            throw cpfDuplicado();
        }
        Usuario usuario = new Usuario(dados.nome().trim(), dados.cpf(), email, dados.dataNascimento(),
                encoder.encode(dados.senha()), dados.telefone(), dados.endereco().paraEndereco(), clock.instant());
        try {
            return repositorio.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacao(e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return repositorio.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return email == null ? Optional.empty() : repositorio.findByEmail(normalizarEmail(email));
    }

    public boolean senhaConfere(Usuario usuario, String senha) {
        return senha != null && !Senha.excedeLimite(senha) && encoder.matches(senha, usuario.getSenhaHash());
    }

    @Transactional
    public void ativar(UUID id) {
        buscarExistente(id).ativar(clock.instant());
    }

    @Transactional
    public Usuario atualizarContato(UUID id, String telefone, EnderecoDados endereco) {
        Usuario usuario = buscarExistente(id);
        usuario.atualizarContato(telefone, endereco.paraEndereco(), clock.instant());
        return usuario;
    }

    static NegocioException traduzirViolacao(DataIntegrityViolationException e) {
        String mensagem = String.valueOf(e.getMostSpecificCause().getMessage());
        return mensagem.contains("uk_usuario_email") ? emailDuplicado() : cpfDuplicado();
    }

    private Usuario buscarExistente(UUID id) {
        return repositorio.findById(id).orElseThrow(() ->
                new NegocioException(HttpStatus.NOT_FOUND, "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado"));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static NegocioException emailDuplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "EMAIL_JA_CADASTRADO", "E-mail já cadastrado", "email");
    }

    private static NegocioException cpfDuplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "CPF_JA_CADASTRADO", "CPF já cadastrado", "cpf");
    }
}
```

Em `EnderecoDados.java`, troque o fechamento `String uf) {}` por:
```java
        String uf) {

    public Endereco paraEndereco() {
        return new Endereco(cep, logradouro, numero, complemento, bairro, cidade, uf);
    }

    public static EnderecoDados de(Endereco endereco) {
        return new EnderecoDados(endereco.getCep(), endereco.getLogradouro(), endereco.getNumero(),
                endereco.getComplemento(), endereco.getBairro(), endereco.getCidade(), endereco.getUf());
    }
}
```

- [ ] **Passo 5: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest='UsuarioServiceTest,TraducaoViolacaoTest,ModularidadeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 6: Commit**

```bash
git add backend/src
git commit -m "feat(usuario): persiste usuário com unicidade, ativação e atualização de contato"
```

---

### Tarefa 5: `autenticacao` — configuração de segurança

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/autenticacao/SegurancaConfig.java`, `NaoAutenticadoEntryPoint.java`, `CsrfController.java`
- Teste: `backend/src/test/java/br/com/demo/cadastro/autenticacao/SegurancaIntegrationTest.java`

**Interfaces:**
- Produz:
  - Rotas públicas: `POST /api/usuarios`, `POST /api/ativacao`, `POST /api/auth/login`, `GET /api/csrf` e tudo fora de `/api/**`. O resto de `/api/**` exige autenticação.
  - Requisição não autenticada recebe `401` com `codigo` `NAO_AUTENTICADO`.
  - CSRF por cookie `XSRF-TOKEN` (legível pelo JS) e header `X-XSRF-TOKEN`, com o handler simples, sem XOR.
  - `GET /api/csrf` responde `{token}`.
  - O principal autenticado é o `id` do usuário (UUID em texto), definido na Tarefa 8.

- [ ] **Passo 1: Escrever o teste que falha**

`backend/src/test/java/br/com/demo/cadastro/autenticacao/SegurancaIntegrationTest.java`:
```java
package br.com.demo.cadastro.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class SegurancaIntegrationTest extends IntegrationTest {

    @Test
    void apiProtegidaSemSessaoRetorna401EmProblemJson() throws Exception {
        mvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void endpointCsrfEmiteCookieLegivelPeloJavascript() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();

        String cookie = resultado.getResponse().getCookie("XSRF-TOKEN").getValue();
        String token = JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
        assertThat(token).isEqualTo(cookie);
    }

    @Test
    void mutacaoSemTokenCsrfEhRecusada() throws Exception {
        mvc.perform(post("/api/ativacao").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recursosForaDaApiNaoExigemAutenticacao() throws Exception {
        mvc.perform(get("/arquivo-inexistente.js")).andExpect(status().isNotFound());
    }
}
```

- [ ] **Passo 2: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest=SegurancaIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA. Com a segurança padrão do Boot, o 401 sai sem corpo problem+json, `/api/csrf` não existe e os recursos estáticos exigem login.

- [ ] **Passo 3: Implementar**

`backend/src/main/java/br/com/demo/cadastro/autenticacao/SegurancaConfig.java`:
```java
package br.com.demo.cadastro.autenticacao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration(proxyBeanMethods = false)
class SegurancaConfig {

    @Bean
    SecurityFilterChain filtroSeguranca(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/ativacao", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(erros -> erros.authenticationEntryPoint(new NaoAutenticadoEntryPoint()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/autenticacao/NaoAutenticadoEntryPoint.java`:
```java
package br.com.demo.cadastro.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

class NaoAutenticadoEntryPoint implements AuthenticationEntryPoint {

    private static final String CORPO = "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Autenticação necessária\",\"codigo\":\"NAO_AUTENTICADO\"}";

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write(CORPO);
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/autenticacao/CsrfController.java`:
```java
package br.com.demo.cadastro.autenticacao;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CsrfController {

    record CsrfResposta(String token) {}

    @GetMapping("/api/csrf")
    CsrfResposta csrf(CsrfToken token) {
        return new CsrfResposta(token.getToken());
    }
}
```

- [ ] **Passo 4: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest='SegurancaIntegrationTest,ModularidadeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 5: Commit**

```bash
git add backend/src
git commit -m "feat(autenticacao): configura sessão, CSRF por cookie e 401 em problem+json"
```

---

### Tarefa 6: `cadastro` — `POST /api/usuarios` (RF01–RF03)

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/cadastro/UsuarioCadastrado.java`, `CadastroResposta.java`, `CadastroService.java`, `CadastroController.java`
- Modificar: `backend/src/test/java/br/com/demo/cadastro/support/DadosTeste.java` (adicionar `cadastroJson`); `backend/src/test/java/br/com/demo/cadastro/support/IntegrationTest.java` (adicionar auxiliares)
- Teste: `backend/src/test/java/br/com/demo/cadastro/cadastro/CadastroIntegrationTest.java`

**Interfaces:**
- Consome: `UsuarioService.cadastrar`, `NovoUsuario` (Tarefas 3 e 4) e a configuração de segurança (Tarefa 5).
- Produz:
  - `public record UsuarioCadastrado(UUID usuarioId, String nome, String email)`, publicado na mesma transação do cadastro.
  - `POST /api/usuarios` → `201 {id, email, status}`.
  - Auxiliares de teste:
    - `DadosTeste.cadastroJson(String email, String cpf)` e `cadastroJson(String email, String cpf, String nome)`.
    - Em `IntegrationTest`: `String cadastrar(String email, String cpf)` (devolve o id em texto), `String cadastrar(String email, String cpf, String nome)`, `void marcarComoAtivo(String email)` e `String statusDoUsuario(String id)`.

- [ ] **Passo 1: Adicionar os auxiliares de teste**

Em `DadosTeste.java`, adicione:
```java
    public static String cadastroJson(String email, String cpf) {
        return cadastroJson(email, cpf, "Maria da Silva");
    }

    public static String cadastroJson(String email, String cpf, String nome) {
        return """
                {"nome":"%s","cpf":"%s","email":"%s","dataNascimento":"1990-05-20","senha":"%s",
                 "telefone":"11987654321",
                 "endereco":{"cep":"01310100","logradouro":"Avenida Paulista","numero":"1000",
                             "complemento":"Apto 12","bairro":"Bela Vista","cidade":"São Paulo","uf":"SP"}}
                """.formatted(nome, cpf, email, SENHA);
    }
```

Em `IntegrationTest.java`, adicione estes imports e métodos:
```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
```
```java
    protected String cadastrar(String email, String cpf) throws Exception {
        return cadastrar(email, cpf, "Maria da Silva");
    }

    protected String cadastrar(String email, String cpf, String nome) throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DadosTeste.cadastroJson(email, cpf, nome)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("select id::text from usuario where email = ?", String.class, email);
    }

    protected void marcarComoAtivo(String email) {
        jdbc.update("update usuario set status = 'ATIVO' where email = ?", email);
    }

    protected String statusDoUsuario(String id) {
        return jdbc.queryForObject("select status from usuario where id = ?::uuid", String.class, id);
    }
```

- [ ] **Passo 2: Escrever o teste que falha**

`backend/src/test/java/br/com/demo/cadastro/cadastro/CadastroIntegrationTest.java`:
```java
package br.com.demo.cadastro.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

@RecordApplicationEvents
class CadastroIntegrationTest extends IntegrationTest {

    @Autowired
    ApplicationEvents eventos;

    private ResultActions postar(String json) throws Exception {
        return mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void cadastraUsuarioPendenteEPublicaEvento() throws Exception {
        String email = DadosTeste.emailUnico();

        postar(DadosTeste.cadastroJson(email.toUpperCase(), DadosTeste.cpfValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("PENDENTE_ATIVACAO"));

        Map<String, Object> linha = jdbc.queryForMap("select status, senha_hash from usuario where email = ?", email);
        assertThat(linha.get("status")).isEqualTo("PENDENTE_ATIVACAO");
        assertThat((String) linha.get("senha_hash")).startsWith("$2").isNotEqualTo(DadosTeste.SENHA);
        assertThat(eventos.stream(UsuarioCadastrado.class))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.email()).isEqualTo(email);
                    assertThat(e.nome()).isEqualTo("Maria da Silva");
                });
    }

    @Test
    void complementoEhOpcional() throws Exception {
        String json = DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())
                .replace("\"complemento\":\"Apto 12\",", "");
        postar(json).andExpect(status().isCreated());
    }

    @Test
    void corpoVazioListaTodosOsCamposObrigatorios() throws Exception {
        postar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[*].campo").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "nome", "cpf", "email", "dataNascimento", "senha", "telefone", "endereco")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'cpf')].mensagem").value(contains("Campo obrigatório")));
    }

    @Test
    void formatosInvalidosRetornamMensagensDaSpec() throws Exception {
        String json = """
                {"nome":"Maria","cpf":"12345678900","email":"invalido","dataNascimento":"2999-01-01",
                 "senha":"fraca","telefone":"123",
                 "endereco":{"cep":"1","logradouro":"Rua","numero":"1","bairro":"Centro","cidade":"Cidade","uf":"XX"}}
                """;
        postar(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'cpf')].mensagem").value(contains("CPF inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'email')].mensagem").value(contains("E-mail inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'dataNascimento')].mensagem")
                        .value(contains("Data de nascimento inválida")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'senha')].mensagem").value(contains(
                        "A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'telefone')].mensagem").value(contains("Telefone inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'endereco.cep')].mensagem").value(contains("CEP inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'endereco.uf')].mensagem").value(contains("UF inválida")));
    }

    @Test
    void dataImpossivelOuJsonMalformadoRetorna400() throws Exception {
        String json = DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())
                .replace("1990-05-20", "2020-13-45");
        postar(json).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        postar("{\"nome\":").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void senhaAcimaDe72BytesRetorna400() throws Exception {
        String json = DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())
                .replace(DadosTeste.SENHA, "Aa1!" + "é".repeat(40));
        postar(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'senha')].mensagem").value(contains("A senha é longa demais")));
    }

    @Test
    void emailDuplicadoComOutraCaixaRetorna409NoCampoEmail() throws Exception {
        String email = DadosTeste.emailUnico();
        postar(DadosTeste.cadastroJson(email, DadosTeste.cpfValido())).andExpect(status().isCreated());

        postar(DadosTeste.cadastroJson(email.toUpperCase(), DadosTeste.cpfValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
                .andExpect(jsonPath("$.erros[0].campo").value("email"));
    }

    @Test
    void cpfDuplicadoRetorna409NoCampoCpf() throws Exception {
        String cpf = DadosTeste.cpfValido();
        postar(DadosTeste.cadastroJson(DadosTeste.emailUnico(), cpf)).andExpect(status().isCreated());

        postar(DadosTeste.cadastroJson(DadosTeste.emailUnico(), cpf))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CPF_JA_CADASTRADO"))
                .andExpect(jsonPath("$.erros[0].campo").value("cpf"));
    }

    @Test
    void semTokenCsrfRetorna403() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    void aceitaTokenCsrfObtidoEmApiCsrf() throws Exception {
        Cookie cookie = mvc.perform(get("/api/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/usuarios").cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DadosTeste.cadastroJson(DadosTeste.emailUnico(), DadosTeste.cpfValido())))
                .andExpect(status().isCreated());
    }
}
```

- [ ] **Passo 3: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest=CadastroIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA de compilação; `UsuarioCadastrado` não existe.

- [ ] **Passo 4: Implementar**

`backend/src/main/java/br/com/demo/cadastro/cadastro/UsuarioCadastrado.java`:
```java
package br.com.demo.cadastro.cadastro;

import java.util.UUID;

public record UsuarioCadastrado(UUID usuarioId, String nome, String email) {}
```

`backend/src/main/java/br/com/demo/cadastro/cadastro/CadastroResposta.java`:
```java
package br.com.demo.cadastro.cadastro;

import java.util.UUID;

record CadastroResposta(UUID id, String email, String status) {}
```

`backend/src/main/java/br/com/demo/cadastro/cadastro/CadastroService.java`:
```java
package br.com.demo.cadastro.cadastro;

import br.com.demo.cadastro.usuario.NovoUsuario;
import br.com.demo.cadastro.usuario.Usuario;
import br.com.demo.cadastro.usuario.UsuarioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CadastroService {

    private final UsuarioService usuarios;
    private final ApplicationEventPublisher eventos;

    CadastroService(UsuarioService usuarios, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    @Transactional
    public CadastroResposta cadastrar(NovoUsuario dados) {
        Usuario usuario = usuarios.cadastrar(dados);
        eventos.publishEvent(new UsuarioCadastrado(usuario.getId(), usuario.getNome(), usuario.getEmail()));
        return new CadastroResposta(usuario.getId(), usuario.getEmail(), usuario.getStatus().name());
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/cadastro/CadastroController.java`:
```java
package br.com.demo.cadastro.cadastro;

import br.com.demo.cadastro.usuario.NovoUsuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CadastroController {

    private final CadastroService servico;

    CadastroController(CadastroService servico) {
        this.servico = servico;
    }

    @PostMapping("/api/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    CadastroResposta cadastrar(@Valid @RequestBody NovoUsuario dados) {
        return servico.cadastrar(dados);
    }
}
```

- [ ] **Passo 5: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest='CadastroIntegrationTest,ModularidadeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 6: Commit**

```bash
git add backend/src
git commit -m "feat(cadastro): expõe POST /api/usuarios e publica UsuarioCadastrado"
```

---

### Tarefa 7: `ativacao` — token, e-mail e ativação (RF04, RF05, RN02)

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/ativacao/GeradorToken.java`, `TokenAtivacao.java`, `TokenAtivacaoRepository.java`, `EmailAtivacao.java`, `AtivacaoService.java`, `AtivacaoController.java`
- Teste: `backend/src/test/java/br/com/demo/cadastro/support/EmailsTeste.java`, `backend/src/test/java/br/com/demo/cadastro/ativacao/GeradorTokenTest.java`, `TokenAtivacaoTest.java`, `AtivacaoIntegrationTest.java`, `AtivacaoFalhaEmailIntegrationTest.java`

**Interfaces:**
- Consome: `UsuarioCadastrado` (Tarefa 6), `UsuarioService.ativar` (Tarefa 4), `NegocioException` e `Clock` (Tarefa 2), as propriedades `app.base-url` e `app.ativacao.expiracao` (Tarefa 1).
- Produz:
  - `POST /api/ativacao {token}` → `204`. Erros: `400` com `TOKEN_INVALIDO`, `TOKEN_EXPIRADO` ou `TOKEN_JA_UTILIZADO`.
  - O e-mail "Ative sua conta", com link `{app.base-url}/ativar?token={token}`.
  - Os pacotes internos `GeradorToken.hash(String): String` (SHA-256 em hex) e `EmailAtivacao.enviar(String para, String nome, String link)`.
  - Os auxiliares de teste `EmailsTeste.aguardarMensagem(greenMail, email)`, `conteudo(MimeMessage)` e `extrairToken(MimeMessage)`.

- [ ] **Passo 1: Escrever os testes unitários que falham**

`backend/src/test/java/br/com/demo/cadastro/ativacao/GeradorTokenTest.java`:
```java
package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeradorTokenTest {

    private final GeradorToken gerador = new GeradorToken();

    @Test
    void geraTokenBase64UrlDe32BytesSemPadding() {
        String token = gerador.gerar();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(gerador.gerar()).isNotEqualTo(token);
    }

    @Test
    void hashEhSha256EmHexadecimal() {
        assertThat(GeradorToken.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/ativacao/TokenAtivacaoTest.java`:
```java
package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.demo.cadastro.shared.NegocioException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TokenAtivacaoTest {

    private static final Instant CRIACAO = Instant.parse("2026-09-30T12:00:00Z");
    private static final Duration VALIDADE = Duration.ofHours(24);

    private TokenAtivacao novoToken() {
        return new TokenAtivacao(UUID.randomUUID(), "hash", CRIACAO, VALIDADE);
    }

    @Test
    void podeSerUsadoDentroDaValidade() {
        assertThatCode(() -> novoToken().usar(CRIACAO.plus(VALIDADE).minusSeconds(1))).doesNotThrowAnyException();
    }

    @Test
    void expiraExatamenteAoFimDaValidade() {
        assertThatThrownBy(() -> novoToken().usar(CRIACAO.plus(VALIDADE)))
                .isInstanceOf(NegocioException.class).hasFieldOrPropertyWithValue("codigo", "TOKEN_EXPIRADO");
    }

    @Test
    void naoPodeSerUsadoDuasVezes() {
        TokenAtivacao token = novoToken();
        token.usar(CRIACAO.plusSeconds(60));

        assertThatThrownBy(() -> token.usar(CRIACAO.plusSeconds(120)))
                .isInstanceOf(NegocioException.class).hasFieldOrPropertyWithValue("codigo", "TOKEN_JA_UTILIZADO");
    }
}
```

- [ ] **Passo 2: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest='GeradorTokenTest,TokenAtivacaoTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA de compilação; `GeradorToken` e `TokenAtivacao` não existem.

- [ ] **Passo 3: Implementar o token**

`backend/src/main/java/br/com/demo/cadastro/ativacao/GeradorToken.java`:
```java
package br.com.demo.cadastro.ativacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class GeradorToken {

    private final SecureRandom aleatorio = new SecureRandom();

    String gerar() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/ativacao/TokenAtivacao.java`:
```java
package br.com.demo.cadastro.ativacao;

import br.com.demo.cadastro.shared.NegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "token_ativacao")
class TokenAtivacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID usuarioId;
    private String tokenHash;
    private Instant expiraEm;
    private Instant usadoEm;
    private Instant criadoEm;

    protected TokenAtivacao() {}

    TokenAtivacao(UUID usuarioId, String tokenHash, Instant agora, Duration validade) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.criadoEm = agora;
        this.expiraEm = agora.plus(validade);
    }

    void usar(Instant agora) {
        if (usadoEm != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "TOKEN_JA_UTILIZADO",
                    "Este link de ativação já foi utilizado");
        }
        if (!agora.isBefore(expiraEm)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRADO", "Este link de ativação expirou");
        }
        usadoEm = agora;
    }

    UUID getUsuarioId() {
        return usuarioId;
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/ativacao/TokenAtivacaoRepository.java`:
```java
package br.com.demo.cadastro.ativacao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TokenAtivacaoRepository extends JpaRepository<TokenAtivacao, UUID> {

    Optional<TokenAtivacao> findByTokenHash(String tokenHash);
}
```

- [ ] **Passo 4: Rodar os testes unitários**

Rodar: `mvn -q -pl backend -am test -Dtest='GeradorTokenTest,TokenAtivacaoTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 5: Escrever os testes de integração que falham**

`backend/src/test/java/br/com/demo/cadastro/support/EmailsTeste.java`:
```java
package br.com.demo.cadastro.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailsTeste {

    private static final Pattern TOKEN = Pattern.compile("/ativar\\?token=([A-Za-z0-9_-]+)");

    private EmailsTeste() {}

    public static MimeMessage aguardarMensagem(GreenMailExtension greenMail, String destinatario) {
        return await().atMost(Duration.ofSeconds(10))
                .until(() -> mensagensPara(greenMail, destinatario), lista -> lista.size() == 1)
                .get(0);
    }

    public static List<MimeMessage> mensagensPara(GreenMailExtension greenMail, String destinatario) {
        return Arrays.stream(greenMail.getReceivedMessages())
                .filter(m -> destinatarios(m).contains(destinatario))
                .toList();
    }

    public static String conteudo(MimeMessage mensagem) {
        try {
            return extrair(mensagem.getContent());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String extrairToken(MimeMessage mensagem) {
        Matcher m = TOKEN.matcher(conteudo(mensagem));
        assertThat(m.find()).as("link de ativação presente no e-mail").isTrue();
        return m.group(1);
    }

    private static List<String> destinatarios(MimeMessage mensagem) {
        try {
            return Arrays.stream(mensagem.getAllRecipients()).map(Address::toString).toList();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String extrair(Object conteudo) throws Exception {
        if (conteudo instanceof String texto) {
            return texto;
        }
        if (conteudo instanceof Multipart partes) {
            StringBuilder resultado = new StringBuilder();
            for (int i = 0; i < partes.getCount(); i++) {
                resultado.append(extrair(partes.getBodyPart(i).getContent())).append('\n');
            }
            return resultado.toString();
        }
        return "";
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/ativacao/AtivacaoIntegrationTest.java`:
```java
package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.EmailsTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AtivacaoIntegrationTest extends IntegrationTest {

    private ResultActions ativar(String corpo) throws Exception {
        return mvc.perform(post("/api/ativacao").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions ativarToken(String token) throws Exception {
        return ativar("{\"token\":\"%s\"}".formatted(token));
    }

    @Test
    void cadastroEnviaEmailDeAtivacaoParaOEnderecoCadastrado() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());

        MimeMessage mensagem = EmailsTeste.aguardarMensagem(greenMail, email);

        assertThat(mensagem.getSubject()).isEqualTo("Ative sua conta");
        assertThat(mensagem.getFrom()[0].toString()).isEqualTo("cadastro@teste.local");
        assertThat(EmailsTeste.conteudo(mensagem))
                .contains("Olá, Maria da Silva!")
                .contains("http://localhost:8080/ativar?token=")
                .contains("24 horas");
    }

    @Test
    void linkDoEmailAtivaAContaUmaUnicaVez() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = cadastrar(email, DadosTeste.cpfValido());
        String token = EmailsTeste.extrairToken(EmailsTeste.aguardarMensagem(greenMail, email));

        String hashGravado = jdbc.queryForObject(
                "select token_hash from token_ativacao where usuario_id = ?::uuid", String.class, id);
        assertThat(hashGravado).isEqualTo(GeradorToken.hash(token)).isNotEqualTo(token);

        ativarToken(token).andExpect(status().isNoContent());
        assertThat(statusDoUsuario(id)).isEqualTo("ATIVO");

        ativarToken(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_JA_UTILIZADO"));
    }

    @Test
    void tokenExpiradoNaoAtivaAConta() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = cadastrar(email, DadosTeste.cpfValido());
        String token = "expirado-" + UUID.randomUUID();
        jdbc.update("""
                insert into token_ativacao (id, usuario_id, token_hash, expira_em, criado_em)
                values (gen_random_uuid(), ?::uuid, ?, now() - interval '1 minute', now() - interval '25 hours')
                """, id, GeradorToken.hash(token));

        ativarToken(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
        assertThat(statusDoUsuario(id)).isEqualTo("PENDENTE_ATIVACAO");
    }

    @Test
    void tokenDesconhecidoVazioOuAusenteEhInvalido() throws Exception {
        ativarToken("nao-existe").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        ativarToken("").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        ativar("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void nomeEhEscapadoNoHtmlDoEmail() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido(), "<b>Ana</b>");

        String conteudo = EmailsTeste.conteudo(EmailsTeste.aguardarMensagem(greenMail, email));

        assertThat(conteudo).contains("<p>Olá, &lt;b&gt;Ana&lt;/b&gt;!</p>").doesNotContain("<p>Olá, <b>Ana</b>");
    }
}
```

`backend/src/test/java/br/com/demo/cadastro/ativacao/AtivacaoFalhaEmailIntegrationTest.java`:
```java
package br.com.demo.cadastro.ativacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AtivacaoFalhaEmailIntegrationTest extends IntegrationTest {

    @MockitoBean
    EmailAtivacao emailAtivacao;

    @Test
    void falhaNoEnvioNaoDesfazOCadastroEDeixaOEventoPendente() throws Exception {
        doThrow(new MailSendException("SMTP indisponível"))
                .when(emailAtivacao).enviar(anyString(), anyString(), anyString());
        String email = DadosTeste.emailUnico();

        String id = cadastrar(email, DadosTeste.cpfValido());

        verify(emailAtivacao, timeout(5000)).enviar(eq(email), anyString(), anyString());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(statusDoUsuario(id)).isEqualTo("PENDENTE_ATIVACAO");
            assertThat(jdbc.queryForObject(
                    "select count(*) from token_ativacao where usuario_id = ?::uuid", Integer.class, id)).isZero();
            assertThat(jdbc.queryForObject(
                    "select count(*) from event_publication where completion_date is null and serialized_event like ?",
                    Integer.class, "%" + id + "%")).isEqualTo(1);
        });
    }
}
```

- [ ] **Passo 6: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest='AtivacaoIntegrationTest,AtivacaoFalhaEmailIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA de compilação; `EmailAtivacao` não existe.

- [ ] **Passo 7: Implementar o e-mail, o serviço e o endpoint**

`backend/src/main/java/br/com/demo/cadastro/ativacao/EmailAtivacao.java`:
```java
package br.com.demo.cadastro.ativacao;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
class EmailAtivacao {

    private final JavaMailSender mailSender;
    private final String remetente;
    private final long horasValidade;

    EmailAtivacao(JavaMailSender mailSender,
                  @Value("${spring.mail.username}") String remetente,
                  @Value("${app.ativacao.expiracao}") Duration validade) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.horasValidade = validade.toHours();
    }

    void enviar(String para, String nome, String link) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper ajudante = new MimeMessageHelper(mensagem, true, "UTF-8");
            ajudante.setFrom(remetente);
            ajudante.setTo(para);
            ajudante.setSubject("Ative sua conta");
            ajudante.setText(texto(nome, link), html(nome, link));
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            throw new MailPreparationException("Falha ao montar o e-mail de ativação", e);
        }
    }

    private String texto(String nome, String link) {
        return """
                Olá, %s!

                Recebemos o seu cadastro. Para ativar a sua conta, acesse o link abaixo:

                %s

                Este link é válido por %d horas.
                """.formatted(nome, link, horasValidade);
    }

    private String html(String nome, String link) {
        return """
                <p>Olá, %s!</p>
                <p>Recebemos o seu cadastro. Para ativar a sua conta, clique no link abaixo:</p>
                <p><a href="%s">Ativar minha conta</a></p>
                <p>Este link é válido por %d horas.</p>
                """.formatted(HtmlUtils.htmlEscape(nome), HtmlUtils.htmlEscape(link), horasValidade);
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/ativacao/AtivacaoService.java`:
```java
package br.com.demo.cadastro.ativacao;

import br.com.demo.cadastro.cadastro.UsuarioCadastrado;
import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.usuario.UsuarioService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AtivacaoService {

    private final TokenAtivacaoRepository tokens;
    private final GeradorToken gerador;
    private final EmailAtivacao email;
    private final UsuarioService usuarios;
    private final Clock clock;
    private final String baseUrl;
    private final Duration validade;

    AtivacaoService(TokenAtivacaoRepository tokens, GeradorToken gerador, EmailAtivacao email,
                    UsuarioService usuarios, Clock clock,
                    @Value("${app.base-url}") String baseUrl,
                    @Value("${app.ativacao.expiracao}") Duration validade) {
        this.tokens = tokens;
        this.gerador = gerador;
        this.email = email;
        this.usuarios = usuarios;
        this.clock = clock;
        this.baseUrl = baseUrl;
        this.validade = validade;
    }

    @ApplicationModuleListener
    public void aoCadastrarUsuario(UsuarioCadastrado evento) {
        String token = gerador.gerar();
        tokens.save(new TokenAtivacao(evento.usuarioId(), GeradorToken.hash(token), clock.instant(), validade));
        email.enviar(evento.email(), evento.nome(), baseUrl + "/ativar?token=" + token);
    }

    @Transactional
    public void ativar(String token) {
        if (token == null || token.isBlank()) {
            throw tokenInvalido();
        }
        TokenAtivacao registro = tokens.findByTokenHash(GeradorToken.hash(token)).orElseThrow(this::tokenInvalido);
        registro.usar(clock.instant());
        usuarios.ativar(registro.getUsuarioId());
    }

    private NegocioException tokenInvalido() {
        return new NegocioException(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "Link de ativação inválido");
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/ativacao/AtivacaoController.java`:
```java
package br.com.demo.cadastro.ativacao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AtivacaoController {

    record AtivacaoRequisicao(String token) {}

    private final AtivacaoService servico;

    AtivacaoController(AtivacaoService servico) {
        this.servico = servico;
    }

    @PostMapping("/api/ativacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void ativar(@RequestBody AtivacaoRequisicao requisicao) {
        servico.ativar(requisicao.token());
    }
}
```

- [ ] **Passo 8: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest='Ativacao*Test,GeradorTokenTest,TokenAtivacaoTest,ModularidadeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 9: Commit**

```bash
git add backend/src
git commit -m "feat(ativacao): envia e-mail com token de uso único e ativa a conta"
```

---

### Tarefa 8: `autenticacao` — login e logout (RF06, RN04)

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/autenticacao/AutenticacaoService.java`, `AutenticacaoController.java`
- Teste: `backend/src/test/java/br/com/demo/cadastro/autenticacao/AutenticacaoIntegrationTest.java`

**Interfaces:**
- Consome: `UsuarioService.buscarPorEmail`, `senhaConfere`, `Usuario.isAtivo` (Tarefa 4); os auxiliares `cadastrar` e `marcarComoAtivo` (Tarefa 6).
- Produz:
  - `POST /api/auth/login {email, senha}` → `200 {nome, email}`. Cria uma sessão nova cujo principal (`Authentication.getName()`) é o id do usuário em texto.
  - Erros do login: `401 CREDENCIAIS_INVALIDAS` e `403 CONTA_PENDENTE` (sem criar sessão).
  - `POST /api/auth/logout` → `204`.

- [ ] **Passo 1: Escrever o teste que falha**

`backend/src/test/java/br/com/demo/cadastro/autenticacao/AutenticacaoIntegrationTest.java`:
```java
package br.com.demo.cadastro.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class AutenticacaoIntegrationTest extends IntegrationTest {

    private ResultActions login(String email, String senha) throws Exception {
        return login(email, senha, new MockHttpSession());
    }

    private ResultActions login(String email, String senha, MockHttpSession sessao) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    @Test
    void usuarioAtivoEntraERecebeSessaoComSeuId() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);
        MockHttpSession sessaoAnterior = new MockHttpSession();

        MvcResult resultado = login(email.toUpperCase(), DadosTeste.SENHA, sessaoAnterior)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        assertThat(sessaoAnterior.isInvalid()).as("sessão anterior invalidada (fixação de sessão)").isTrue();
        HttpSession sessao = resultado.getRequest().getSession(false);
        SecurityContext contexto = (SecurityContext) sessao.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(contexto.getAuthentication().getName()).isEqualTo(id);
    }

    @Test
    void senhaErradaEEmailInexistenteRetornamOMesmo401() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);

        login(email, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
        login(DadosTeste.emailUnico(), DadosTeste.SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void usuarioPendenteComSenhaCorretaRecebe403SemSessao() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());

        MvcResult resultado = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, DadosTeste.SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CONTA_PENDENTE"))
                .andReturn();

        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    void usuarioPendenteComSenhaErradaRecebe401() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());

        login(email, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void senhaAcimaDe72BytesRetorna401EmVezDe500() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);

        login(email, "Aa1!" + "é".repeat(40)).andExpect(status().isUnauthorized());
    }

    @Test
    void corpoSemCamposRetorna400() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void logoutInvalidaASessao() throws Exception {
        String email = DadosTeste.emailUnico();
        cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);
        MockHttpSession sessao = (MockHttpSession) login(email, DadosTeste.SENHA)
                .andReturn().getRequest().getSession(false);

        mvc.perform(post("/api/auth/logout").with(csrf()).session(sessao)).andExpect(status().isNoContent());

        assertThat(sessao.isInvalid()).isTrue();
    }

    @Test
    void logoutSemSessaoRetorna401() throws Exception {
        mvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Passo 2: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest=AutenticacaoIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA; `POST /api/auth/login` responde 404.

- [ ] **Passo 3: Implementar**

`backend/src/main/java/br/com/demo/cadastro/autenticacao/AutenticacaoService.java`:
```java
package br.com.demo.cadastro.autenticacao;

import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.usuario.Usuario;
import br.com.demo.cadastro.usuario.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
class AutenticacaoService {

    private final UsuarioService usuarios;

    AutenticacaoService(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarios.buscarPorEmail(email)
                .filter(u -> usuarios.senhaConfere(u, senha))
                .orElseThrow(() -> new NegocioException(
                        HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos"));
        if (!usuario.isAtivo()) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "CONTA_PENDENTE",
                    "Sua conta ainda não foi ativada. Verifique seu e-mail.");
        }
        return usuario;
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/autenticacao/AutenticacaoController.java`:
```java
package br.com.demo.cadastro.autenticacao;

import br.com.demo.cadastro.shared.Mensagens;
import br.com.demo.cadastro.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AutenticacaoController {

    record LoginRequisicao(
            @NotBlank(message = Mensagens.OBRIGATORIO) String email,
            @NotBlank(message = Mensagens.OBRIGATORIO) String senha) {}

    record UsuarioLogado(String nome, String email) {}

    private final AutenticacaoService servico;
    private final SecurityContextRepository repositorioContexto = new HttpSessionSecurityContextRepository();

    AutenticacaoController(AutenticacaoService servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    UsuarioLogado login(@Valid @RequestBody LoginRequisicao requisicao,
                        HttpServletRequest request, HttpServletResponse response) {
        Usuario usuario = servico.autenticar(requisicao.email(), requisicao.senha());

        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.getId().toString(), null, List.of()));
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, request, response);

        return new UsuarioLogado(usuario.getNome(), usuario.getEmail());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
```

- [ ] **Passo 4: Rodar e confirmar que passa**

Rodar: `mvn -q -pl backend -am test -Dtest='AutenticacaoIntegrationTest,SegurancaIntegrationTest,ModularidadeTest' -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: PASSA.

- [ ] **Passo 5: Commit**

```bash
git add backend/src
git commit -m "feat(autenticacao): login por sessão bloqueando contas pendentes e logout"
```

---

### Tarefa 9: `perfil` — `GET/PUT /api/perfil` (RF07, RN01)

**Arquivos:**
- Criar: `backend/src/main/java/br/com/demo/cadastro/perfil/PerfilResposta.java`, `AtualizacaoPerfil.java`, `PerfilController.java`
- Teste: `backend/src/test/java/br/com/demo/cadastro/perfil/PerfilIntegrationTest.java`

**Interfaces:**
- Consome: `UsuarioService.buscarPorId` e `atualizarContato`, `EnderecoDados` (Tarefa 4); o principal autenticado igual ao id do usuário (Tarefa 8).
- Produz:
  - `GET /api/perfil` → `200 {nome, cpf, email, dataNascimento, telefone, endereco{cep, logradouro, numero, complemento, bairro, cidade, uf}, status}`.
  - `PUT /api/perfil {telefone, endereco}` → `200`, com o mesmo formato.

- [ ] **Passo 1: Escrever o teste que falha**

`backend/src/test/java/br/com/demo/cadastro/perfil/PerfilIntegrationTest.java`:
```java
package br.com.demo.cadastro.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.DadosTeste;
import br.com.demo.cadastro.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class PerfilIntegrationTest extends IntegrationTest {

    private static final String ATUALIZACAO = """
            {"telefone":"2133334444",
             "endereco":{"cep":"20040002","logradouro":"Rua da Assembleia","numero":"S/N","complemento":"",
                         "bairro":"Centro","cidade":"Rio de Janeiro","uf":"RJ"}}
            """;

    private static RequestPostProcessor como(String id) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(id, null, List.of()));
    }

    private String usuarioAtivo(String email) throws Exception {
        String id = cadastrar(email, DadosTeste.cpfValido());
        marcarComoAtivo(email);
        return id;
    }

    @Test
    void retornaPerfilCompletoSemSenha() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = usuarioAtivo(email);

        mvc.perform(get("/api/perfil").with(como(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.cpf").isString())
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"))
                .andExpect(jsonPath("$.telefone").value("11987654321"))
                .andExpect(jsonPath("$.endereco.cep").value("01310100"))
                .andExpect(jsonPath("$.endereco.complemento").value("Apto 12"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void atualizaTelefoneEEndereco() throws Exception {
        String id = usuarioAtivo(DadosTeste.emailUnico());

        mvc.perform(put("/api/perfil").with(como(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ATUALIZACAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("2133334444"))
                .andExpect(jsonPath("$.endereco.cidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.endereco.complemento").value(org.hamcrest.Matchers.nullValue()));

        Map<String, Object> linha = jdbc.queryForMap(
                "select telefone, cep, complemento from usuario where id = ?::uuid", id);
        assertThat(linha).containsEntry("telefone", "2133334444").containsEntry("cep", "20040002");
        assertThat(linha.get("complemento")).isNull();
    }

    @Test
    void ignoraCamposImutaveisEnviadosNoCorpo() throws Exception {
        String email = DadosTeste.emailUnico();
        String id = usuarioAtivo(email);
        Map<String, Object> antes = jdbc.queryForMap(
                "select nome, cpf, email, data_nascimento from usuario where id = ?::uuid", id);
        String corpo = ATUALIZACAO.replaceFirst("\\{",
                "{\"nome\":\"Outro Nome\",\"cpf\":\"52998224725\",\"email\":\"outro@teste.local\","
                        + "\"dataNascimento\":\"2000-01-01\",");

        mvc.perform(put("/api/perfil").with(como(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForMap("select nome, cpf, email, data_nascimento from usuario where id = ?::uuid", id))
                .isEqualTo(antes);
    }

    @Test
    void validaTelefoneEEnderecoObrigatorio() throws Exception {
        String id = usuarioAtivo(DadosTeste.emailUnico());

        mvc.perform(put("/api/perfil").with(como(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'telefone')].mensagem")
                        .value(org.hamcrest.Matchers.contains("Telefone inválido")))
                .andExpect(jsonPath("$.erros[?(@.campo == 'endereco')].mensagem")
                        .value(org.hamcrest.Matchers.contains("Campo obrigatório")));
    }

    @Test
    void exigeAutenticacaoECsrf() throws Exception {
        String id = usuarioAtivo(DadosTeste.emailUnico());

        mvc.perform(put("/api/perfil").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(ATUALIZACAO))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil").with(como(id)).contentType(MediaType.APPLICATION_JSON).content(ATUALIZACAO))
                .andExpect(status().isForbidden());
    }

    @Test
    void fluxoCompletoComSessaoReal() throws Exception {
        String email = DadosTeste.emailUnico();
        usuarioAtivo(email);
        MockHttpSession sessao = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, DadosTeste.SENHA)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/perfil").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
```

- [ ] **Passo 2: Rodar e confirmar a falha**

Rodar: `mvn -q -pl backend -am test -Dtest=PerfilIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA; `/api/perfil` responde 404 para usuário autenticado.

- [ ] **Passo 3: Implementar**

`backend/src/main/java/br/com/demo/cadastro/perfil/PerfilResposta.java`:
```java
package br.com.demo.cadastro.perfil;

import br.com.demo.cadastro.usuario.EnderecoDados;
import br.com.demo.cadastro.usuario.Usuario;
import java.time.LocalDate;

record PerfilResposta(String nome, String cpf, String email, LocalDate dataNascimento, String telefone,
                      EnderecoDados endereco, String status) {

    static PerfilResposta de(Usuario usuario) {
        return new PerfilResposta(usuario.getNome(), usuario.getCpf(), usuario.getEmail(),
                usuario.getDataNascimento(), usuario.getTelefone(), EnderecoDados.de(usuario.getEndereco()),
                usuario.getStatus().name());
    }
}
```

`backend/src/main/java/br/com/demo/cadastro/perfil/AtualizacaoPerfil.java`:
```java
package br.com.demo.cadastro.perfil;

import br.com.demo.cadastro.shared.Mensagens;
import br.com.demo.cadastro.usuario.EnderecoDados;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record AtualizacaoPerfil(
        @NotBlank(message = Mensagens.OBRIGATORIO)
        @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
        String telefone,

        @NotNull(message = Mensagens.OBRIGATORIO)
        @Valid
        EnderecoDados endereco) {}
```

`backend/src/main/java/br/com/demo/cadastro/perfil/PerfilController.java`:
```java
package br.com.demo.cadastro.perfil;

import br.com.demo.cadastro.shared.NegocioException;
import br.com.demo.cadastro.usuario.UsuarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
class PerfilController {

    private final UsuarioService usuarios;

    PerfilController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    PerfilResposta obter(Authentication autenticacao) {
        return usuarios.buscarPorId(idDe(autenticacao))
                .map(PerfilResposta::de)
                .orElseThrow(() -> new NegocioException(
                        HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Autenticação necessária"));
    }

    @PutMapping
    PerfilResposta atualizar(Authentication autenticacao, @Valid @RequestBody AtualizacaoPerfil requisicao) {
        return PerfilResposta.de(
                usuarios.atualizarContato(idDe(autenticacao), requisicao.telefone(), requisicao.endereco()));
    }

    private static UUID idDe(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}
```

- [ ] **Passo 4: Rodar a suíte inteira do backend**

Rodar: `mvn -q -pl backend -am verify`
Esperado: BUILD SUCCESS, com todos os testes do backend passando, inclusive `ModularidadeTest`.

- [ ] **Passo 5: Commit**

```bash
git add backend/src
git commit -m "feat(perfil): consulta e edição de telefone e endereço preservando dados imutáveis"
```

---

### Tarefa 10: Módulo `frontend`, cliente HTTP e entrega da SPA pelo backend

**Arquivos:**
- Criar: `frontend/pom.xml`, `frontend/package.json`, `frontend/index.html`, `frontend/vite.config.ts`, `frontend/tsconfig.json`
- Criar: `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/styles.css`, `frontend/src/api/cliente.ts`, `frontend/src/api/tipos.ts`
- Criar: `frontend/src/test/setup.ts`, `frontend/src/test/servidor.ts`
- Gerar: `frontend/package-lock.json` (via `npm install`)
- Criar: `backend/src/main/java/br/com/demo/cadastro/shared/SpaController.java`
- Modificar: `pom.xml` (incluir o módulo `frontend` antes de `backend`), `backend/pom.xml` (dependência `frontend`)
- Teste: `frontend/src/api/cliente.test.ts`, `backend/src/test/java/br/com/demo/cadastro/shared/SpaControllerTest.java`

**Interfaces:**
- Consome: o contrato da API das Tarefas 5–9.
- Produz:
  - `requisitar<T>(metodo: 'GET' | 'POST' | 'PUT', caminho: string, corpo?: unknown): Promise<T>` — envia `X-XSRF-TOKEN` em métodos que não são GET, busca `/api/csrf` se o cookie faltar e retorna `undefined` para `204`.
  - `class ApiError extends Error { status: number; codigo?: string; erros: ErroCampo[] }`.
  - Os tipos `ErroCampo`, `EnderecoDados`, `Perfil`, `UsuarioLogado` e `CadastroResposta`.
  - O servidor MSW `servidor`, com o cookie `XSRF-TOKEN=token-teste` em todo teste.
  - O backend encaminha `/`, `/{rota}` e `/{rota}/{sub}` (fora de `/api`, sem ponto) para `/index.html`.
  - `App` mínimo, que a Tarefa 14 substitui.

- [ ] **Passo 1: Criar o módulo e a configuração do frontend**

`frontend/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>br.com.demo.cadastro</groupId>
    <artifactId>cadastro</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>frontend</artifactId>
  <name>Cadastro - Frontend</name>
  <description>SPA React empacotada em META-INF/resources</description>

  <properties>
    <skipTests>false</skipTests>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <version>2.0.2</version>
        <configuration>
          <nodeVersion>v24.21.0</nodeVersion>
        </configuration>
        <executions>
          <execution>
            <id>instalar-node-e-npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
          </execution>
          <execution>
            <id>npm-ci</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>ci</arguments></configuration>
          </execution>
          <execution>
            <id>npm-build</id>
            <goals><goal>npm</goal></goals>
            <phase>generate-resources</phase>
            <configuration><arguments>run build</arguments></configuration>
          </execution>
          <execution>
            <id>npm-test</id>
            <goals><goal>npm</goal></goals>
            <phase>test</phase>
            <configuration>
              <arguments>run test</arguments>
              <skip>${skipTests}</skip>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

`frontend/package.json`:
```json
{
  "name": "cadastro-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc --noEmit && vite build",
    "test": "vitest run",
    "test:watch": "vitest"
  },
  "dependencies": {
    "@hookform/resolvers": "^5.9.1",
    "react": "^19.3.0",
    "react-dom": "^19.3.0",
    "react-hook-form": "^7.89.0",
    "react-router": "^7.18.4",
    "zod": "^4.6.5"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^7.0.1",
    "@testing-library/react": "^16.3.3",
    "@testing-library/user-event": "^14.6.7",
    "@types/react": "^19.3.0",
    "@types/react-dom": "^19.3.0",
    "@vitejs/plugin-react": "^6.1.1",
    "jsdom": "^30.1.1",
    "msw": "^2.15.0",
    "typescript": "^5.9.3",
    "vite": "^8.3.1",
    "vitest": "^5.0.3"
  }
}
```

`frontend/index.html`:
```html
<!doctype html>
<html lang="pt-BR">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Cadastro de Usuários</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

`frontend/vite.config.ts`:
```ts
/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  build: {
    outDir: 'target/classes/META-INF/resources',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
});
```

`frontend/tsconfig.json`:
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noEmit": true,
    "skipLibCheck": true,
    "isolatedModules": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  },
  "include": ["src", "vite.config.ts"]
}
```

Rodar: `cd frontend && npm install && cd ..` para gerar o `package-lock.json`.

- [ ] **Passo 2: Criar a infraestrutura de teste e o teste do cliente, que falha**

`frontend/src/test/servidor.ts`:
```ts
import { setupServer } from 'msw/node';

export const servidor = setupServer();
```

`frontend/src/test/setup.ts`:
```ts
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';
import { servidor } from './servidor';

beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }));
beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=token-teste';
});
afterEach(() => {
  cleanup();
  servidor.resetHandlers();
});
afterAll(() => servidor.close());
```

`frontend/src/api/cliente.test.ts`:
```ts
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { servidor } from '../test/servidor';
import { ApiError, requisitar } from './cliente';

describe('requisitar', () => {
  it('envia JSON com o token CSRF do cookie em requisições mutáveis', async () => {
    let cabecalho: string | null = null;
    let corpo: unknown;
    servidor.use(
      http.post('/api/exemplo', async ({ request }) => {
        cabecalho = request.headers.get('X-XSRF-TOKEN');
        corpo = await request.json();
        return HttpResponse.json({ ok: true });
      }),
    );

    const resposta = await requisitar<{ ok: boolean }>('POST', '/api/exemplo', { a: 1 });

    expect(resposta).toEqual({ ok: true });
    expect(cabecalho).toBe('token-teste');
    expect(corpo).toEqual({ a: 1 });
  });

  it('busca o token em /api/csrf quando o cookie não existe', async () => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    let csrfChamado = false;
    servidor.use(
      http.get('/api/csrf', () => {
        csrfChamado = true;
        document.cookie = 'XSRF-TOKEN=token-novo';
        return HttpResponse.json({ token: 'token-novo' });
      }),
      http.post('/api/exemplo', ({ request }) =>
        HttpResponse.json({ cabecalho: request.headers.get('X-XSRF-TOKEN') }),
      ),
    );

    const resposta = await requisitar<{ cabecalho: string }>('POST', '/api/exemplo', {});

    expect(csrfChamado).toBe(true);
    expect(resposta.cabecalho).toBe('token-novo');
  });

  it('retorna undefined para 204', async () => {
    servidor.use(http.post('/api/exemplo', () => new HttpResponse(null, { status: 204 })));

    await expect(requisitar('POST', '/api/exemplo')).resolves.toBeUndefined();
  });

  it('converte problem+json em ApiError', async () => {
    servidor.use(
      http.post('/api/exemplo', () =>
        HttpResponse.json(
          {
            detail: 'Dados inválidos',
            codigo: 'VALIDACAO',
            erros: [{ campo: 'cpf', mensagem: 'CPF inválido' }],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const erro = await requisitar('POST', '/api/exemplo', {}).catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ApiError);
    expect(erro).toMatchObject({
      status: 400,
      codigo: 'VALIDACAO',
      message: 'Dados inválidos',
      erros: [{ campo: 'cpf', mensagem: 'CPF inválido' }],
    });
  });

  it('usa mensagem genérica quando a resposta de erro não é JSON', async () => {
    servidor.use(http.get('/api/exemplo', () => new HttpResponse('falhou', { status: 500 })));

    const erro = await requisitar('GET', '/api/exemplo').catch((e: unknown) => e);

    expect(erro).toMatchObject({ status: 500, message: 'Erro inesperado', erros: [] });
  });
});
```

Rodar: `cd frontend && npx vitest run src/api/cliente.test.ts; cd ..`
Esperado: FALHA; `./cliente` não existe.

- [ ] **Passo 3: Implementar o cliente, os tipos e o App mínimo**

`frontend/src/api/tipos.ts`:
```ts
export interface ErroCampo {
  campo: string;
  mensagem: string;
}

export interface EnderecoDados {
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: string;
}

export interface Perfil {
  nome: string;
  cpf: string;
  email: string;
  dataNascimento: string;
  telefone: string;
  endereco: EnderecoDados;
  status: string;
}

export interface UsuarioLogado {
  nome: string;
  email: string;
}

export interface CadastroResposta {
  id: string;
  email: string;
  status: string;
}
```

`frontend/src/api/cliente.ts`:
```ts
import type { ErroCampo } from './tipos';

export class ApiError extends Error {
  readonly status: number;
  readonly codigo?: string;
  readonly erros: ErroCampo[];

  constructor(status: number, mensagem: string, codigo?: string, erros: ErroCampo[] = []) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
    this.erros = erros;
  }
}

type Metodo = 'GET' | 'POST' | 'PUT';

function urlAbsoluta(caminho: string): string {
  return new URL(caminho, window.location.origin).toString();
}

function lerCookie(nome: string): string | undefined {
  const par = document.cookie.split('; ').find((c) => c.startsWith(`${nome}=`));
  const valor = par?.substring(nome.length + 1);
  return valor ? decodeURIComponent(valor) : undefined;
}

async function obterTokenCsrf(): Promise<string> {
  let token = lerCookie('XSRF-TOKEN');
  if (!token) {
    await fetch(urlAbsoluta('/api/csrf'), { credentials: 'same-origin' });
    token = lerCookie('XSRF-TOKEN');
  }
  return token ?? '';
}

export async function requisitar<T>(metodo: Metodo, caminho: string, corpo?: unknown): Promise<T> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json' };
  if (corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json';
  }
  if (metodo !== 'GET') {
    cabecalhos['X-XSRF-TOKEN'] = await obterTokenCsrf();
  }

  const resposta = await fetch(urlAbsoluta(caminho), {
    method: metodo,
    headers: cabecalhos,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
    credentials: 'same-origin',
  });

  if (!resposta.ok) {
    let problema: { detail?: string; codigo?: string; erros?: ErroCampo[] } = {};
    try {
      problema = await resposta.json();
    } catch {
      // resposta sem corpo JSON
    }
    throw new ApiError(resposta.status, problema.detail ?? 'Erro inesperado', problema.codigo, problema.erros ?? []);
  }

  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}
```

`frontend/src/App.tsx` (temporário, substituído na Tarefa 14):
```tsx
export function App() {
  return <h1>Cadastro de Usuários</h1>;
}
```

`frontend/src/main.tsx`:
```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import { App } from './App';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
```

`frontend/src/styles.css`:
```css
:root {
  --cor-fundo: #f4f6f8;
  --cor-cartao: #ffffff;
  --cor-texto: #1f2933;
  --cor-suave: #616e7c;
  --cor-primaria: #2563eb;
  --cor-erro: #b42318;
  --cor-sucesso: #027a48;
  --cor-borda: #cbd2d9;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  color: var(--cor-texto);
  background: var(--cor-fundo);
}

* { box-sizing: border-box; }
body { margin: 0; }

.layout header { background: var(--cor-primaria); color: #fff; padding: 12px 16px; }
.layout header h1 { margin: 0; font-size: 1.1rem; }
.layout main { max-width: 640px; margin: 24px auto; padding: 0 16px; }

.cartao { background: var(--cor-cartao); border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgb(0 0 0 / 0.1); }
.cartao h1 { margin-top: 0; font-size: 1.4rem; }

fieldset { border: 1px solid var(--cor-borda); border-radius: 6px; padding: 12px 16px; margin: 16px 0; }
legend { padding: 0 4px; font-weight: 600; }

.campo { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
.campo label { font-weight: 500; }
.campo input, .campo select { padding: 8px 10px; border: 1px solid var(--cor-borda); border-radius: 4px; font-size: 1rem; }
.campo input[aria-invalid='true'], .campo select[aria-invalid='true'] { border-color: var(--cor-erro); }
.erro-campo { color: var(--cor-erro); font-size: 0.875rem; }

.forca-senha { list-style: none; padding: 0; margin: -4px 0 12px; font-size: 0.85rem; color: var(--cor-suave); }
.forca-senha li.ok { color: var(--cor-sucesso); }

button { background: var(--cor-primaria); color: #fff; border: none; border-radius: 4px; padding: 10px 16px; font-size: 1rem; cursor: pointer; }
button:disabled { opacity: 0.6; cursor: default; }
button.secundario { background: transparent; color: var(--cor-primaria); border: 1px solid var(--cor-primaria); }
.acoes { display: flex; gap: 8px; margin-top: 8px; }

.alerta { padding: 10px 12px; border-radius: 4px; margin-bottom: 16px; }
.alerta-erro { background: #fef3f2; color: var(--cor-erro); }
.alerta-sucesso { background: #ecfdf3; color: var(--cor-sucesso); }

dl.dados { display: grid; grid-template-columns: max-content 1fr; gap: 8px 16px; }
dl.dados dt { font-weight: 600; color: var(--cor-suave); }
dl.dados dd { margin: 0; }
```

- [ ] **Passo 4: Rodar o teste do cliente**

Rodar: `cd frontend && npx vitest run src/api/cliente.test.ts && npm run build; cd ..`
Esperado: PASSA (5 testes), e o build gera `frontend/target/classes/META-INF/resources/index.html`.

- [ ] **Passo 5: Integrar ao Maven e escrever o teste da SPA, que falha**

Em `pom.xml` (raiz), troque o bloco `<modules>` por:
```xml
  <modules>
    <module>database</module>
    <module>frontend</module>
    <module>backend</module>
  </modules>
```

Em `backend/pom.xml`, logo depois da dependência `database`, adicione:
```xml
    <dependency>
      <groupId>br.com.demo.cadastro</groupId>
      <artifactId>frontend</artifactId>
      <version>${project.version}</version>
      <scope>runtime</scope>
    </dependency>
```

`backend/src/test/java/br/com/demo/cadastro/shared/SpaControllerTest.java`:
```java
package br.com.demo.cadastro.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.demo.cadastro.support.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class SpaControllerTest extends IntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/cadastro", "/cadastro/concluido", "/ativar", "/login", "/perfil"})
    void rotasDaSpaSaoEncaminhadasParaOIndex(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void rotasDaApiNaoSaoEncaminhadas() throws Exception {
        mvc.perform(get("/api/inexistente")).andExpect(status().isUnauthorized());
    }

    @Test
    void arquivosComExtensaoNaoSaoEncaminhados() throws Exception {
        mvc.perform(get("/assets/nao-existe.js")).andExpect(status().isNotFound());
    }
}
```

Rodar: `mvn -q -pl backend -am test -Dtest=SpaControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Esperado: FALHA; `/cadastro` responde 404 sem encaminhamento.

- [ ] **Passo 6: Implementar o encaminhamento da SPA**

`backend/src/main/java/br/com/demo/cadastro/shared/SpaController.java`:
```java
package br.com.demo.cadastro.shared;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaController {

    @GetMapping({"/", "/{rota:(?!api$)[^.]*}", "/{rota:(?!api$)[^.]*}/{subrota:[^.]*}"})
    String encaminhar() {
        return "forward:/index.html";
    }
}
```

- [ ] **Passo 7: Rodar o build completo**

Rodar: `mvn -q verify`
Esperado: BUILD SUCCESS. O frontend é instalado e testado, e o backend passa todos os testes, inclusive `SpaControllerTest`.

Dica para os próximos ciclos só do backend: `mvn -q -pl backend -am test -Dskip.npm -Dskip.installnodenpm -Dtest=...`.

- [ ] **Passo 8: Commit**

```bash
git add pom.xml backend frontend/pom.xml frontend/package.json frontend/package-lock.json frontend/index.html frontend/vite.config.ts frontend/tsconfig.json frontend/src
git commit -m "build(frontend): adiciona SPA React ao monolito e cliente HTTP com CSRF"
```

---

### Tarefa 11: Frontend — regras, máscaras e esquemas de validação (RF02)

**Arquivos:**
- Criar: `frontend/src/validacao/regras.ts`, `frontend/src/validacao/mascaras.ts`, `frontend/src/validacao/esquemas.ts`
- Teste: `frontend/src/validacao/regras.test.ts`, `mascaras.test.ts`, `esquemas.test.ts`

**Interfaces:**
- Consome: os tipos `Perfil` e `EnderecoDados` (Tarefa 10).
- Produz:
  - Regras: `soDigitos`, `cpfValido`, `CRITERIOS_SENHA: {rotulo, teste}[]`, `senhaForte`, `senhaExcedeLimite`, `dataPassada`, `emailValido`, `UFS`.
  - Máscaras: `mascaraCpf`, `mascaraTelefone`, `mascaraCep`, `formatarData` (de `yyyy-mm-dd` para `dd/mm/aaaa`).
  - Esquemas: `OBRIGATORIO`, `MENSAGEM_SENHA_FRACA`, `MENSAGEM_SENHA_LONGA`, `cadastroSchema`, `perfilSchema`, `loginSchema`.
  - Tipos: `CadastroForm`, `PerfilForm`, `LoginForm`, `EnderecoForm`; a constante `ENDERECO_VAZIO`.
  - Conversores: `cadastroParaRequisicao`, `perfilParaFormulario`, `perfilParaRequisicao`.

- [ ] **Passo 1: Escrever os testes que falham**

`frontend/src/validacao/regras.test.ts`:
```ts
import { describe, expect, it } from 'vitest';
import { cpfValido, dataPassada, emailValido, senhaExcedeLimite, senhaForte, soDigitos } from './regras';

describe('regras', () => {
  it('extrai só os dígitos', () => {
    expect(soDigitos('529.982.247-25')).toBe('52998224725');
  });

  it.each(['52998224725', '529.982.247-25', '11144477735'])('aceita CPF válido %s', (cpf) => {
    expect(cpfValido(cpf)).toBe(true);
  });

  it.each(['52998224724', '11111111111', '5299822472', ''])('rejeita CPF inválido %s', (cpf) => {
    expect(cpfValido(cpf)).toBe(false);
  });

  it('exige maiúscula, minúscula, número, especial e 8 caracteres', () => {
    expect(senhaForte('Senha@123')).toBe(true);
    expect(senhaForte('Sen@1')).toBe(false);
    expect(senhaForte('senha@123')).toBe(false);
    expect(senhaForte('SENHA@123')).toBe(false);
    expect(senhaForte('Senha@abc')).toBe(false);
    expect(senhaForte('Senha1234')).toBe(false);
  });

  it('conta o limite da senha em bytes UTF-8', () => {
    expect(senhaExcedeLimite('Aa1!' + 'x'.repeat(68))).toBe(false);
    expect(senhaExcedeLimite('Aa1!' + 'é'.repeat(35))).toBe(true);
  });

  it('aceita só datas anteriores a hoje', () => {
    expect(dataPassada('1990-05-20')).toBe(true);
    expect(dataPassada('2999-01-01')).toBe(false);
    expect(dataPassada('')).toBe(false);
  });

  it('valida formato de e-mail', () => {
    expect(emailValido('maria@teste.local')).toBe(true);
    expect(emailValido('maria@')).toBe(false);
    expect(emailValido('maria teste@x.com')).toBe(false);
  });
});
```

`frontend/src/validacao/mascaras.test.ts`:
```ts
import { describe, expect, it } from 'vitest';
import { formatarData, mascaraCep, mascaraCpf, mascaraTelefone } from './mascaras';

describe('máscaras', () => {
  it('formata CPF progressivamente', () => {
    expect(mascaraCpf('529')).toBe('529');
    expect(mascaraCpf('5299822')).toBe('529.982.2');
    expect(mascaraCpf('52998224725')).toBe('529.982.247-25');
    expect(mascaraCpf('529.982.247-2599')).toBe('529.982.247-25');
  });

  it('formata telefone fixo e celular', () => {
    expect(mascaraTelefone('11')).toBe('(11');
    expect(mascaraTelefone('1133334444')).toBe('(11) 3333-4444');
    expect(mascaraTelefone('11987654321')).toBe('(11) 98765-4321');
  });

  it('formata CEP', () => {
    expect(mascaraCep('01310')).toBe('01310');
    expect(mascaraCep('01310100')).toBe('01310-100');
  });

  it('formata data ISO para o padrão brasileiro', () => {
    expect(formatarData('1990-05-20')).toBe('20/05/1990');
  });
});
```

`frontend/src/validacao/esquemas.test.ts`:
```ts
import { describe, expect, it } from 'vitest';
import type { ZodError } from 'zod';
import type { Perfil } from '../api/tipos';
import {
  cadastroParaRequisicao,
  cadastroSchema,
  MENSAGEM_SENHA_FRACA,
  MENSAGEM_SENHA_LONGA,
  OBRIGATORIO,
  perfilParaFormulario,
  perfilParaRequisicao,
  type CadastroForm,
} from './esquemas';

function mensagens(resultado: { success: true } | { success: false; error: ZodError }) {
  const mapa: Record<string, string> = {};
  if (!resultado.success) {
    for (const issue of resultado.error.issues) {
      mapa[issue.path.join('.')] ??= issue.message;
    }
  }
  return mapa;
}

const valido: CadastroForm = {
  nome: 'Maria da Silva',
  cpf: '529.982.247-25',
  email: 'maria@teste.local',
  dataNascimento: '1990-05-20',
  senha: 'Senha@123',
  confirmacaoSenha: 'Senha@123',
  telefone: '(11) 98765-4321',
  endereco: {
    cep: '01310-100',
    logradouro: 'Avenida Paulista',
    numero: '1000',
    complemento: '',
    bairro: 'Bela Vista',
    cidade: 'São Paulo',
    uf: 'SP',
  },
};

describe('cadastroSchema', () => {
  it('aceita formulário válido sem complemento', () => {
    expect(cadastroSchema.safeParse(valido).success).toBe(true);
  });

  it('marca obrigatórios com a mensagem da spec', () => {
    const vazio = mensagens(
      cadastroSchema.safeParse({
        ...valido,
        nome: ' ',
        cpf: '',
        senha: '',
        endereco: { ...valido.endereco, cep: '', uf: '' },
      }),
    );
    expect(vazio).toMatchObject({
      nome: OBRIGATORIO,
      cpf: OBRIGATORIO,
      senha: OBRIGATORIO,
      'endereco.cep': OBRIGATORIO,
      'endereco.uf': OBRIGATORIO,
    });
  });

  it('valida formatos', () => {
    const erros = mensagens(
      cadastroSchema.safeParse({
        ...valido,
        cpf: '529.982.247-24',
        email: 'maria@',
        dataNascimento: '2999-01-01',
        senha: 'fraca',
        confirmacaoSenha: 'fraca',
        telefone: '(11) 123',
        endereco: { ...valido.endereco, cep: '0131', uf: 'XX' },
      }),
    );
    expect(erros).toMatchObject({
      cpf: 'CPF inválido',
      email: 'E-mail inválido',
      dataNascimento: 'Data de nascimento inválida',
      senha: MENSAGEM_SENHA_FRACA,
      telefone: 'Telefone inválido',
      'endereco.cep': 'CEP inválido',
      'endereco.uf': 'UF inválida',
    });
  });

  it('rejeita senha acima de 72 bytes', () => {
    const senha = 'Aa1!' + 'é'.repeat(40);
    expect(mensagens(cadastroSchema.safeParse({ ...valido, senha, confirmacaoSenha: senha })).senha).toBe(
      MENSAGEM_SENHA_LONGA,
    );
  });

  it('acusa senhas diferentes mesmo com outros campos inválidos', () => {
    const erros = mensagens(cadastroSchema.safeParse({ ...valido, nome: '', confirmacaoSenha: 'Outra@123' }));
    expect(erros.confirmacaoSenha).toBe('As senhas não conferem');
  });
});

describe('conversões', () => {
  it('envia só dígitos, complemento nulo e sem confirmação de senha', () => {
    expect(cadastroParaRequisicao(valido)).toEqual({
      nome: 'Maria da Silva',
      cpf: '52998224725',
      email: 'maria@teste.local',
      dataNascimento: '1990-05-20',
      senha: 'Senha@123',
      telefone: '11987654321',
      endereco: {
        cep: '01310100',
        logradouro: 'Avenida Paulista',
        numero: '1000',
        complemento: null,
        bairro: 'Bela Vista',
        cidade: 'São Paulo',
        uf: 'SP',
      },
    });
  });

  it('converte perfil para formulário mascarado e de volta', () => {
    const perfil: Perfil = {
      nome: 'Maria da Silva',
      cpf: '52998224725',
      email: 'maria@teste.local',
      dataNascimento: '1990-05-20',
      telefone: '11987654321',
      endereco: { ...valido.endereco, cep: '01310100', complemento: null },
      status: 'ATIVO',
    };
    const formulario = perfilParaFormulario(perfil);
    expect(formulario.telefone).toBe('(11) 98765-4321');
    expect(formulario.endereco.cep).toBe('01310-100');
    expect(formulario.endereco.complemento).toBe('');
    expect(perfilParaRequisicao(formulario)).toEqual({
      telefone: '11987654321',
      endereco: { ...perfil.endereco },
    });
  });
});
```

Rodar: `cd frontend && npx vitest run src/validacao; cd ..`
Esperado: FALHA; os módulos não existem.

- [ ] **Passo 2: Implementar**

`frontend/src/validacao/regras.ts`:
```ts
export const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
] as const;

export const LIMITE_BYTES_SENHA = 72;

export const CRITERIOS_SENHA: { rotulo: string; teste: (senha: string) => boolean }[] = [
  { rotulo: 'Mínimo de 8 caracteres', teste: (s) => s.length >= 8 },
  { rotulo: 'Letra maiúscula', teste: (s) => /[A-Z]/.test(s) },
  { rotulo: 'Letra minúscula', teste: (s) => /[a-z]/.test(s) },
  { rotulo: 'Número', teste: (s) => /\d/.test(s) },
  { rotulo: 'Caractere especial', teste: (s) => /[^A-Za-z0-9]/.test(s) },
];

export function soDigitos(valor: string): string {
  return valor.replace(/\D/g, '');
}

export function cpfValido(valor: string): boolean {
  const d = soDigitos(valor);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) {
    return false;
  }
  const digito = (quantidade: number) => {
    let soma = 0;
    for (let i = 0; i < quantidade; i++) {
      soma += Number(d[i]) * (quantidade + 1 - i);
    }
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10]);
}

export function senhaForte(senha: string): boolean {
  return CRITERIOS_SENHA.every((criterio) => criterio.teste(senha));
}

export function senhaExcedeLimite(senha: string): boolean {
  return new TextEncoder().encode(senha).length > LIMITE_BYTES_SENHA;
}

function hojeIso(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

export function dataPassada(iso: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(iso) && iso < hojeIso();
}

export function emailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
```

`frontend/src/validacao/mascaras.ts`:
```ts
import { soDigitos } from './regras';

export function mascaraCpf(valor: string): string {
  return soDigitos(valor)
    .slice(0, 11)
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
}

export function mascaraTelefone(valor: string): string {
  const d = soDigitos(valor).slice(0, 11);
  if (d.length === 0) return '';
  if (d.length <= 2) return `(${d}`;
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

export function mascaraCep(valor: string): string {
  const d = soDigitos(valor).slice(0, 8);
  return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
}

export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}
```

`frontend/src/validacao/esquemas.ts`:
```ts
import { z } from 'zod';
import type { EnderecoDados, Perfil } from '../api/tipos';
import { mascaraCep, mascaraTelefone } from './mascaras';
import { cpfValido, dataPassada, emailValido, senhaExcedeLimite, senhaForte, soDigitos, UFS } from './regras';

export const OBRIGATORIO = 'Campo obrigatório';
export const MENSAGEM_SENHA_FRACA =
  'A senha deve ter ao menos 8 caracteres, com letra maiúscula, minúscula, número e caractere especial';
export const MENSAGEM_SENHA_LONGA = 'A senha é longa demais';

const maximo = (n: number) => `Máximo de ${n} caracteres`;
const obrigatorio = () => z.string().trim().min(1, OBRIGATORIO);

const telefone = obrigatorio().refine((v) => /^\d{10,11}$/.test(soDigitos(v)), 'Telefone inválido');

export const enderecoSchema = z.object({
  cep: obrigatorio().refine((v) => soDigitos(v).length === 8, 'CEP inválido'),
  logradouro: obrigatorio().max(200, maximo(200)),
  numero: obrigatorio().max(10, maximo(10)),
  complemento: z.string().trim().max(100, maximo(100)),
  bairro: obrigatorio().max(100, maximo(100)),
  cidade: obrigatorio().max(100, maximo(100)),
  uf: obrigatorio().refine((v) => (UFS as readonly string[]).includes(v), 'UF inválida'),
});

export const cadastroSchema = z
  .object({
    nome: obrigatorio().max(150, maximo(150)),
    cpf: obrigatorio().refine(cpfValido, 'CPF inválido'),
    email: obrigatorio().max(254, maximo(254)).refine(emailValido, 'E-mail inválido'),
    dataNascimento: obrigatorio().refine(dataPassada, 'Data de nascimento inválida'),
    senha: z
      .string()
      .min(1, OBRIGATORIO)
      .refine((s) => !senhaExcedeLimite(s), MENSAGEM_SENHA_LONGA)
      .refine(senhaForte, MENSAGEM_SENHA_FRACA),
    confirmacaoSenha: z.string().min(1, OBRIGATORIO),
    telefone,
    endereco: enderecoSchema,
  })
  .refine((d) => d.senha === d.confirmacaoSenha, {
    message: 'As senhas não conferem',
    path: ['confirmacaoSenha'],
    // roda mesmo quando outros campos têm erro, para o aviso aparecer em tempo real
    when(payload) {
      const valor = payload.value as { senha?: unknown; confirmacaoSenha?: unknown };
      return typeof valor?.senha === 'string' && typeof valor?.confirmacaoSenha === 'string';
    },
  });

export const perfilSchema = z.object({ telefone, endereco: enderecoSchema });

export const loginSchema = z.object({
  email: obrigatorio(),
  senha: z.string().min(1, OBRIGATORIO),
});

export type EnderecoForm = z.infer<typeof enderecoSchema>;
export type CadastroForm = z.infer<typeof cadastroSchema>;
export type PerfilForm = z.infer<typeof perfilSchema>;
export type LoginForm = z.infer<typeof loginSchema>;

export const ENDERECO_VAZIO: EnderecoForm = {
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
};

function enderecoParaRequisicao(e: EnderecoForm): EnderecoDados {
  return {
    cep: soDigitos(e.cep),
    logradouro: e.logradouro.trim(),
    numero: e.numero.trim(),
    complemento: e.complemento.trim() || null,
    bairro: e.bairro.trim(),
    cidade: e.cidade.trim(),
    uf: e.uf,
  };
}

export function cadastroParaRequisicao(d: CadastroForm) {
  return {
    nome: d.nome.trim(),
    cpf: soDigitos(d.cpf),
    email: d.email.trim(),
    dataNascimento: d.dataNascimento,
    senha: d.senha,
    telefone: soDigitos(d.telefone),
    endereco: enderecoParaRequisicao(d.endereco),
  };
}

export function perfilParaFormulario(p: Perfil): PerfilForm {
  return {
    telefone: mascaraTelefone(p.telefone),
    endereco: { ...p.endereco, cep: mascaraCep(p.endereco.cep), complemento: p.endereco.complemento ?? '' },
  };
}

export function perfilParaRequisicao(d: PerfilForm) {
  return { telefone: soDigitos(d.telefone), endereco: enderecoParaRequisicao(d.endereco) };
}
```

- [ ] **Passo 3: Rodar e confirmar que passa**

Rodar: `cd frontend && npx vitest run src/validacao && npx tsc --noEmit; cd ..`
Esperado: PASSA, sem erros de tipo. Se o `tsc` rejeitar a opção `when` do refine, confira a assinatura em `node_modules/zod/v4/core/api.d.ts` (parâmetro `when?: (payload) => boolean`). Ajuste a tipagem, sem mudar o comportamento: o teste "acusa senhas diferentes mesmo com outros campos inválidos" tem que continuar passando.

- [ ] **Passo 4: Commit**

```bash
git add frontend/src/validacao
git commit -m "feat(frontend): regras, máscaras e esquemas de validação espelhando o backend"
```

---

### Tarefa 12: Frontend — tela de cadastro (RF01, RF02)

**Arquivos:**
- Criar: `frontend/src/componentes/Campo.tsx`, `CamposEndereco.tsx`, `IndicadorForcaSenha.tsx`
- Criar: `frontend/src/paginas/CadastroPagina.tsx`, `CadastroConcluidoPagina.tsx`
- Criar: `frontend/src/test/renderizar.tsx`
- Teste: `frontend/src/componentes/IndicadorForcaSenha.test.tsx`, `frontend/src/paginas/CadastroPagina.test.tsx`

**Interfaces:**
- Consome: `requisitar`, `ApiError`, `CadastroResposta` (Tarefa 10); `cadastroSchema`, `CadastroForm`, `ENDERECO_VAZIO`, `EnderecoForm`, `cadastroParaRequisicao`, `CRITERIOS_SENHA`, `UFS`, `mascaraCpf`, `mascaraTelefone`, `mascaraCep` (Tarefa 11).
- Produz:
  - `Campo` com as props `{ rotulo, registro: UseFormRegisterReturn, erro?, tipo?, mascara?, autoComplete?, inputMode? }`.
  - `CamposEndereco` com as props `{ registrar: (campo: keyof EnderecoForm) => UseFormRegisterReturn, erros?: Partial<Record<keyof EnderecoForm, { message?: string }>> }`.
  - `IndicadorForcaSenha({ senha })`.
  - `CadastroPagina` e `CadastroConcluidoPagina`, que lê `location.state.email`.
  - `renderizar(rotaInicial, rotas, { strict? })`.
  - Rótulos usados nos testes: "Nome completo", "CPF", "E-mail", "Data de nascimento", "Senha", "Confirmação de senha", "Telefone", "CEP", "Logradouro", "Número", "Complemento (opcional)", "Bairro", "Cidade", "UF".

- [ ] **Passo 1: Escrever os testes que falham**

`frontend/src/test/renderizar.tsx`:
```tsx
import { render } from '@testing-library/react';
import { StrictMode, type ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router';

export interface RotaTeste {
  caminho: string;
  elemento: ReactNode;
}

export function renderizar(rotaInicial: string, rotas: RotaTeste[], opcoes: { strict?: boolean } = {}) {
  const arvore = (
    <MemoryRouter initialEntries={[rotaInicial]}>
      <Routes>
        {rotas.map((r) => (
          <Route key={r.caminho} path={r.caminho} element={r.elemento} />
        ))}
      </Routes>
    </MemoryRouter>
  );
  return render(opcoes.strict ? <StrictMode>{arvore}</StrictMode> : arvore);
}
```

`frontend/src/componentes/IndicadorForcaSenha.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { IndicadorForcaSenha } from './IndicadorForcaSenha';

describe('IndicadorForcaSenha', () => {
  it('marca apenas os critérios atendidos', () => {
    render(<IndicadorForcaSenha senha="abc1" />);

    expect(screen.getByText(/Letra minúscula/)).toHaveAttribute('data-ok', 'true');
    expect(screen.getByText(/Número/)).toHaveAttribute('data-ok', 'true');
    expect(screen.getByText(/Letra maiúscula/)).toHaveAttribute('data-ok', 'false');
    expect(screen.getByText(/Caractere especial/)).toHaveAttribute('data-ok', 'false');
    expect(screen.getByText(/Mínimo de 8 caracteres/)).toHaveAttribute('data-ok', 'false');
  });
});
```

`frontend/src/paginas/CadastroPagina.test.tsx`:
```tsx
import { screen } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { CadastroConcluidoPagina } from './CadastroConcluidoPagina';
import { CadastroPagina } from './CadastroPagina';

function renderizarCadastro() {
  return renderizar('/cadastro', [
    { caminho: '/cadastro', elemento: <CadastroPagina /> },
    { caminho: '/cadastro/concluido', elemento: <CadastroConcluidoPagina /> },
  ]);
}

async function preencherFormularioValido(user: UserEvent) {
  await user.type(screen.getByLabelText('Nome completo'), 'Maria da Silva');
  await user.type(screen.getByLabelText('CPF'), '52998224725');
  await user.type(screen.getByLabelText('E-mail'), 'Maria@Teste.local');
  await user.type(screen.getByLabelText('Data de nascimento'), '1990-05-20');
  await user.type(screen.getByLabelText('Senha'), 'Senha@123');
  await user.type(screen.getByLabelText('Confirmação de senha'), 'Senha@123');
  await user.type(screen.getByLabelText('Telefone'), '11987654321');
  await user.type(screen.getByLabelText('CEP'), '01310100');
  await user.type(screen.getByLabelText('Logradouro'), 'Avenida Paulista');
  await user.type(screen.getByLabelText('Número'), '1000');
  await user.type(screen.getByLabelText('Bairro'), 'Bela Vista');
  await user.type(screen.getByLabelText('Cidade'), 'São Paulo');
  await user.selectOptions(screen.getByLabelText('UF'), 'SP');
}

describe('CadastroPagina', () => {
  it('mostra os obrigatórios ao enviar vazio, sem chamar a API', async () => {
    const user = userEvent.setup();
    renderizarCadastro();

    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findAllByText('Campo obrigatório')).toHaveLength(13);
  });

  it('aplica máscara e valida o CPF ao sair do campo', async () => {
    const user = userEvent.setup();
    renderizarCadastro();
    const cpf = screen.getByLabelText('CPF');

    await user.type(cpf, '52998224724');
    await user.tab();

    expect(cpf).toHaveValue('529.982.247-24');
    expect(await screen.findByText('CPF inválido')).toBeInTheDocument();
    expect(cpf).toHaveAttribute('aria-invalid', 'true');
  });

  it('avisa quando as senhas não conferem', async () => {
    const user = userEvent.setup();
    renderizarCadastro();

    await user.type(screen.getByLabelText('Senha'), 'Senha@123');
    await user.type(screen.getByLabelText('Confirmação de senha'), 'Senha@124');
    await user.tab();

    expect(await screen.findByText('As senhas não conferem')).toBeInTheDocument();
  });

  it('envia os dados normalizados e mostra a confirmação', async () => {
    let corpo: Record<string, unknown> | undefined;
    servidor.use(
      http.post('/api/usuarios', async ({ request }) => {
        corpo = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          { id: 'abc', email: 'maria@teste.local', status: 'PENDENTE_ATIVACAO' },
          { status: 201 },
        );
      }),
    );
    const user = userEvent.setup();
    renderizarCadastro();

    await preencherFormularioValido(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('heading', { name: 'Verifique seu e-mail' })).toBeInTheDocument();
    expect(screen.getByText('maria@teste.local')).toBeInTheDocument();
    expect(corpo).toEqual({
      nome: 'Maria da Silva',
      cpf: '52998224725',
      email: 'Maria@Teste.local',
      dataNascimento: '1990-05-20',
      senha: 'Senha@123',
      telefone: '11987654321',
      endereco: {
        cep: '01310100',
        logradouro: 'Avenida Paulista',
        numero: '1000',
        complemento: null,
        bairro: 'Bela Vista',
        cidade: 'São Paulo',
        uf: 'SP',
      },
    });
  });

  it('marca o campo e-mail quando a API responde 409', async () => {
    servidor.use(
      http.post('/api/usuarios', () =>
        HttpResponse.json(
          {
            detail: 'E-mail já cadastrado',
            codigo: 'EMAIL_JA_CADASTRADO',
            erros: [{ campo: 'email', mensagem: 'E-mail já cadastrado' }],
          },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = userEvent.setup();
    renderizarCadastro();

    await preencherFormularioValido(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('E-mail já cadastrado')).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true');
  });

  it('mostra erro geral quando a API falha sem erros de campo', async () => {
    servidor.use(http.post('/api/usuarios', () => new HttpResponse(null, { status: 500 })));
    const user = userEvent.setup();
    renderizarCadastro();

    await preencherFormularioValido(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('Erro inesperado')).toBeInTheDocument();
  });
});
```

Rodar: `cd frontend && npx vitest run src/componentes src/paginas; cd ..`
Esperado: FALHA; os componentes não existem.

- [ ] **Passo 2: Implementar os componentes**

`frontend/src/componentes/Campo.tsx`:
```tsx
import { useId, type HTMLAttributes } from 'react';
import type { UseFormRegisterReturn } from 'react-hook-form';

interface CampoProps {
  rotulo: string;
  registro: UseFormRegisterReturn;
  erro?: string;
  tipo?: string;
  mascara?: (valor: string) => string;
  autoComplete?: string;
  inputMode?: HTMLAttributes<HTMLInputElement>['inputMode'];
}

export function Campo({ rotulo, registro, erro, tipo = 'text', mascara, autoComplete, inputMode }: CampoProps) {
  const id = useId();
  const idErro = `${id}-erro`;
  return (
    <div className="campo">
      <label htmlFor={id}>{rotulo}</label>
      <input
        id={id}
        type={tipo}
        autoComplete={autoComplete}
        inputMode={inputMode}
        aria-invalid={erro ? true : undefined}
        aria-describedby={erro ? idErro : undefined}
        {...registro}
        onChange={(evento) => {
          if (mascara) {
            evento.target.value = mascara(evento.target.value);
          }
          return registro.onChange(evento);
        }}
      />
      {erro && (
        <span id={idErro} className="erro-campo" role="alert">
          {erro}
        </span>
      )}
    </div>
  );
}
```

`frontend/src/componentes/CamposEndereco.tsx`:
```tsx
import { useId } from 'react';
import type { UseFormRegisterReturn } from 'react-hook-form';
import type { EnderecoForm } from '../validacao/esquemas';
import { mascaraCep } from '../validacao/mascaras';
import { UFS } from '../validacao/regras';
import { Campo } from './Campo';

interface CamposEnderecoProps {
  registrar: (campo: keyof EnderecoForm) => UseFormRegisterReturn;
  erros?: Partial<Record<keyof EnderecoForm, { message?: string }>>;
}

export function CamposEndereco({ registrar, erros }: CamposEnderecoProps) {
  const idUf = useId();
  const erroUf = erros?.uf?.message;
  return (
    <fieldset>
      <legend>Endereço</legend>
      <Campo rotulo="CEP" registro={registrar('cep')} erro={erros?.cep?.message} mascara={mascaraCep}
        inputMode="numeric" autoComplete="postal-code" />
      <Campo rotulo="Logradouro" registro={registrar('logradouro')} erro={erros?.logradouro?.message}
        autoComplete="address-line1" />
      <Campo rotulo="Número" registro={registrar('numero')} erro={erros?.numero?.message} />
      <Campo rotulo="Complemento (opcional)" registro={registrar('complemento')}
        erro={erros?.complemento?.message} autoComplete="address-line2" />
      <Campo rotulo="Bairro" registro={registrar('bairro')} erro={erros?.bairro?.message} />
      <Campo rotulo="Cidade" registro={registrar('cidade')} erro={erros?.cidade?.message}
        autoComplete="address-level2" />
      <div className="campo">
        <label htmlFor={idUf}>UF</label>
        <select id={idUf} aria-invalid={erroUf ? true : undefined} {...registrar('uf')}>
          <option value="">Selecione</option>
          {UFS.map((uf) => (
            <option key={uf} value={uf}>
              {uf}
            </option>
          ))}
        </select>
        {erroUf && (
          <span className="erro-campo" role="alert">
            {erroUf}
          </span>
        )}
      </div>
    </fieldset>
  );
}
```

`frontend/src/componentes/IndicadorForcaSenha.tsx`:
```tsx
import { CRITERIOS_SENHA } from '../validacao/regras';

export function IndicadorForcaSenha({ senha }: { senha: string }) {
  return (
    <ul className="forca-senha" aria-label="Requisitos da senha">
      {CRITERIOS_SENHA.map((criterio) => {
        const atendido = criterio.teste(senha ?? '');
        return (
          <li key={criterio.rotulo} data-ok={atendido} className={atendido ? 'ok' : undefined}>
            {atendido ? '✓' : '•'} {criterio.rotulo}
          </li>
        );
      })}
    </ul>
  );
}
```

- [ ] **Passo 3: Implementar as páginas**

`frontend/src/paginas/CadastroPagina.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';
import type { CadastroResposta } from '../api/tipos';
import { Campo } from '../componentes/Campo';
import { CamposEndereco } from '../componentes/CamposEndereco';
import { IndicadorForcaSenha } from '../componentes/IndicadorForcaSenha';
import { cadastroParaRequisicao, cadastroSchema, ENDERECO_VAZIO, type CadastroForm } from '../validacao/esquemas';
import { mascaraCpf, mascaraTelefone } from '../validacao/mascaras';

const VALORES_INICIAIS: CadastroForm = {
  nome: '',
  cpf: '',
  email: '',
  dataNascimento: '',
  senha: '',
  confirmacaoSenha: '',
  telefone: '',
  endereco: ENDERECO_VAZIO,
};

export function CadastroPagina() {
  const navigate = useNavigate();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CadastroForm>({
    resolver: zodResolver(cadastroSchema),
    mode: 'onTouched',
    defaultValues: VALORES_INICIAIS,
  });

  async function enviar(dados: CadastroForm) {
    setErroGeral(null);
    try {
      const resposta = await requisitar<CadastroResposta>('POST', '/api/usuarios', cadastroParaRequisicao(dados));
      navigate('/cadastro/concluido', { state: { email: resposta.email } });
    } catch (erro) {
      if (erro instanceof ApiError && erro.erros.length > 0) {
        for (const { campo, mensagem } of erro.erros) {
          setError(campo as Path<CadastroForm>, { type: 'server', message: mensagem });
        }
        return;
      }
      setErroGeral(erro instanceof ApiError ? erro.message : 'Não foi possível concluir o cadastro. Tente novamente.');
    }
  }

  return (
    <section className="cartao">
      <h1>Criar conta</h1>
      {erroGeral && (
        <div className="alerta alerta-erro" role="alert">
          {erroGeral}
        </div>
      )}
      <form onSubmit={handleSubmit(enviar)} noValidate>
        <Campo rotulo="Nome completo" registro={register('nome')} erro={errors.nome?.message} autoComplete="name" />
        <Campo rotulo="CPF" registro={register('cpf')} erro={errors.cpf?.message} mascara={mascaraCpf}
          inputMode="numeric" />
        <Campo rotulo="E-mail" tipo="email" registro={register('email')} erro={errors.email?.message}
          autoComplete="email" />
        <Campo rotulo="Data de nascimento" tipo="date" registro={register('dataNascimento')}
          erro={errors.dataNascimento?.message} autoComplete="bday" />
        <Campo rotulo="Senha" tipo="password" registro={register('senha')} erro={errors.senha?.message}
          autoComplete="new-password" />
        <IndicadorForcaSenha senha={watch('senha')} />
        <Campo rotulo="Confirmação de senha" tipo="password" registro={register('confirmacaoSenha')}
          erro={errors.confirmacaoSenha?.message} autoComplete="new-password" />
        <Campo rotulo="Telefone" tipo="tel" registro={register('telefone')} erro={errors.telefone?.message}
          mascara={mascaraTelefone} autoComplete="tel" />
        <CamposEndereco registrar={(campo) => register(`endereco.${campo}`)} erros={errors.endereco} />
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Enviando…' : 'Cadastrar'}
        </button>
      </form>
      <p>
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </section>
  );
}
```

`frontend/src/paginas/CadastroConcluidoPagina.tsx`:
```tsx
import { Link, useLocation } from 'react-router';

export function CadastroConcluidoPagina() {
  const { state } = useLocation();
  const email = (state as { email?: string } | null)?.email;
  return (
    <section className="cartao">
      <h1>Verifique seu e-mail</h1>
      <p>
        {email ? (
          <>
            Enviamos um link de ativação para <strong>{email}</strong>.
          </>
        ) : (
          'Enviamos um link de ativação para o e-mail cadastrado.'
        )}
      </p>
      <p>O link é válido por 24 horas.</p>
      <Link to="/login">Ir para o login</Link>
    </section>
  );
}
```

- [ ] **Passo 4: Rodar e confirmar que passa**

Rodar: `cd frontend && npx vitest run src/componentes src/paginas && npx tsc --noEmit; cd ..`
Esperado: PASSA, sem erros de tipo.

- [ ] **Passo 5: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): tela de cadastro com máscaras e validação em tempo real"
```

---

### Tarefa 13: Frontend — telas de ativação e login (RF05, RF06, RN04)

**Arquivos:**
- Criar: `frontend/src/paginas/AtivacaoPagina.tsx`, `frontend/src/paginas/LoginPagina.tsx`
- Teste: `frontend/src/paginas/AtivacaoPagina.test.tsx`, `frontend/src/paginas/LoginPagina.test.tsx`

**Interfaces:**
- Consome: `requisitar`, `ApiError`, `UsuarioLogado` (Tarefa 10); `loginSchema`, `LoginForm` (Tarefa 11); `Campo` e `renderizar` (Tarefa 12).
- Produz: `AtivacaoPagina`, que lê `?token=` e chama a API **uma única vez**, e `LoginPagina`, que leva a `/perfil` em caso de sucesso.

- [ ] **Passo 1: Escrever os testes que falham**

`frontend/src/paginas/AtivacaoPagina.test.tsx`:
```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { AtivacaoPagina } from './AtivacaoPagina';

function erroToken(codigo: string) {
  return HttpResponse.json(
    { detail: 'erro', codigo },
    { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

describe('AtivacaoPagina', () => {
  it('ativa a conta chamando a API uma única vez, mesmo em StrictMode', async () => {
    let chamadas = 0;
    let tokenRecebido: unknown;
    servidor.use(
      http.post('/api/ativacao', async ({ request }) => {
        chamadas += 1;
        tokenRecebido = ((await request.json()) as { token: string }).token;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderizar('/ativar?token=abc123', [{ caminho: '/ativar', elemento: <AtivacaoPagina /> }], { strict: true });

    expect(await screen.findByText('Conta ativada com sucesso!')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/login');
    expect(chamadas).toBe(1);
    expect(tokenRecebido).toBe('abc123');
  });

  it.each([
    ['TOKEN_EXPIRADO', 'Este link de ativação expirou.'],
    ['TOKEN_JA_UTILIZADO', 'Este link já foi utilizado. Se você já ativou sua conta, faça login.'],
    ['TOKEN_INVALIDO', 'Link de ativação inválido.'],
  ])('mostra a mensagem específica para %s', async (codigo, mensagem) => {
    servidor.use(http.post('/api/ativacao', () => erroToken(codigo)));

    renderizar('/ativar?token=abc', [{ caminho: '/ativar', elemento: <AtivacaoPagina /> }]);

    expect(await screen.findByText(mensagem)).toBeInTheDocument();
  });

  it('sem token na URL mostra link inválido sem chamar a API', async () => {
    renderizar('/ativar', [{ caminho: '/ativar', elemento: <AtivacaoPagina /> }]);

    expect(await screen.findByText('Link de ativação inválido.')).toBeInTheDocument();
  });
});
```

`frontend/src/paginas/LoginPagina.test.tsx`:
```tsx
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { LoginPagina } from './LoginPagina';

function renderizarLogin() {
  return renderizar('/login', [
    { caminho: '/login', elemento: <LoginPagina /> },
    { caminho: '/perfil', elemento: <p>Página de perfil</p> },
  ]);
}

function problema(status: number, codigo: string) {
  return HttpResponse.json(
    { detail: 'erro', codigo },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

async function entrar() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('E-mail'), 'maria@teste.local');
  await user.type(screen.getByLabelText('Senha'), 'Senha@123');
  await user.click(screen.getByRole('button', { name: 'Entrar' }));
}

describe('LoginPagina', () => {
  it('envia as credenciais e vai para o perfil', async () => {
    let corpo: unknown;
    servidor.use(
      http.post('/api/auth/login', async ({ request }) => {
        corpo = await request.json();
        return HttpResponse.json({ nome: 'Maria da Silva', email: 'maria@teste.local' });
      }),
    );
    renderizarLogin();

    await entrar();

    expect(await screen.findByText('Página de perfil')).toBeInTheDocument();
    expect(corpo).toEqual({ email: 'maria@teste.local', senha: 'Senha@123' });
  });

  it('mostra mensagem genérica para credenciais inválidas', async () => {
    servidor.use(http.post('/api/auth/login', () => problema(401, 'CREDENCIAIS_INVALIDAS')));
    renderizarLogin();

    await entrar();

    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument();
  });

  it('avisa que a conta está pendente de ativação', async () => {
    servidor.use(http.post('/api/auth/login', () => problema(403, 'CONTA_PENDENTE')));
    renderizarLogin();

    await entrar();

    expect(await screen.findByText('Sua conta ainda não foi ativada. Verifique seu e-mail.')).toBeInTheDocument();
  });

  it('exige e-mail e senha', async () => {
    const user = userEvent.setup();
    renderizarLogin();

    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findAllByText('Campo obrigatório')).toHaveLength(2);
  });
});
```

Rodar: `cd frontend && npx vitest run src/paginas/AtivacaoPagina.test.tsx src/paginas/LoginPagina.test.tsx; cd ..`
Esperado: FALHA; as páginas não existem.

- [ ] **Passo 2: Implementar**

`frontend/src/paginas/AtivacaoPagina.tsx`:
```tsx
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';

const MENSAGENS: Record<string, string> = {
  TOKEN_EXPIRADO: 'Este link de ativação expirou.',
  TOKEN_JA_UTILIZADO: 'Este link já foi utilizado. Se você já ativou sua conta, faça login.',
  TOKEN_INVALIDO: 'Link de ativação inválido.',
};

type Estado = { tipo: 'carregando' } | { tipo: 'sucesso' } | { tipo: 'erro'; mensagem: string };

export function AtivacaoPagina() {
  const [parametros] = useSearchParams();
  const token = parametros.get('token');
  const [estado, setEstado] = useState<Estado>(
    token ? { tipo: 'carregando' } : { tipo: 'erro', mensagem: MENSAGENS.TOKEN_INVALIDO },
  );
  // O token é de uso único: o StrictMode (e remontagens) não podem disparar uma segunda chamada.
  const enviado = useRef(false);

  useEffect(() => {
    if (!token || enviado.current) {
      return;
    }
    enviado.current = true;
    requisitar<void>('POST', '/api/ativacao', { token })
      .then(() => setEstado({ tipo: 'sucesso' }))
      .catch((erro: unknown) => {
        const mensagem =
          (erro instanceof ApiError && erro.codigo && MENSAGENS[erro.codigo]) ||
          'Não foi possível ativar a conta. Tente novamente mais tarde.';
        setEstado({ tipo: 'erro', mensagem });
      });
  }, [token]);

  return (
    <section className="cartao">
      <h1>Ativação de conta</h1>
      {estado.tipo === 'carregando' && <p>Ativando sua conta…</p>}
      {estado.tipo === 'sucesso' && <div className="alerta alerta-sucesso">Conta ativada com sucesso!</div>}
      {estado.tipo === 'erro' && (
        <div className="alerta alerta-erro" role="alert">
          {estado.mensagem}
        </div>
      )}
      {estado.tipo !== 'carregando' && <Link to="/login">Ir para o login</Link>}
    </section>
  );
}
```

`frontend/src/paginas/LoginPagina.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';
import type { UsuarioLogado } from '../api/tipos';
import { Campo } from '../componentes/Campo';
import { loginSchema, type LoginForm } from '../validacao/esquemas';

export function LoginPagina() {
  const navigate = useNavigate();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: { email: '', senha: '' },
  });

  async function entrar(dados: LoginForm) {
    setErroGeral(null);
    try {
      await requisitar<UsuarioLogado>('POST', '/api/auth/login', { email: dados.email.trim(), senha: dados.senha });
      navigate('/perfil');
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 401) {
        setErroGeral('E-mail ou senha inválidos.');
      } else if (erro instanceof ApiError && erro.status === 403) {
        setErroGeral('Sua conta ainda não foi ativada. Verifique seu e-mail.');
      } else if (erro instanceof ApiError && erro.erros.length > 0) {
        for (const { campo, mensagem } of erro.erros) {
          setError(campo as Path<LoginForm>, { type: 'server', message: mensagem });
        }
      } else {
        setErroGeral('Não foi possível entrar. Tente novamente.');
      }
    }
  }

  return (
    <section className="cartao">
      <h1>Entrar</h1>
      {erroGeral && (
        <div className="alerta alerta-erro" role="alert">
          {erroGeral}
        </div>
      )}
      <form onSubmit={handleSubmit(entrar)} noValidate>
        <Campo rotulo="E-mail" tipo="email" registro={register('email')} erro={errors.email?.message}
          autoComplete="email" />
        <Campo rotulo="Senha" tipo="password" registro={register('senha')} erro={errors.senha?.message}
          autoComplete="current-password" />
        <button type="submit" disabled={isSubmitting}>
          Entrar
        </button>
      </form>
      <p>
        Ainda não tem conta? <Link to="/cadastro">Criar conta</Link>
      </p>
    </section>
  );
}
```

- [ ] **Passo 3: Rodar e confirmar que passa**

Rodar: `cd frontend && npx vitest run src/paginas && npx tsc --noEmit; cd ..`
Esperado: PASSA.

- [ ] **Passo 4: Commit**

```bash
git add frontend/src/paginas
git commit -m "feat(frontend): telas de ativação (chamada única) e login"
```

---

### Tarefa 14: Frontend — perfil, início e rotas da aplicação (RF07, RN01)

**Arquivos:**
- Criar: `frontend/src/paginas/PerfilPagina.tsx`, `frontend/src/paginas/InicioPagina.tsx`
- Modificar: `frontend/src/App.tsx` (substituir o conteúdo temporário)
- Teste: `frontend/src/paginas/PerfilPagina.test.tsx`, `frontend/src/paginas/InicioPagina.test.tsx`, `frontend/src/App.test.tsx`

**Interfaces:**
- Consome: `requisitar`, `ApiError`, `Perfil` (Tarefa 10); `perfilSchema`, `PerfilForm`, `perfilParaFormulario`, `perfilParaRequisicao`, `mascaraCpf`, `mascaraTelefone`, `mascaraCep`, `formatarData` (Tarefa 11); `Campo`, `CamposEndereco` e `renderizar` (Tarefa 12); as páginas das Tarefas 12 e 13.
- Produz: `App` com as rotas `/`, `/cadastro`, `/cadastro/concluido`, `/ativar`, `/login`, `/perfil` e `*` (que redireciona para `/`).

- [ ] **Passo 1: Escrever os testes que falham**

`frontend/src/paginas/PerfilPagina.test.tsx`:
```tsx
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import type { Perfil } from '../api/tipos';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { PerfilPagina } from './PerfilPagina';

const perfil: Perfil = {
  nome: 'Maria da Silva',
  cpf: '52998224725',
  email: 'maria@teste.local',
  dataNascimento: '1990-05-20',
  telefone: '11987654321',
  endereco: {
    cep: '01310100',
    logradouro: 'Avenida Paulista',
    numero: '1000',
    complemento: 'Apto 12',
    bairro: 'Bela Vista',
    cidade: 'São Paulo',
    uf: 'SP',
  },
  status: 'ATIVO',
};

function renderizarPerfil() {
  return renderizar('/perfil', [
    { caminho: '/perfil', elemento: <PerfilPagina /> },
    { caminho: '/login', elemento: <p>Página de login</p> },
  ]);
}

describe('PerfilPagina', () => {
  it('mostra os dados com os imutáveis apenas como texto', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json(perfil)));
    renderizarPerfil();

    expect(await screen.findByText('Maria da Silva')).toBeInTheDocument();
    expect(screen.getByText('529.982.247-25')).toBeInTheDocument();
    expect(screen.getByText('maria@teste.local')).toBeInTheDocument();
    expect(screen.getByText('20/05/1990')).toBeInTheDocument();
    expect(screen.getByText('(11) 98765-4321')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });

  it('redireciona para o login sem sessão', async () => {
    servidor.use(
      http.get('/api/perfil', () =>
        HttpResponse.json({ codigo: 'NAO_AUTENTICADO' }, { status: 401 }),
      ),
    );
    renderizarPerfil();

    expect(await screen.findByText('Página de login')).toBeInTheDocument();
  });

  it('edita telefone e endereço enviando só os campos editáveis', async () => {
    let corpo: Record<string, unknown> | undefined;
    servidor.use(
      http.get('/api/perfil', () => HttpResponse.json(perfil)),
      http.put('/api/perfil', async ({ request }) => {
        corpo = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...perfil, telefone: '1133334444' });
      }),
    );
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Editar contato' }));
    expect(screen.queryByLabelText('Nome completo')).not.toBeInTheDocument();
    const telefone = screen.getByLabelText('Telefone');
    await user.clear(telefone);
    await user.type(telefone, '1133334444');
    await user.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Dados atualizados com sucesso.')).toBeInTheDocument();
    expect(screen.getByText('(11) 3333-4444')).toBeInTheDocument();
    expect(corpo).toEqual({ telefone: '1133334444', endereco: perfil.endereco });
  });

  it('mostra erro de validação vindo da API no campo', async () => {
    servidor.use(
      http.get('/api/perfil', () => HttpResponse.json(perfil)),
      http.put('/api/perfil', () =>
        HttpResponse.json(
          { detail: 'Dados inválidos', codigo: 'VALIDACAO', erros: [{ campo: 'endereco.cidade', mensagem: 'Máximo de 100 caracteres' }] },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Editar contato' }));
    await user.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Máximo de 100 caracteres')).toBeInTheDocument();
  });

  it('cancelar volta para a visualização sem chamar a API', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json(perfil)));
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Editar contato' }));
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.getByRole('button', { name: 'Editar contato' })).toBeInTheDocument();
  });

  it('sair encerra a sessão e vai para o login', async () => {
    let saiu = false;
    servidor.use(
      http.get('/api/perfil', () => HttpResponse.json(perfil)),
      http.post('/api/auth/logout', () => {
        saiu = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderizarPerfil();

    await user.click(await screen.findByRole('button', { name: 'Sair' }));

    expect(await screen.findByText('Página de login')).toBeInTheDocument();
    expect(saiu).toBe(true);
  });
});
```

`frontend/src/paginas/InicioPagina.test.tsx`:
```tsx
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderizar } from '../test/renderizar';
import { servidor } from '../test/servidor';
import { InicioPagina } from './InicioPagina';

const rotas = [
  { caminho: '/', elemento: <InicioPagina /> },
  { caminho: '/perfil', elemento: <p>Página de perfil</p> },
  { caminho: '/login', elemento: <p>Página de login</p> },
];

describe('InicioPagina', () => {
  it('vai para o perfil quando há sessão', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json({})));
    renderizar('/', rotas);
    expect(await screen.findByText('Página de perfil')).toBeInTheDocument();
  });

  it('vai para o login sem sessão', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json({}, { status: 401 })));
    renderizar('/', rotas);
    expect(await screen.findByText('Página de login')).toBeInTheDocument();
  });
});
```

`frontend/src/App.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { servidor } from './test/servidor';

describe('App', () => {
  it('redireciona rota desconhecida para o início e daí para o login sem sessão', async () => {
    servidor.use(http.get('/api/perfil', () => HttpResponse.json({}, { status: 401 })));

    render(
      <MemoryRouter initialEntries={['/rota-inexistente']}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('abre a tela de cadastro', () => {
    render(
      <MemoryRouter initialEntries={['/cadastro']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Criar conta' })).toBeInTheDocument();
  });
});
```

Rodar: `cd frontend && npx vitest run src/paginas/PerfilPagina.test.tsx src/paginas/InicioPagina.test.tsx src/App.test.tsx; cd ..`
Esperado: FALHA; `PerfilPagina` e `InicioPagina` não existem, e o `App` temporário não tem rotas.

- [ ] **Passo 2: Implementar**

`frontend/src/paginas/InicioPagina.tsx`:
```tsx
import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { requisitar } from '../api/cliente';

export function InicioPagina() {
  const navigate = useNavigate();

  useEffect(() => {
    let ativo = true;
    requisitar('GET', '/api/perfil')
      .then(() => ativo && navigate('/perfil', { replace: true }))
      .catch(() => ativo && navigate('/login', { replace: true }));
    return () => {
      ativo = false;
    };
  }, [navigate]);

  return <p>Carregando…</p>;
}
```

`frontend/src/paginas/PerfilPagina.tsx`:
```tsx
import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useState } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';
import type { Perfil } from '../api/tipos';
import { Campo } from '../componentes/Campo';
import { CamposEndereco } from '../componentes/CamposEndereco';
import {
  perfilParaFormulario,
  perfilParaRequisicao,
  perfilSchema,
  type PerfilForm,
} from '../validacao/esquemas';
import { formatarData, mascaraCep, mascaraCpf, mascaraTelefone } from '../validacao/mascaras';

function formatarEndereco({ endereco: e }: Perfil): string {
  const complemento = e.complemento ? ` - ${e.complemento}` : '';
  return `${e.logradouro}, ${e.numero}${complemento} — ${e.bairro}, ${e.cidade}/${e.uf} — CEP ${mascaraCep(e.cep)}`;
}

interface FormularioContatoProps {
  perfil: Perfil;
  aoSalvar: (perfil: Perfil) => void;
  aoCancelar: () => void;
}

function FormularioContato({ perfil, aoSalvar, aoCancelar }: FormularioContatoProps) {
  const navigate = useNavigate();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PerfilForm>({
    resolver: zodResolver(perfilSchema),
    mode: 'onTouched',
    defaultValues: perfilParaFormulario(perfil),
  });

  async function salvar(dados: PerfilForm) {
    setErroGeral(null);
    try {
      aoSalvar(await requisitar<Perfil>('PUT', '/api/perfil', perfilParaRequisicao(dados)));
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 401) {
        navigate('/login', { replace: true });
      } else if (erro instanceof ApiError && erro.erros.length > 0) {
        for (const { campo, mensagem } of erro.erros) {
          setError(campo as Path<PerfilForm>, { type: 'server', message: mensagem });
        }
      } else {
        setErroGeral('Não foi possível salvar as alterações. Tente novamente.');
      }
    }
  }

  return (
    <form onSubmit={handleSubmit(salvar)} noValidate>
      {erroGeral && (
        <div className="alerta alerta-erro" role="alert">
          {erroGeral}
        </div>
      )}
      <Campo rotulo="Telefone" tipo="tel" registro={register('telefone')} erro={errors.telefone?.message}
        mascara={mascaraTelefone} autoComplete="tel" />
      <CamposEndereco registrar={(campo) => register(`endereco.${campo}`)} erros={errors.endereco} />
      <div className="acoes">
        <button type="submit" disabled={isSubmitting}>
          Salvar
        </button>
        <button type="button" className="secundario" onClick={aoCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

export function PerfilPagina() {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState<Perfil | null>(null);
  const [editando, setEditando] = useState(false);
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    requisitar<Perfil>('GET', '/api/perfil')
      .then((dados) => ativo && setPerfil(dados))
      .catch((e: unknown) => {
        if (!ativo) return;
        if (e instanceof ApiError && e.status === 401) {
          navigate('/login', { replace: true });
        } else {
          setErro('Não foi possível carregar o perfil.');
        }
      });
    return () => {
      ativo = false;
    };
  }, [navigate]);

  async function sair() {
    try {
      await requisitar<void>('POST', '/api/auth/logout');
    } finally {
      navigate('/login', { replace: true });
    }
  }

  if (erro) {
    return (
      <div className="alerta alerta-erro" role="alert">
        {erro}
      </div>
    );
  }
  if (!perfil) {
    return <p>Carregando…</p>;
  }

  return (
    <section className="cartao">
      <h1>Meu perfil</h1>
      {mensagem && <div className="alerta alerta-sucesso">{mensagem}</div>}
      <dl className="dados">
        <dt>Nome</dt>
        <dd>{perfil.nome}</dd>
        <dt>CPF</dt>
        <dd>{mascaraCpf(perfil.cpf)}</dd>
        <dt>E-mail</dt>
        <dd>{perfil.email}</dd>
        <dt>Nascimento</dt>
        <dd>{formatarData(perfil.dataNascimento)}</dd>
        {!editando && (
          <>
            <dt>Telefone</dt>
            <dd>{mascaraTelefone(perfil.telefone)}</dd>
            <dt>Endereço</dt>
            <dd>{formatarEndereco(perfil)}</dd>
          </>
        )}
      </dl>
      {editando ? (
        <FormularioContato
          perfil={perfil}
          aoSalvar={(atualizado) => {
            setPerfil(atualizado);
            setEditando(false);
            setMensagem('Dados atualizados com sucesso.');
          }}
          aoCancelar={() => setEditando(false)}
        />
      ) : (
        <div className="acoes">
          <button
            type="button"
            onClick={() => {
              setMensagem(null);
              setEditando(true);
            }}
          >
            Editar contato
          </button>
          <button type="button" className="secundario" onClick={sair}>
            Sair
          </button>
        </div>
      )}
    </section>
  );
}
```

`frontend/src/App.tsx` (substitui o conteúdo temporário):
```tsx
import { Navigate, Route, Routes } from 'react-router';
import { AtivacaoPagina } from './paginas/AtivacaoPagina';
import { CadastroConcluidoPagina } from './paginas/CadastroConcluidoPagina';
import { CadastroPagina } from './paginas/CadastroPagina';
import { InicioPagina } from './paginas/InicioPagina';
import { LoginPagina } from './paginas/LoginPagina';
import { PerfilPagina } from './paginas/PerfilPagina';

export function App() {
  return (
    <div className="layout">
      <header>
        <h1>Cadastro de Usuários</h1>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<InicioPagina />} />
          <Route path="/cadastro" element={<CadastroPagina />} />
          <Route path="/cadastro/concluido" element={<CadastroConcluidoPagina />} />
          <Route path="/ativar" element={<AtivacaoPagina />} />
          <Route path="/login" element={<LoginPagina />} />
          <Route path="/perfil" element={<PerfilPagina />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
```

- [ ] **Passo 3: Rodar a suíte inteira do frontend**

Rodar: `cd frontend && npm test && npm run build; cd ..`
Esperado: todos os testes passam e o build termina sem erros de tipo.

- [ ] **Passo 4: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): perfil com edição de contato, início e rotas da aplicação"
```

---

### Tarefa 15: README, verificação completa e roteiro manual

**Arquivos:**
- Criar: `README.md`

**Interfaces:**
- Consome: tudo das tarefas anteriores.
- Produz: a documentação de execução e a evidência de que `mvn clean verify` passa e o JAR sobe.

- [ ] **Passo 1: Escrever o README**

`README.md`:
````markdown
# Cadastro de Usuários

Monolito modular (Spring Boot + React + PostgreSQL) que implementa o esboço em
[docs/sistema_cadastro_esboco.md](docs/sistema_cadastro_esboco.md), seguindo a spec
[docs/superpowers/specs/2026-09-30-cadastro-usuarios-design.md](docs/superpowers/specs/2026-09-30-cadastro-usuarios-design.md).

## Módulos

| Módulo | Conteúdo |
|---|---|
| `database/` | `docker-compose.yml` do PostgreSQL e migrations Flyway |
| `frontend/` | SPA React + Vite (empacotada no JAR) |
| `backend/` | Spring Boot: API REST `/api/**` e entrega da SPA |

## Pré-requisitos

- JDK 21 e Maven 3.9+ (o Node é baixado pelo Maven)
- Docker com Compose (banco local e testes com Testcontainers)
- Conta Gmail com verificação em duas etapas e uma [senha de app](https://myaccount.google.com/apppasswords)

## Configuração

```bash
cp .env.example .env
# edite MAIL_USERNAME e MAIL_PASSWORD (senha de app, sem espaços)
```

## Executar (JAR único)

```bash
docker compose -f database/docker-compose.yml up -d
mvn -DskipTests package
java -jar backend/target/backend-0.1.0-SNAPSHOT.jar
```

Acesse http://localhost:8080.

## Desenvolvimento com hot reload

```bash
docker compose -f database/docker-compose.yml up -d
mvn -pl backend -am spring-boot:run -Dskip.npm -Dskip.installnodenpm   # API em :8080
cd frontend && npm run dev                                              # SPA em :5173
```

Para o link do e-mail apontar para o Vite, defina `APP_BASE_URL=http://localhost:5173` no `.env`.

## Testes

```bash
mvn verify        # backend (JUnit + Testcontainers + GreenMail) e frontend (Vitest); requer Docker
cd frontend && npm test
```

## Verificação manual (Gmail real)

1. Configure o `.env`, suba o banco e rode o JAR.
2. Cadastre um usuário com um e-mail real → aparece a tela "Verifique seu e-mail".
3. Tente entrar antes de ativar → aparece "Sua conta ainda não foi ativada".
4. Abra o e-mail "Ative sua conta" e clique no link → aparece "Conta ativada com sucesso!".
5. Abra o mesmo link de novo → aparece "Este link já foi utilizado".
6. Entre → o perfil mostra os dados corretos.
7. Edite telefone e endereço, salve e recarregue → as alterações persistiram.
8. Tente cadastrar de novo com o mesmo e-mail e, depois, com o mesmo CPF → erro no campo correspondente.
9. Clique em "Sair" e acesse `/perfil` → volta para o login.

## Limitação conhecida

Sem o reenvio de ativação (fora do escopo desta rodada), quem deixar o link expirar
não consegue ativar a conta nem se cadastrar de novo com o mesmo e-mail ou CPF.
````

- [ ] **Passo 2: Rodar a verificação completa a partir de um build limpo**

Rodar: `mvn clean verify`
Esperado: BUILD SUCCESS em `database`, `frontend` e `backend`, com todos os testes passando, incluindo `ModularidadeTest`. Registre a contagem de testes do resumo do Surefire e do Vitest.

- [ ] **Passo 3: Rodar o JAR e conferir a integração**

Rodar:
```bash
docker compose -f database/docker-compose.yml up -d
mvn -q -DskipTests package
java -jar backend/target/backend-0.1.0-SNAPSHOT.jar
```
Em outro terminal:
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/cadastro      # 200 (SPA)
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/perfil               # 401 com codigo NAO_AUTENTICADO
```
Esperado: `200` e `401`. Sem `.env` configurado, a aplicação não sobe por falta de `MAIL_USERNAME`; isso confirma que as credenciais de e-mail são obrigatórias.

- [ ] **Passo 4: Commit**

```bash
git add README.md
git commit -m "docs: README com execução, testes e roteiro de verificação manual"
```

- [ ] **Passo 5: Verificação manual com o Gmail real (humano)**

Siga os 9 passos do README com um `.env` real. Este passo é feito pelo humano parceiro; quem executa o plano registra que ele está pendente.
