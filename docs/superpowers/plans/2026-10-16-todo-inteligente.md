# To-Do Inteligente (MVP): Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gerenciador de tarefas pessoais com CRUD, situação de prazo calculada no backend (RF02) e score semanal de produtividade (RF03), rodando localmente com testes automatizados.

**Architecture:** Monólito modular com três pastas independentes. `backend/` é uma API REST em Spring Boot, com pacotes por funcionalidade (`tarefa`, `produtividade`, `comum`) e regras de negócio em classes puras que recebem a data de hoje de um `Clock` injetado. `frontend/` é uma página única em React, que só exibe o que a API calcula. `database/` traz o PostgreSQL em Docker, e o schema é versionado por Flyway dentro do backend.

**Tech Stack:** Java 21, Spring Boot 3.5.7, Spring Data JPA, Flyway, PostgreSQL 16, Testcontainers 1.21.4, JUnit 5, AssertJ, MockMvc. React 18.3.1, Vite 6, TypeScript 5.8, Vitest 3, Testing Library.

**Spec:** [`docs/superpowers/specs/2026-10-16-todo-inteligente-design.md`](../specs/2026-10-16-todo-inteligente-design.md)

## Global Constraints

- **Java:** 21 (`<java.version>21</java.version>`). O JDK 25 instalado compila com `--release 21`.
- **Spring Boot e Testcontainers:** parent `spring-boot-starter-parent` 3.5.7. Testcontainers fixado em `1.21.4`, porque o Docker Engine 29 rejeita versões anteriores.
- **Banco:** imagem `postgres:16-alpine` no `docker-compose` e nos testes. O banco, o usuário e a senha de desenvolvimento são `todo`/`todo`/`todo`, na porta 5432.
- **Código Java:** pacote raiz `br.com.demosdd.todo`. Os identificadores ficam em português, conforme a spec.
- **API:** base `/api`. Os erros saem em `application/problem+json` com `title`, `detail`, `instance` e, nos erros de validação, `erros: [{campo, mensagem}]`.
- **Data de hoje:** sempre `LocalDate.now(clock)`, com o `Clock` no fuso `America/Sao_Paulo`. Nunca usar `LocalDate.now()` sem o clock.
- **Situações:** `ATRASADA`, `PARA_HOJE`, `NO_PRAZO`, `CONCLUIDA`. Prioridades: `BAIXA`, `MEDIA`, `ALTA`. Status: `PENDENTE`, `CONCLUIDA`.
- **Frontend:** React 18.3.1, Vite 6.3.5, Vitest 3.2.4, TypeScript 5.8.3. Sem biblioteca de estado e sem UI kit.
- **Maven nesta máquina:** o Avast intercepta TLS. Rode `export MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` antes de `mvn` ou `./mvnw`.
- **npm nesta máquina:** se der erro de certificado, rode `export NODE_OPTIONS=--use-system-ca`.
- **Docker Desktop** precisa estar rodando para os testes de integração. Para iniciar: `"/c/Program Files/Docker/Docker/Docker Desktop.exe" &`.
- **Commits:** use `git commit -F -` com heredoc, no Git Bash. Toda mensagem termina com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Isolamento:** não ler nem copiar nada de outros branches do repositório.

## Review Focus

1. **Data de hoje perto da meia-noite UTC.** Às 22h30 em São Paulo, que já é o dia seguinte em UTC, uma tarefa que vence hoje deve aparecer como `PARA_HOJE`, e não como `ATRASADA`. Teste: Task 4, `calculaHojeNoFusoDeSaoPaulo`.
2. **Datas exibidas no frontend sem deslocamento de fuso.** `new Date('2026-10-16')` vira 15/10 no Brasil, então a formatação precisa ser feita sobre a string. Teste: Task 8, `formatarData`.
3. **Limite de 100 caracteres com acentos.** Um título de 100 caracteres acentuados é aceito e um de 101 é rejeitado com erro no campo `titulo`. Teste: Task 4.
4. **Espaços nas pontas.** O título é salvo sem os espaços das pontas, e uma descrição vazia ou só com espaços vira `null`. Isso evita `""` no banco e títulos visualmente duplicados. Testes: Task 4 (`normalizaTituloEDescricao`) e Task 9 (payload do formulário).
5. **Editar uma tarefa concluída em outro dia.** A `dataConclusao` original é mantida, para que o score não mude só porque o título foi corrigido. Teste: Task 5, `editarConcluidaMantemDataConclusaoOriginal`.

---

## Mapa de arquivos

```
.gitignore, .gitattributes
README.md                                                     (Task 11)
database/docker-compose.yml                                   (Task 1)
backend/pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/…               (Task 1)
backend/src/main/resources/application.yml                    (Task 1)
backend/src/main/resources/db/migration/V1__cria_tabela_tarefa.sql   (Task 1)
backend/src/main/java/br/com/demosdd/todo/
  TodoApplication.java                                        (Task 1)
  comum/ClockConfig.java                                      (Task 1)
  comum/CorsConfig.java                                       (Task 4)
  comum/RecursoNaoEncontradoException.java                    (Task 4)
  comum/TratadorGlobalErros.java                              (Task 4)
  tarefa/Prioridade.java, StatusTarefa.java, Situacao.java    (Task 2)
  tarefa/CalculadoraSituacao.java                             (Task 2)
  tarefa/Tarefa.java, TarefaRepository.java                   (Task 3)
  tarefa/TarefaService.java, TarefaController.java            (Task 4; Task 5 acrescenta atualizar)
  tarefa/dto/TarefaRequest.java, TarefaResponse.java          (Task 4)
  produtividade/CalculadoraScore.java                         (Task 6)
  produtividade/ProdutividadeService.java, ProdutividadeController.java (Task 6)
  produtividade/dto/ScoreSemanalResponse.java                 (Task 6)
backend/src/test/java/br/com/demosdd/todo/
  ClockAjustavel.java, TestcontainersConfig.java, IntegracaoBase.java, MigracaoIntegrationTest.java (Task 1)
  comum/ClockConfigTest.java (Task 1), comum/TratadorGlobalErrosTest.java (Task 4)
  tarefa/CalculadoraSituacaoTest.java (Task 2), TarefaTest.java, TarefaRepositoryIntegrationTest.java (Task 3)
  tarefa/TarefaApiIntegrationTest.java (Task 4), TarefaAtualizacaoIntegrationTest.java (Task 5)
  produtividade/CalculadoraScoreTest.java, ProdutividadeApiIntegrationTest.java (Task 6)
frontend/
  package.json, index.html, vite.config.ts, tsconfig.json     (Task 7)
  src/setupTests.ts, src/estilos.css, src/tipos.ts            (Task 7)
  src/api/tarefasApi.ts (+ .test.ts)                          (Task 7)
  src/formatacao.ts (+ .test.ts)                              (Task 8)
  src/componentes/SeloSituacao.tsx, ScoreCard.tsx (+ testes)  (Task 8)
  src/componentes/TarefaForm.tsx (+ teste)                    (Task 9)
  src/componentes/TarefaLista.tsx (+ teste)                   (Task 10)
  src/App.tsx (+ teste), src/main.tsx                         (Task 10)
```

---

### Task 1: Banco em Docker, esqueleto do backend e migration

**Files:**
- Create: `.gitignore`, `.gitattributes`, `database/docker-compose.yml`
- Create: `backend/pom.xml` e o Maven wrapper gerado (`backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`)
- Create: `backend/src/main/java/br/com/demosdd/todo/TodoApplication.java`
- Create: `backend/src/main/java/br/com/demosdd/todo/comum/ClockConfig.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__cria_tabela_tarefa.sql`
- Test: `backend/src/test/java/br/com/demosdd/todo/ClockAjustavel.java`, `TestcontainersConfig.java`, `IntegracaoBase.java`, `MigracaoIntegrationTest.java`, `comum/ClockConfigTest.java`

**Interfaces:**
- Consumes: nada.
- Produces:
  - `ClockConfig.FUSO` (`ZoneId`, América/São Paulo) e o bean `Clock clock()`.
  - Utilitário de teste `ClockAjustavel` com `definirHoje(LocalDate)` e `definirInstante(Instant)`, registrado como bean `@Primary`.
  - Classe base de testes `IntegracaoBase` com os campos protegidos `mockMvc`, `jdbc` e `clock` e a constante `HOJE = 2026-10-14`, uma quarta-feira. Antes de cada teste, ela apaga a tabela `tarefa` e coloca o relógio em `HOJE`.
  - Tabela `tarefa`.

- [ ] **Step 1: Arquivos de repositório e banco**

`.gitignore`:
```gitignore
backend/target/
frontend/node_modules/
frontend/dist/
*.log
.idea/
.vscode/
```

`.gitattributes`:
```gitattributes
mvnw text eol=lf
*.cmd text eol=crlf
```

`database/docker-compose.yml`:
```yaml
name: demo-sdd-todo

services:
  postgres:
    image: postgres:16-alpine
    container_name: todo-postgres
    environment:
      POSTGRES_DB: todo
      POSTGRES_USER: todo
      POSTGRES_PASSWORD: todo
    ports:
      - "5432:5432"
    volumes:
      - todo-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U todo -d todo"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  todo-pgdata:
```

- [ ] **Step 2: `backend/pom.xml` e Maven wrapper**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>

    <groupId>br.com.demosdd</groupId>
    <artifactId>todo-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>todo-backend</name>
    <description>To-Do Inteligente - API REST</description>

    <properties>
        <java.version>21</java.version>
        <!-- Docker Engine 29 exige Testcontainers >= 1.21.4 -->
        <testcontainers.version>1.21.4</testcontainers.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
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
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Gere o wrapper:
```bash
cd backend
export MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT
mvn -N -q org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=3.9.12
ls mvnw mvnw.cmd .mvn/wrapper/maven-wrapper.properties
```
Expected: os três arquivos listados.

- [ ] **Step 3: Escrever os testes (que ainda vão falhar)**

`backend/src/test/java/br/com/demosdd/todo/ClockAjustavel.java`:
```java
package br.com.demosdd.todo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Relógio de teste cujo instante pode ser trocado entre os testes. */
public class ClockAjustavel extends Clock {

    private final ZoneId fuso;
    private volatile Instant instante;

    public ClockAjustavel(Instant instante, ZoneId fuso) {
        this.instante = instante;
        this.fuso = fuso;
    }

    public void definirInstante(Instant novoInstante) {
        this.instante = novoInstante;
    }

    /** Ajusta o relógio para o meio-dia da data informada, no fuso do relógio. */
    public void definirHoje(LocalDate hoje) {
        this.instante = hoje.atTime(LocalTime.NOON).atZone(fuso).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return fuso;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
```

`backend/src/test/java/br/com/demosdd/todo/TestcontainersConfig.java`:
```java
package br.com.demosdd.todo;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import br.com.demosdd.todo.comum.ClockConfig;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    @Bean
    @Primary
    ClockAjustavel clockAjustavel() {
        return new ClockAjustavel(Instant.parse("2026-10-14T15:00:00Z"), ClockConfig.FUSO);
    }
}
```

`backend/src/test/java/br/com/demosdd/todo/IntegracaoBase.java`:
```java
package br.com.demosdd.todo;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegracaoBase {

    /** Quarta-feira; a semana corrente vai de 2026-10-12 a 2026-10-18. */
    protected static final LocalDate HOJE = LocalDate.of(2026, 10, 14);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ClockAjustavel clock;

    @BeforeEach
    void prepararBase() {
        jdbc.update("DELETE FROM tarefa");
        clock.definirHoje(HOJE);
    }
}
```

`backend/src/test/java/br/com/demosdd/todo/MigracaoIntegrationTest.java`:
```java
package br.com.demosdd.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MigracaoIntegrationTest extends IntegracaoBase {

    @Test
    void criaTabelaTarefaVazia() {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM tarefa", Integer.class);
        assertThat(total).isZero();
    }

    @Test
    void rejeitaConcluidaSemDataConclusao() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarefa (id, titulo, data_vencimento, prioridade, status)
                VALUES (gen_random_uuid(), 'x', DATE '2026-10-16', 'ALTA', 'CONCLUIDA')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejeitaPendenteComDataConclusao() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarefa (id, titulo, data_vencimento, prioridade, status, data_conclusao)
                VALUES (gen_random_uuid(), 'x', DATE '2026-10-16', 'ALTA', 'PENDENTE', DATE '2026-10-15')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejeitaPrioridadeForaDoDominio() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarefa (id, titulo, data_vencimento, prioridade, status)
                VALUES (gen_random_uuid(), 'x', DATE '2026-10-16', 'URGENTE', 'PENDENTE')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

`backend/src/test/java/br/com/demosdd/todo/comum/ClockConfigTest.java`:
```java
package br.com.demosdd.todo.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void relogioUsaFusoDeSaoPaulo() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}
```

- [ ] **Step 4: Rodar os testes e ver falhar**

Com o Docker Desktop rodando:
```bash
cd backend && export MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT && ./mvnw -q test
```
Expected: FAIL com erro de compilação. `ClockConfig` não existe e não há classe `@SpringBootApplication`.

- [ ] **Step 5: Implementar**

`backend/src/main/java/br/com/demosdd/todo/TodoApplication.java`:
```java
package br.com.demosdd.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
```

`backend/src/main/java/br/com/demosdd/todo/comum/ClockConfig.java`:
```java
package br.com.demosdd.todo.comum;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Fonte única da data de hoje. Os testes substituem por um relógio fixo. */
@Configuration
public class ClockConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(FUSO);
    }
}
```

`backend/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: todo-backend
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/todo}
    username: ${DB_USER:todo}
    password: ${DB_PASSWORD:todo}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

`backend/src/main/resources/db/migration/V1__cria_tabela_tarefa.sql`:
```sql
CREATE TABLE tarefa (
    id              UUID         PRIMARY KEY,
    titulo          VARCHAR(100) NOT NULL,
    descricao       TEXT,
    data_vencimento DATE         NOT NULL,
    prioridade      VARCHAR(10)  NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    data_conclusao  DATE,
    CONSTRAINT ck_tarefa_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_tarefa_status CHECK (status IN ('PENDENTE', 'CONCLUIDA')),
    CONSTRAINT ck_tarefa_data_conclusao CHECK ((status = 'CONCLUIDA') = (data_conclusao IS NOT NULL))
);
```

- [ ] **Step 6: Rodar os testes e ver passar**

```bash
cd backend && ./mvnw -q test
```
Expected: `Tests run: 5, Failures: 0, Errors: 0`, BUILD SUCCESS.

- [ ] **Step 7: Conferir o compose**

```bash
docker compose -f database/docker-compose.yml up -d
docker compose -f database/docker-compose.yml ps
```
Expected: o serviço `postgres` aparece como `healthy`, depois de alguns segundos. Se a porta 5432 estiver ocupada por outro container, pare esse container com `docker stop <nome>`.

- [ ] **Step 8: Commit**

```bash
git add .gitignore .gitattributes database backend
git commit -F - <<'EOF'
feat: esqueleto do backend, banco em Docker e migration da tabela tarefa

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 2: Enums e cálculo da situação (RF02)

**Files:**
- Create: `backend/src/main/java/br/com/demosdd/todo/tarefa/Prioridade.java`, `StatusTarefa.java`, `Situacao.java`, `CalculadoraSituacao.java`
- Test: `backend/src/test/java/br/com/demosdd/todo/tarefa/CalculadoraSituacaoTest.java`

**Interfaces:**
- Consumes: nada.
- Produces:
  - `enum Prioridade { BAIXA, MEDIA, ALTA }`
  - `enum StatusTarefa { PENDENTE, CONCLUIDA }`
  - `enum Situacao { ATRASADA, PARA_HOJE, NO_PRAZO, CONCLUIDA }`
  - `static Situacao CalculadoraSituacao.calcular(StatusTarefa status, LocalDate dataVencimento, LocalDate hoje)`

- [ ] **Step 1: Escrever o teste que vai falhar**

```java
package br.com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CalculadoraSituacaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 16);

    @Test
    void pendenteVencidaOntemEstaAtrasada() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.PENDENTE, HOJE.minusDays(1), HOJE))
                .isEqualTo(Situacao.ATRASADA);
    }

    @Test
    void pendenteQueVenceHojeEstaParaHoje() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.PENDENTE, HOJE, HOJE))
                .isEqualTo(Situacao.PARA_HOJE);
    }

    @Test
    void pendenteQueVenceAmanhaEstaNoPrazo() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.PENDENTE, HOJE.plusDays(1), HOJE))
                .isEqualTo(Situacao.NO_PRAZO);
    }

    @Test
    void concluidaComVencimentoPassadoEstaConcluida() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.CONCLUIDA, HOJE.minusDays(10), HOJE))
                .isEqualTo(Situacao.CONCLUIDA);
    }

    @Test
    void concluidaComVencimentoFuturoEstaConcluida() {
        assertThat(CalculadoraSituacao.calcular(StatusTarefa.CONCLUIDA, HOJE.plusDays(10), HOJE))
                .isEqualTo(Situacao.CONCLUIDA);
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest=CalculadoraSituacaoTest`
Expected: FAIL, erro de compilação (`CalculadoraSituacao` não existe).

- [ ] **Step 3: Implementar**

`Prioridade.java`:
```java
package br.com.demosdd.todo.tarefa;

public enum Prioridade {
    BAIXA, MEDIA, ALTA
}
```

`StatusTarefa.java`:
```java
package br.com.demosdd.todo.tarefa;

public enum StatusTarefa {
    PENDENTE, CONCLUIDA
}
```

`Situacao.java`:
```java
package br.com.demosdd.todo.tarefa;

/** Situação de prazo calculada em tempo de execução (RF02); nunca persistida. */
public enum Situacao {
    ATRASADA, PARA_HOJE, NO_PRAZO, CONCLUIDA
}
```

`CalculadoraSituacao.java`:
```java
package br.com.demosdd.todo.tarefa;

import java.time.LocalDate;

public final class CalculadoraSituacao {

    private CalculadoraSituacao() {
    }

    public static Situacao calcular(StatusTarefa status, LocalDate dataVencimento, LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return Situacao.CONCLUIDA;
        }
        if (hoje.isAfter(dataVencimento)) {
            return Situacao.ATRASADA;
        }
        if (hoje.isEqual(dataVencimento)) {
            return Situacao.PARA_HOJE;
        }
        return Situacao.NO_PRAZO;
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd backend && ./mvnw -q test -Dtest=CalculadoraSituacaoTest`
Expected: `Tests run: 5, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -F - <<'EOF'
feat: cálculo da situação de prazo da tarefa (RF02)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 3: Entidade `Tarefa` e repositório

**Files:**
- Create: `backend/src/main/java/br/com/demosdd/todo/tarefa/Tarefa.java`, `TarefaRepository.java`
- Test: `backend/src/test/java/br/com/demosdd/todo/tarefa/TarefaTest.java`, `TarefaRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `Prioridade`, `StatusTarefa` (Task 2); `IntegracaoBase`, `HOJE` (Task 1).
- Produces:
  - `new Tarefa(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade)`: gera o id (`UUID`) e começa `PENDENTE`.
  - `void atualizarDados(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade)`
  - `void concluir(LocalDate hoje)`: não faz nada se a tarefa já estiver concluída.
  - `void reabrir()`
  - `void alterarStatus(StatusTarefa novoStatus, LocalDate hoje)`
  - Getters: `getId()`, `getTitulo()`, `getDescricao()`, `getDataVencimento()`, `getPrioridade()`, `getStatus()`, `getDataConclusao()`.
  - `TarefaRepository extends JpaRepository<Tarefa, UUID>`, com `List<Tarefa> listarOrdenado()` e `List<Tarefa> findByStatusAndDataConclusaoBetween(StatusTarefa status, LocalDate inicio, LocalDate fim)`.

- [ ] **Step 1: Escrever os testes que vão falhar**

`TarefaTest.java`:
```java
package br.com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TarefaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 16);

    private Tarefa novaTarefa() {
        return new Tarefa("Pagar boleto", "Conta de luz", HOJE.plusDays(2), Prioridade.ALTA);
    }

    @Test
    void novaTarefaComecaPendenteSemDataConclusao() {
        Tarefa tarefa = novaTarefa();

        assertThat(tarefa.getId()).isNotNull();
        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void concluirGravaDataDeConclusao() {
        Tarefa tarefa = novaTarefa();

        tarefa.concluir(HOJE);

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void concluirTarefaJaConcluidaMantemDataOriginal() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.concluir(HOJE.plusDays(3));

        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void reabrirLimpaDataDeConclusao() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.reabrir();

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void alterarStatusParaConcluidaConclui() {
        Tarefa tarefa = novaTarefa();

        tarefa.alterarStatus(StatusTarefa.CONCLUIDA, HOJE);

        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void alterarStatusParaPendenteReabre() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.alterarStatus(StatusTarefa.PENDENTE, HOJE);

        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.PENDENTE);
        assertThat(tarefa.getDataConclusao()).isNull();
    }

    @Test
    void atualizarDadosNaoMexeNoStatus() {
        Tarefa tarefa = novaTarefa();
        tarefa.concluir(HOJE);

        tarefa.atualizarDados("Outro título", null, HOJE.plusDays(5), Prioridade.BAIXA);

        assertThat(tarefa.getTitulo()).isEqualTo("Outro título");
        assertThat(tarefa.getDescricao()).isNull();
        assertThat(tarefa.getDataVencimento()).isEqualTo(HOJE.plusDays(5));
        assertThat(tarefa.getPrioridade()).isEqualTo(Prioridade.BAIXA);
        assertThat(tarefa.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(tarefa.getDataConclusao()).isEqualTo(HOJE);
    }
}
```

`TarefaRepositoryIntegrationTest.java`:
```java
package br.com.demosdd.todo.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.demosdd.todo.IntegracaoBase;

class TarefaRepositoryIntegrationTest extends IntegracaoBase {

    @Autowired
    private TarefaRepository repository;

    @Test
    void persisteERecuperaTodosOsCampos() {
        Tarefa tarefa = new Tarefa("Estudar", "Capítulo 3 inteiro", LocalDate.of(2026, 10, 20), Prioridade.MEDIA);
        tarefa.concluir(HOJE);
        repository.save(tarefa);

        Tarefa lida = repository.findById(tarefa.getId()).orElseThrow();

        assertThat(lida.getTitulo()).isEqualTo("Estudar");
        assertThat(lida.getDescricao()).isEqualTo("Capítulo 3 inteiro");
        assertThat(lida.getDataVencimento()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(lida.getPrioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(lida.getStatus()).isEqualTo(StatusTarefa.CONCLUIDA);
        assertThat(lida.getDataConclusao()).isEqualTo(HOJE);
    }

    @Test
    void listaPendentesPorVencimentoETituloDepoisConcluidas() {
        Tarefa concluida = new Tarefa("Concluída antiga", null, LocalDate.of(2026, 10, 1), Prioridade.BAIXA);
        concluida.concluir(LocalDate.of(2026, 10, 2));
        Tarefa futura = new Tarefa("Futura", null, LocalDate.of(2026, 10, 20), Prioridade.ALTA);
        Tarefa hojeB = new Tarefa("B hoje", null, HOJE, Prioridade.MEDIA);
        Tarefa hojeA = new Tarefa("A hoje", null, HOJE, Prioridade.MEDIA);
        Tarefa atrasada = new Tarefa("Atrasada", null, LocalDate.of(2026, 10, 10), Prioridade.ALTA);
        repository.saveAll(List.of(concluida, futura, hojeB, hojeA, atrasada));

        assertThat(repository.listarOrdenado())
                .extracting(Tarefa::getTitulo)
                .containsExactly("Atrasada", "A hoje", "B hoje", "Futura", "Concluída antiga");
    }

    @Test
    void buscaConcluidasNoIntervaloInclusivo() {
        Tarefa domingoAnterior = concluidaEm("Domingo anterior", LocalDate.of(2026, 10, 11));
        Tarefa segunda = concluidaEm("Segunda", LocalDate.of(2026, 10, 12));
        Tarefa domingo = concluidaEm("Domingo", LocalDate.of(2026, 10, 18));
        Tarefa segundaSeguinte = concluidaEm("Segunda seguinte", LocalDate.of(2026, 10, 19));
        Tarefa pendente = new Tarefa("Pendente", null, HOJE, Prioridade.BAIXA);
        repository.saveAll(List.of(domingoAnterior, segunda, domingo, segundaSeguinte, pendente));

        List<Tarefa> encontradas = repository.findByStatusAndDataConclusaoBetween(
                StatusTarefa.CONCLUIDA, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));

        assertThat(encontradas).extracting(Tarefa::getTitulo).containsExactlyInAnyOrder("Segunda", "Domingo");
    }

    private Tarefa concluidaEm(String titulo, LocalDate dataConclusao) {
        Tarefa tarefa = new Tarefa(titulo, null, dataConclusao, Prioridade.MEDIA);
        tarefa.concluir(dataConclusao);
        return tarefa;
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest='TarefaTest,TarefaRepositoryIntegrationTest'`
Expected: FAIL, erro de compilação (`Tarefa` não existe).

- [ ] **Step 3: Implementar**

`Tarefa.java`:
```java
package br.com.demosdd.todo.tarefa;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tarefa")
public class Tarefa {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusTarefa status;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    protected Tarefa() {
        // exigido pelo JPA
    }

    public Tarefa(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        this.id = UUID.randomUUID();
        this.status = StatusTarefa.PENDENTE;
        atualizarDados(titulo, descricao, dataVencimento, prioridade);
    }

    public void atualizarDados(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.dataVencimento = dataVencimento;
        this.prioridade = prioridade;
    }

    /** Marca como concluída hoje; concluir de novo preserva a data original. */
    public void concluir(LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return;
        }
        this.status = StatusTarefa.CONCLUIDA;
        this.dataConclusao = hoje;
    }

    public void reabrir() {
        this.status = StatusTarefa.PENDENTE;
        this.dataConclusao = null;
    }

    public void alterarStatus(StatusTarefa novoStatus, LocalDate hoje) {
        if (novoStatus == StatusTarefa.CONCLUIDA) {
            concluir(hoje);
        } else {
            reabrir();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public LocalDate getDataConclusao() {
        return dataConclusao;
    }
}
```

`TarefaRepository.java`:
```java
package br.com.demosdd.todo.tarefa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    /** Pendentes antes das concluídas; depois por vencimento e título. */
    @Query("""
            select t from Tarefa t
            order by case when t.status = br.com.demosdd.todo.tarefa.StatusTarefa.PENDENTE then 0 else 1 end,
                     t.dataVencimento asc,
                     t.titulo asc
            """)
    List<Tarefa> listarOrdenado();

    List<Tarefa> findByStatusAndDataConclusaoBetween(StatusTarefa status, LocalDate inicio, LocalDate fim);
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd backend && ./mvnw -q test -Dtest='TarefaTest,TarefaRepositoryIntegrationTest'`
Expected: `Tests run: 10, Failures: 0`. Se o Hibernate acusar erro de schema na validação, confira se os nomes e tipos de coluna batem com a migration da Task 1.

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -F - <<'EOF'
feat: entidade Tarefa com transições de status e repositório

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 4: API de tarefas (criar, listar, buscar, excluir), erros padronizados e CORS

**Files:**
- Create: `backend/src/main/java/br/com/demosdd/todo/comum/RecursoNaoEncontradoException.java`, `TratadorGlobalErros.java`, `CorsConfig.java`
- Create: `backend/src/main/java/br/com/demosdd/todo/tarefa/dto/TarefaRequest.java`, `TarefaResponse.java`
- Create: `backend/src/main/java/br/com/demosdd/todo/tarefa/TarefaService.java`, `TarefaController.java`
- Modify: `backend/src/test/java/br/com/demosdd/todo/IntegracaoBase.java` (adiciona o helper `criarTarefa`)
- Test: `backend/src/test/java/br/com/demosdd/todo/tarefa/TarefaApiIntegrationTest.java`, `backend/src/test/java/br/com/demosdd/todo/comum/TratadorGlobalErrosTest.java`

**Interfaces:**
- Consumes: `Tarefa`, `TarefaRepository` (Task 3); `CalculadoraSituacao` (Task 2); `Clock` (Task 1).
- Produces:
  - `record TarefaRequest(String titulo, String descricao, LocalDate dataVencimento, Prioridade prioridade, StatusTarefa status)`, com a interface de grupo `TarefaRequest.Atualizacao` que torna `status` obrigatório.
  - `record TarefaResponse(...)` e `static TarefaResponse de(Tarefa, LocalDate hoje)`.
  - `TarefaService`: `LocalDate hoje()`, `List<Tarefa> listar()`, `Tarefa buscar(UUID)`, `Tarefa criar(TarefaRequest)`, `void excluir(UUID)`.
  - `RecursoNaoEncontradoException(String mensagem)`
  - `TratadorGlobalErros.ErroCampo(String campo, String mensagem)` e o método público `tratarErroInesperado(Exception, WebRequest)`.
  - Helper de teste `String IntegracaoBase.criarTarefa(String corpoJson)`, que devolve o id.

- [ ] **Step 1: Adicionar o helper à `IntegracaoBase`**

Substitua o conteúdo de `backend/src/test/java/br/com/demosdd/todo/IntegracaoBase.java` por:
```java
package br.com.demosdd.todo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegracaoBase {

    /** Quarta-feira; a semana corrente vai de 2026-10-12 a 2026-10-18. */
    protected static final LocalDate HOJE = LocalDate.of(2026, 10, 14);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ClockAjustavel clock;

    @BeforeEach
    void prepararBase() {
        jdbc.update("DELETE FROM tarefa");
        clock.definirHoje(HOJE);
    }

    /** Cria uma tarefa via API e devolve o id gerado. */
    protected String criarTarefa(String corpoJson) throws Exception {
        String resposta = mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$.id");
    }
}
```

- [ ] **Step 2: Escrever os testes que vão falhar**

`backend/src/test/java/br/com/demosdd/todo/tarefa/TarefaApiIntegrationTest.java`:
```java
package br.com.demosdd.todo.tarefa;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import br.com.demosdd.todo.IntegracaoBase;

class TarefaApiIntegrationTest extends IntegracaoBase {

    private ResultActions postar(String corpo) throws Exception {
        return mockMvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void criaTarefaPendentePorPadrao() throws Exception {
        postar("""
                {"titulo":"Pagar boleto","descricao":"Conta de luz","dataVencimento":"2026-10-14","prioridade":"ALTA"}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/tarefas/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Pagar boleto"))
                .andExpect(jsonPath("$.descricao").value("Conta de luz"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-14"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").value(nullValue()))
                .andExpect(jsonPath("$.situacao").value("PARA_HOJE"));
    }

    @Test
    void criaTarefaJaConcluidaComDataDeHoje() throws Exception {
        postar("""
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"BAIXA","status":"CONCLUIDA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-14"))
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
    }

    @Test
    void ignoraCamposSomenteLeituraEnviadosPeloCliente() throws Exception {
        postar("""
                {"id":"00000000-0000-0000-0000-000000000001","titulo":"Futura","dataVencimento":"2026-10-20",
                 "prioridade":"MEDIA","dataConclusao":"2020-01-01","situacao":"ATRASADA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not("00000000-0000-0000-0000-000000000001")))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").value(nullValue()))
                .andExpect(jsonPath("$.situacao").value("NO_PRAZO"));
    }

    @Test
    void normalizaTituloEDescricao() throws Exception {
        postar("""
                {"titulo":"  Estudar  ","descricao":"   ","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Estudar"))
                .andExpect(jsonPath("$.descricao").value(nullValue()));
    }

    @Test
    void aceitaTituloCom100CaracteresAcentuados() throws Exception {
        String titulo = "á".repeat(100);
        postar("""
                {"titulo":"%s","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""".formatted(titulo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value(titulo));
    }

    @Test
    void rejeitaTituloCom101Caracteres() throws Exception {
        postar("""
                {"titulo":"%s","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""".formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos."))
                .andExpect(jsonPath("$.instance").value("/api/tarefas"))
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("deve ter no máximo 100 caracteres"));
    }

    @Test
    void rejeitaTituloSoComEspacos() throws Exception {
        postar("""
                {"titulo":"   ","dataVencimento":"2026-10-20","prioridade":"MEDIA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("é obrigatório"));
    }

    @Test
    void listaErrosDeTodosOsCamposObrigatorios() throws Exception {
        postar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("titulo", "dataVencimento", "prioridade")));
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        postar("{\"titulo\":")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou malformado."));
    }

    @Test
    void rejeitaPrioridadeInvalida() throws Exception {
        postar("""
                {"titulo":"X","dataVencimento":"2026-10-20","prioridade":"URGENTE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o campo 'prioridade'."));
    }

    @Test
    void rejeitaDataInexistente() throws Exception {
        postar("""
                {"titulo":"X","dataVencimento":"2026-02-30","prioridade":"MEDIA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para o campo 'dataVencimento'."));
    }

    @Test
    void buscaTarefaPorId() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Ler","dataVencimento":"2026-10-10","prioridade":"BAIXA"}""");

        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.situacao").value("ATRASADA"));
    }

    @Test
    void retorna404ParaTarefaInexistente() throws Exception {
        mockMvc.perform(get("/api/tarefas/{id}", "6f1c2a7e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Tarefa não encontrada: 6f1c2a7e-0000-4000-8000-000000000000"));
    }

    @Test
    void retorna400ParaIdInvalido() throws Exception {
        mockMvc.perform(get("/api/tarefas/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("Valor inválido para 'id': abc"));
    }

    @Test
    void listaTarefasNaOrdemComSituacaoCalculada() throws Exception {
        criarTarefa("""
                {"titulo":"Concluída","dataVencimento":"2026-10-01","prioridade":"BAIXA","status":"CONCLUIDA"}""");
        criarTarefa("""
                {"titulo":"No prazo","dataVencimento":"2026-10-20","prioridade":"ALTA"}""");
        criarTarefa("""
                {"titulo":"Hoje","dataVencimento":"2026-10-14","prioridade":"MEDIA"}""");
        criarTarefa("""
                {"titulo":"Atrasada","dataVencimento":"2026-10-10","prioridade":"ALTA"}""");

        mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Atrasada"))
                .andExpect(jsonPath("$[0].situacao").value("ATRASADA"))
                .andExpect(jsonPath("$[1].situacao").value("PARA_HOJE"))
                .andExpect(jsonPath("$[2].situacao").value("NO_PRAZO"))
                .andExpect(jsonPath("$[3].situacao").value("CONCLUIDA"));
    }

    @Test
    void calculaHojeNoFusoDeSaoPaulo() throws Exception {
        // 01:30 UTC do dia 15 = 22:30 do dia 14 em São Paulo
        clock.definirInstante(Instant.parse("2026-10-15T01:30:00Z"));
        String id = criarTarefa("""
                {"titulo":"Vence hoje","dataVencimento":"2026-10-14","prioridade":"MEDIA"}""");

        mockMvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.situacao").value("PARA_HOJE"));
    }

    @Test
    void excluiTarefa() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Descartável","dataVencimento":"2026-10-20","prioridade":"BAIXA"}""");

        mockMvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void retorna404AoExcluirInexistente() throws Exception {
        mockMvc.perform(delete("/api/tarefas/{id}", "6f1c2a7e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void liberaCorsParaOFrontend() throws Exception {
        mockMvc.perform(options("/api/tarefas")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void recusaCorsDeOutraOrigem() throws Exception {
        mockMvc.perform(options("/api/tarefas")
                        .header("Origin", "http://outro.exemplo")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
```

`backend/src/test/java/br/com/demosdd/todo/comum/TratadorGlobalErrosTest.java`:
```java
package br.com.demosdd.todo.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class TratadorGlobalErrosTest {

    @Test
    void erroInesperadoNaoExpoeDetalhesInternos() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/tarefas"));

        ResponseEntity<ProblemDetail> resposta = new TratadorGlobalErros()
                .tratarErroInesperado(new IllegalStateException("senha do banco"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        ProblemDetail corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.getTitle()).isEqualTo("Erro interno");
        assertThat(corpo.getDetail()).isEqualTo("Ocorreu um erro inesperado.");
        assertThat(corpo.getInstance()).hasToString("/api/tarefas");
    }
}
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest='TarefaApiIntegrationTest,TratadorGlobalErrosTest'`
Expected: FAIL, erro de compilação (`TratadorGlobalErros` não existe).

- [ ] **Step 4: Implementar o tratamento de erros e o CORS**

`comum/RecursoNaoEncontradoException.java`:
```java
package br.com.demosdd.todo.comum;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
```

`comum/TratadorGlobalErros.java`:
```java
package br.com.demosdd.todo.comum;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.JsonMappingException;

/** Converte exceções em respostas application/problem+json (RFC 9457). */
@RestControllerAdvice
public class TratadorGlobalErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalErros.class);

    public record ErroCampo(String campo, String mensagem) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErroCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroCampo::campo))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos são inválidos.", request);
        problema.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detalhe = "Corpo da requisição ausente ou malformado.";
        if (ex.getCause() instanceof JsonMappingException erro && !erro.getPath().isEmpty()) {
            String campo = erro.getPath().get(erro.getPath().size() - 1).getFieldName();
            detalhe = "Valor inválido para o campo '" + campo + "'.";
        }
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", detalhe, request));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String nome = ex instanceof MethodArgumentTypeMismatchException erro ? erro.getName() : ex.getPropertyName();
        String detalhe = "Valor inválido para '" + nome + "': " + ex.getValue();
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detalhe, request));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarNaoEncontrado(RecursoNaoEncontradoException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarErroInesperado(Exception ex, WebRequest request) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.", request));
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        if (request instanceof ServletWebRequest servlet) {
            problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
        }
        return problema;
    }
}
```

`comum/CorsConfig.java`:
```java
package br.com.demosdd.todo.comum;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

- [ ] **Step 5: Implementar os DTOs, o service e o controller**

`tarefa/dto/TarefaRequest.java`:
```java
package br.com.demosdd.todo.tarefa.dto;

import java.time.LocalDate;

import br.com.demosdd.todo.tarefa.Prioridade;
import br.com.demosdd.todo.tarefa.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada de criação e atualização. Campos somente leitura (id, dataConclusao, situacao)
 * não existem aqui e são ignorados se enviados.
 */
public record TarefaRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String titulo,

        String descricao,

        @NotNull(message = "é obrigatória")
        LocalDate dataVencimento,

        @NotNull(message = "é obrigatória")
        Prioridade prioridade,

        @NotNull(groups = TarefaRequest.Atualizacao.class, message = "é obrigatório")
        StatusTarefa status) {

    /** Grupo de validação do PUT, em que o status é obrigatório. */
    public interface Atualizacao {
    }

    public TarefaRequest {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao;
    }
}
```

`tarefa/dto/TarefaResponse.java`:
```java
package br.com.demosdd.todo.tarefa.dto;

import java.time.LocalDate;
import java.util.UUID;

import br.com.demosdd.todo.tarefa.CalculadoraSituacao;
import br.com.demosdd.todo.tarefa.Prioridade;
import br.com.demosdd.todo.tarefa.Situacao;
import br.com.demosdd.todo.tarefa.StatusTarefa;
import br.com.demosdd.todo.tarefa.Tarefa;

public record TarefaResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate dataVencimento,
        Prioridade prioridade,
        StatusTarefa status,
        LocalDate dataConclusao,
        Situacao situacao) {

    public static TarefaResponse de(Tarefa tarefa, LocalDate hoje) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getDataVencimento(),
                tarefa.getPrioridade(),
                tarefa.getStatus(),
                tarefa.getDataConclusao(),
                CalculadoraSituacao.calcular(tarefa.getStatus(), tarefa.getDataVencimento(), hoje));
    }
}
```

`tarefa/TarefaService.java`:
```java
package br.com.demosdd.todo.tarefa;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.todo.comum.RecursoNaoEncontradoException;
import br.com.demosdd.todo.tarefa.dto.TarefaRequest;

@Service
@Transactional
public class TarefaService {

    private final TarefaRepository repository;
    private final Clock clock;

    public TarefaService(TarefaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar() {
        return repository.listarOrdenado();
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada: " + id));
    }

    public Tarefa criar(TarefaRequest dados) {
        Tarefa tarefa = new Tarefa(dados.titulo(), dados.descricao(), dados.dataVencimento(), dados.prioridade());
        if (dados.status() == StatusTarefa.CONCLUIDA) {
            tarefa.concluir(hoje());
        }
        return repository.save(tarefa);
    }

    public void excluir(UUID id) {
        repository.delete(buscar(id));
    }
}
```

`tarefa/TarefaController.java`:
```java
package br.com.demosdd.todo.tarefa;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.demosdd.todo.tarefa.dto.TarefaRequest;
import br.com.demosdd.todo.tarefa.dto.TarefaResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    @GetMapping
    public List<TarefaResponse> listar() {
        LocalDate hoje = service.hoje();
        return service.listar().stream().map(tarefa -> TarefaResponse.de(tarefa, hoje)).toList();
    }

    @GetMapping("/{id}")
    public TarefaResponse buscar(@PathVariable UUID id) {
        return TarefaResponse.de(service.buscar(id), service.hoje());
    }

    @PostMapping
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest dados) {
        Tarefa tarefa = service.criar(dados);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(tarefa.getId()).toUri();
        return ResponseEntity.created(local).body(TarefaResponse.de(tarefa, service.hoje()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
```

- [ ] **Step 6: Rodar e ver passar**

Run: `cd backend && ./mvnw -q test -Dtest='TarefaApiIntegrationTest,TratadorGlobalErrosTest'`
Expected: `Tests run: 21, Failures: 0`.

- [ ] **Step 7: Rodar a suíte inteira**

Run: `cd backend && ./mvnw -q test`
Expected: BUILD SUCCESS, sem falhas.

- [ ] **Step 8: Commit**

```bash
git add backend/src
git commit -F - <<'EOF'
feat: API de tarefas (criar, listar, buscar, excluir) com erros ProblemDetail e CORS

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 5: Atualização (`PUT`) e transições de status

**Files:**
- Modify: `backend/src/main/java/br/com/demosdd/todo/tarefa/TarefaService.java` (adiciona `atualizar`)
- Modify: `backend/src/main/java/br/com/demosdd/todo/tarefa/TarefaController.java` (adiciona o `@PutMapping`)
- Test: `backend/src/test/java/br/com/demosdd/todo/tarefa/TarefaAtualizacaoIntegrationTest.java`

**Interfaces:**
- Consumes: `TarefaRequest` e o grupo `TarefaRequest.Atualizacao` (Task 4); `Tarefa.atualizarDados` e `Tarefa.alterarStatus` (Task 3); `IntegracaoBase.criarTarefa` (Task 4).
- Produces: `Tarefa TarefaService.atualizar(UUID id, TarefaRequest dados)`; `PUT /api/tarefas/{id}`.

- [ ] **Step 1: Escrever o teste que vai falhar**

```java
package br.com.demosdd.todo.tarefa;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import br.com.demosdd.todo.IntegracaoBase;

class TarefaAtualizacaoIntegrationTest extends IntegracaoBase {

    private static final String PENDENTE = """
            {"titulo":"Pagar boleto","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA"}""";

    private ResultActions atualizar(String id, String corpo) throws Exception {
        return mockMvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void atualizaTodosOsCampos() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"Pagar fatura","descricao":"Cartão","dataVencimento":"2026-10-14","prioridade":"BAIXA","status":"PENDENTE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Pagar fatura"))
                .andExpect(jsonPath("$.descricao").value("Cartão"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-10-14"))
                .andExpect(jsonPath("$.prioridade").value("BAIXA"))
                .andExpect(jsonPath("$.situacao").value("PARA_HOJE"));
    }

    @Test
    void concluirGravaDataDeHoje() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"Pagar boleto","descricao":"Luz","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-14"))
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
    }

    @Test
    void reabrirLimpaDataConclusao() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""");

        atualizar(id, """
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").value(nullValue()))
                .andExpect(jsonPath("$.situacao").value("NO_PRAZO"));
    }

    @Test
    void editarConcluidaMantemDataConclusaoOriginal() throws Exception {
        String id = criarTarefa("""
                {"titulo":"Feita","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""");
        clock.definirHoje(LocalDate.of(2026, 10, 16));

        atualizar(id, """
                {"titulo":"Feita (corrigido)","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"CONCLUIDA"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Feita (corrigido)"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-10-14"));
    }

    @Test
    void exigeStatusNoPut() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"Pagar boleto","dataVencimento":"2026-10-20","prioridade":"ALTA"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("status"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("é obrigatório"));
    }

    @Test
    void validaCamposNoPut() throws Exception {
        String id = criarTarefa(PENDENTE);

        atualizar(id, """
                {"titulo":"","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"));
    }

    @Test
    void retorna404AoAtualizarInexistente() throws Exception {
        atualizar("6f1c2a7e-0000-4000-8000-000000000000", """
                {"titulo":"X","dataVencimento":"2026-10-20","prioridade":"ALTA","status":"PENDENTE"}""")
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest=TarefaAtualizacaoIntegrationTest`
Expected: FAIL. Os testes de `PUT` recebem 405, porque o método ainda não é suportado.

- [ ] **Step 3: Implementar**

Em `TarefaService.java`, adicione depois de `criar`:
```java
    public Tarefa atualizar(UUID id, TarefaRequest dados) {
        Tarefa tarefa = buscar(id);
        tarefa.atualizarDados(dados.titulo(), dados.descricao(), dados.dataVencimento(), dados.prioridade());
        if (dados.status() != tarefa.getStatus()) {
            tarefa.alterarStatus(dados.status(), hoje());
        }
        return tarefa;
    }
```
O `buscar` é `readOnly`, mas é chamado de dentro da transação de escrita da classe (`@Transactional`) e participa dela. A entidade fica gerenciada, então as alterações são gravadas no commit.

Em `TarefaController.java`, adicione os imports:
```java
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.groups.Default;
```
e o método, depois de `criar`:
```java
    @PutMapping("/{id}")
    public TarefaResponse atualizar(@PathVariable UUID id,
            @Validated({Default.class, TarefaRequest.Atualizacao.class}) @RequestBody TarefaRequest dados) {
        return TarefaResponse.de(service.atualizar(id, dados), service.hoje());
    }
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd backend && ./mvnw -q test`
Expected: BUILD SUCCESS. Os 7 testes novos passam, e o `POST` sem status continua aceito.

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -F - <<'EOF'
feat: atualização de tarefa com transições concluir/reabrir

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 6: Score semanal de produtividade (RF03)

**Files:**
- Create: `backend/src/main/java/br/com/demosdd/todo/produtividade/CalculadoraScore.java`
- Create: `backend/src/main/java/br/com/demosdd/todo/produtividade/ProdutividadeService.java`, `ProdutividadeController.java`
- Create: `backend/src/main/java/br/com/demosdd/todo/produtividade/dto/ScoreSemanalResponse.java`
- Test: `backend/src/test/java/br/com/demosdd/todo/produtividade/CalculadoraScoreTest.java`, `ProdutividadeApiIntegrationTest.java`

**Interfaces:**
- Consumes: `TarefaRepository.findByStatusAndDataConclusaoBetween`, `Tarefa`, `StatusTarefa` (Task 3); `Clock` (Task 1); `IntegracaoBase` (Task 1).
- Produces:
  - `CalculadoraScore`: `record Conclusao(LocalDate dataConclusao, LocalDate dataVencimento)`, `static LocalDate inicioSemana(LocalDate)`, `static LocalDate fimSemana(LocalDate)`, `static long contarNoPrazo(List<Conclusao>)`, `static BigDecimal percentual(long noPrazo, long total)`.
  - `record ScoreSemanalResponse(LocalDate inicioSemana, LocalDate fimSemana, long totalConcluidas, long concluidasNoPrazo, BigDecimal percentualNoPrazo)`.
  - `GET /api/produtividade/score-semanal`.

- [ ] **Step 1: Escrever os testes que vão falhar**

`CalculadoraScoreTest.java`:
```java
package br.com.demosdd.todo.produtividade;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.demosdd.todo.produtividade.CalculadoraScore.Conclusao;

class CalculadoraScoreTest {

    @Test
    void semanaDeUmaQuartaVaiDeSegundaADomingo() {
        LocalDate quarta = LocalDate.of(2026, 10, 14);
        assertThat(CalculadoraScore.inicioSemana(quarta)).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(CalculadoraScore.fimSemana(quarta)).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void segundaFeiraIniciaAPropriaSemana() {
        LocalDate segunda = LocalDate.of(2026, 10, 12);
        assertThat(CalculadoraScore.inicioSemana(segunda)).isEqualTo(segunda);
        assertThat(CalculadoraScore.fimSemana(segunda)).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void domingoEncerraAPropriaSemana() {
        LocalDate domingo = LocalDate.of(2026, 10, 18);
        assertThat(CalculadoraScore.inicioSemana(domingo)).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(CalculadoraScore.fimSemana(domingo)).isEqualTo(domingo);
    }

    @Test
    void conclusaoNoDiaDoVencimentoContaNoPrazo() {
        LocalDate dia = LocalDate.of(2026, 10, 14);
        assertThat(CalculadoraScore.contarNoPrazo(List.of(new Conclusao(dia, dia)))).isEqualTo(1);
    }

    @Test
    void conclusaoUmDiaDepoisDoVencimentoNaoContaNoPrazo() {
        LocalDate vencimento = LocalDate.of(2026, 10, 14);
        assertThat(CalculadoraScore.contarNoPrazo(List.of(new Conclusao(vencimento.plusDays(1), vencimento))))
                .isZero();
    }

    @Test
    void percentualSemConclusoesENulo() {
        assertThat(CalculadoraScore.percentual(0, 0)).isNull();
    }

    @Test
    void percentualArredondaParaDuasCasasHalfUp() {
        assertThat(CalculadoraScore.percentual(2, 3)).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(CalculadoraScore.percentual(1, 3)).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    void percentualTemSempreDuasCasas() {
        assertThat(CalculadoraScore.percentual(4, 5)).isEqualTo(new BigDecimal("80.00"));
        assertThat(CalculadoraScore.percentual(3, 3)).isEqualTo(new BigDecimal("100.00"));
        assertThat(CalculadoraScore.percentual(0, 2)).isEqualTo(new BigDecimal("0.00"));
    }
}
```

`ProdutividadeApiIntegrationTest.java`:
```java
package br.com.demosdd.todo.produtividade;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.demosdd.todo.IntegracaoBase;
import br.com.demosdd.todo.tarefa.Prioridade;
import br.com.demosdd.todo.tarefa.Tarefa;
import br.com.demosdd.todo.tarefa.TarefaRepository;

class ProdutividadeApiIntegrationTest extends IntegracaoBase {

    @Autowired
    private TarefaRepository repository;

    private Tarefa concluida(String titulo, LocalDate vencimento, LocalDate conclusao) {
        Tarefa tarefa = new Tarefa(titulo, null, vencimento, Prioridade.MEDIA);
        tarefa.concluir(conclusao);
        return tarefa;
    }

    @Test
    void semConclusoesNaSemanaPercentualENulo() throws Exception {
        repository.save(new Tarefa("Pendente", null, HOJE, Prioridade.ALTA));

        mockMvc.perform(get("/api/produtividade/score-semanal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-12"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-18"))
                .andExpect(jsonPath("$.totalConcluidas").value(0))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(0))
                .andExpect(jsonPath("$.percentualNoPrazo").value(nullValue()));
    }

    @Test
    void calculaScoreSomenteComConclusoesDaSemanaCorrente() throws Exception {
        clock.definirHoje(LocalDate.of(2026, 10, 18)); // domingo, último dia da semana
        repository.saveAll(List.of(
                concluida("Antes do prazo", LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 12)),
                concluida("No dia do prazo", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)),
                concluida("Atrasada", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 18)),
                concluida("Semana passada", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11)),
                new Tarefa("Pendente", null, LocalDate.of(2026, 10, 1), Prioridade.ALTA)));

        mockMvc.perform(get("/api/produtividade/score-semanal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioSemana").value("2026-10-12"))
                .andExpect(jsonPath("$.fimSemana").value("2026-10-18"))
                .andExpect(jsonPath("$.totalConcluidas").value(3))
                .andExpect(jsonPath("$.concluidasNoPrazo").value(2))
                .andExpect(jsonPath("$.percentualNoPrazo").value(66.67));
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd backend && ./mvnw -q test -Dtest='CalculadoraScoreTest,ProdutividadeApiIntegrationTest'`
Expected: FAIL, erro de compilação (`CalculadoraScore` não existe).

- [ ] **Step 3: Implementar**

`produtividade/CalculadoraScore.java`:
```java
package br.com.demosdd.todo.produtividade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Regras puras do score semanal (RF03). */
public final class CalculadoraScore {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public record Conclusao(LocalDate dataConclusao, LocalDate dataVencimento) {

        boolean noPrazo() {
            return !dataConclusao.isAfter(dataVencimento);
        }
    }

    private CalculadoraScore() {
    }

    public static LocalDate inicioSemana(LocalDate hoje) {
        return hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate fimSemana(LocalDate hoje) {
        return inicioSemana(hoje).plusDays(6);
    }

    public static long contarNoPrazo(List<Conclusao> conclusoes) {
        return conclusoes.stream().filter(Conclusao::noPrazo).count();
    }

    /** Percentual com duas casas (HALF_UP); null quando não há conclusões. */
    public static BigDecimal percentual(long noPrazo, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(noPrazo).multiply(CEM).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
```

`produtividade/dto/ScoreSemanalResponse.java`:
```java
package br.com.demosdd.todo.produtividade.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScoreSemanalResponse(
        LocalDate inicioSemana,
        LocalDate fimSemana,
        long totalConcluidas,
        long concluidasNoPrazo,
        BigDecimal percentualNoPrazo) {
}
```

`produtividade/ProdutividadeService.java`:
```java
package br.com.demosdd.todo.produtividade;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.todo.produtividade.CalculadoraScore.Conclusao;
import br.com.demosdd.todo.produtividade.dto.ScoreSemanalResponse;
import br.com.demosdd.todo.tarefa.StatusTarefa;
import br.com.demosdd.todo.tarefa.TarefaRepository;

@Service
public class ProdutividadeService {

    private final TarefaRepository repository;
    private final Clock clock;

    public ProdutividadeService(TarefaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScoreSemanalResponse scoreSemanal() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = CalculadoraScore.inicioSemana(hoje);
        LocalDate fim = CalculadoraScore.fimSemana(hoje);
        List<Conclusao> conclusoes = repository
                .findByStatusAndDataConclusaoBetween(StatusTarefa.CONCLUIDA, inicio, fim).stream()
                .map(tarefa -> new Conclusao(tarefa.getDataConclusao(), tarefa.getDataVencimento()))
                .toList();
        long noPrazo = CalculadoraScore.contarNoPrazo(conclusoes);
        return new ScoreSemanalResponse(inicio, fim, conclusoes.size(), noPrazo,
                CalculadoraScore.percentual(noPrazo, conclusoes.size()));
    }
}
```

`produtividade/ProdutividadeController.java`:
```java
package br.com.demosdd.todo.produtividade;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.demosdd.todo.produtividade.dto.ScoreSemanalResponse;

@RestController
@RequestMapping("/api/produtividade")
public class ProdutividadeController {

    private final ProdutividadeService service;

    public ProdutividadeController(ProdutividadeService service) {
        this.service = service;
    }

    @GetMapping("/score-semanal")
    public ScoreSemanalResponse scoreSemanal() {
        return service.scoreSemanal();
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd backend && ./mvnw -q verify`
Expected: BUILD SUCCESS. A suíte inteira do backend passa.

- [ ] **Step 5: Commit**

```bash
git add backend/src
git commit -F - <<'EOF'
feat: score semanal de produtividade (RF03)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 7: Esqueleto do frontend, tipos e cliente da API

**Files:**
- Create: `frontend/package.json`, `frontend/index.html`, `frontend/vite.config.ts`, `frontend/tsconfig.json`
- Create: `frontend/src/setupTests.ts`, `frontend/src/estilos.css`, `frontend/src/tipos.ts`, `frontend/src/api/tarefasApi.ts`
- Test: `frontend/src/api/tarefasApi.test.ts`

**Interfaces:**
- Consumes: o contrato REST das Tasks 4 a 6.
- Produces:
  - Tipos `Prioridade`, `StatusTarefa`, `Situacao`, `Tarefa`, `TarefaInput`, `ScoreSemanal` e `ErroCampo` (em `src/tipos.ts`).
  - `class ErroApi extends Error`, com `status: number`, `detail: string` e `erros: ErroCampo[]`. Status 0 indica falha de rede.
  - `tarefasApi.listar(): Promise<Tarefa[]>`
  - `tarefasApi.criar(dados: TarefaInput): Promise<Tarefa>`
  - `tarefasApi.atualizar(id: string, dados: TarefaInput): Promise<Tarefa>`
  - `tarefasApi.excluir(id: string): Promise<void>`
  - `tarefasApi.scoreSemanal(): Promise<ScoreSemanal>`
  - Classes CSS: `app`, `banner-erro`, `score-card`, `score-semana`, `tarefa-form`, `campo`, `erro-campo`, `acoes-form`, `acoes`, `lista-vazia`, `selo` e `selo-<situacao em minúsculas>`.

- [ ] **Step 1: Criar o `package.json` e instalar as dependências**

`frontend/package.json`:
```json
{
  "name": "todo-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "test": "vitest run"
  }
}
```

```bash
cd frontend
npm install --save-exact react@18.3.1 react-dom@18.3.1
npm install --save-exact -D typescript@5.8.3 vite@6.3.5 @vitejs/plugin-react@4.6.0 vitest@3.2.4 jsdom@26.1.0 \
  @types/react@18.3.23 @types/react-dom@18.3.7 \
  @testing-library/react@16.3.0 @testing-library/dom@10.4.0 @testing-library/user-event@14.6.1 @testing-library/jest-dom@6.6.3
```
Expected: o `package-lock.json` é criado sem erros. Se der erro de certificado, rode `export NODE_OPTIONS=--use-system-ca` e repita.

- [ ] **Step 2: Arquivos de configuração**

`frontend/index.html`:
```html
<!doctype html>
<html lang="pt-BR">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>To-Do Inteligente</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

`frontend/vite.config.ts`:
```ts
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
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
    "isolatedModules": true,
    "skipLibCheck": true,
    "types": ["vite/client"]
  },
  "include": ["src", "vite.config.ts"]
}
```

`frontend/src/setupTests.ts`:
```ts
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
});
```

`frontend/src/estilos.css`:
```css
:root {
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  color: #1f2933;
  background: #f5f7fa;
}

body {
  margin: 0;
}

.app {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}

h1 {
  margin-top: 0;
}

.banner-erro {
  background: #fde8e8;
  color: #9b1c1c;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.score-card,
.tarefa-form {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.score-card h2,
.tarefa-form h2 {
  margin: 0 0 8px;
  font-size: 1.1rem;
}

.score-semana {
  color: #52606d;
  font-size: 0.9rem;
  margin: 4px 0 0;
}

.tarefa-form {
  display: grid;
  gap: 12px;
}

.campo {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.erro-campo {
  color: #c81e1e;
  font-size: 0.85rem;
}

.acoes-form,
.acoes {
  display: flex;
  gap: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th,
td {
  text-align: left;
  padding: 8px;
  border-bottom: 1px solid #e4e7eb;
}

.lista-vazia {
  color: #52606d;
}

.selo {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #fff;
}

.selo-atrasada {
  background: #c81e1e;
}

.selo-para_hoje {
  background: #d97706;
}

.selo-no_prazo {
  background: #2563eb;
}

.selo-concluida {
  background: #16a34a;
}
```

`frontend/src/tipos.ts`:
```ts
export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA';
export type StatusTarefa = 'PENDENTE' | 'CONCLUIDA';
export type Situacao = 'ATRASADA' | 'PARA_HOJE' | 'NO_PRAZO' | 'CONCLUIDA';

export interface Tarefa {
  id: string;
  titulo: string;
  descricao: string | null;
  dataVencimento: string; // YYYY-MM-DD
  prioridade: Prioridade;
  status: StatusTarefa;
  dataConclusao: string | null;
  situacao: Situacao;
}

export interface TarefaInput {
  titulo: string;
  descricao: string | null;
  dataVencimento: string;
  prioridade: Prioridade;
  status: StatusTarefa;
}

export interface ScoreSemanal {
  inicioSemana: string;
  fimSemana: string;
  totalConcluidas: number;
  concluidasNoPrazo: number;
  percentualNoPrazo: number | null;
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}
```

- [ ] **Step 3: Escrever o teste que vai falhar**

`frontend/src/api/tarefasApi.test.ts`:
```ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { TarefaInput } from '../tipos';
import { ErroApi, tarefasApi } from './tarefasApi';

const fetchMock = vi.fn();

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const dados: TarefaInput = {
  titulo: 'Pagar boleto',
  descricao: null,
  dataVencimento: '2026-10-16',
  prioridade: 'ALTA',
  status: 'PENDENTE',
};

describe('tarefasApi', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    fetchMock.mockReset();
    vi.unstubAllGlobals();
  });

  it('lista tarefas com GET em /api/tarefas', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, [{ id: '1' }]));

    const tarefas = await tarefasApi.listar();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/tarefas');
    expect(tarefas).toEqual([{ id: '1' }]);
  });

  it('cria tarefa com POST e corpo JSON', async () => {
    fetchMock.mockResolvedValue(respostaJson(201, { id: '1', ...dados }));

    await tarefasApi.criar(dados);

    expect(fetchMock).toHaveBeenCalledWith('/api/tarefas', {
      method: 'POST',
      body: JSON.stringify(dados),
      headers: { 'Content-Type': 'application/json' },
    });
  });

  it('atualiza tarefa com PUT no id', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, { id: 'abc', ...dados }));

    await tarefasApi.atualizar('abc', dados);

    expect(fetchMock.mock.calls[0][0]).toBe('/api/tarefas/abc');
    expect(fetchMock.mock.calls[0][1].method).toBe('PUT');
  });

  it('exclui tarefa e aceita resposta 204 sem corpo', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(tarefasApi.excluir('abc')).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[0][1].method).toBe('DELETE');
  });

  it('busca o score semanal', async () => {
    fetchMock.mockResolvedValue(respostaJson(200, { totalConcluidas: 0, percentualNoPrazo: null }));

    const score = await tarefasApi.scoreSemanal();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/produtividade/score-semanal');
    expect(score.percentualNoPrazo).toBeNull();
  });

  it('converte problem+json em ErroApi com erros de campo', async () => {
    fetchMock.mockResolvedValue(
      respostaJson(400, {
        title: 'Dados inválidos',
        detail: 'Um ou mais campos são inválidos.',
        erros: [{ campo: 'titulo', mensagem: 'é obrigatório' }],
      }),
    );

    const erro = await tarefasApi.criar(dados).catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ErroApi);
    expect(erro).toMatchObject({
      status: 400,
      detail: 'Um ou mais campos são inválidos.',
      erros: [{ campo: 'titulo', mensagem: 'é obrigatório' }],
    });
  });

  it('converte falha de rede em ErroApi com status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));

    const erro = await tarefasApi.listar().catch((e: unknown) => e);

    expect(erro).toMatchObject({ status: 0, detail: 'Não foi possível conectar ao servidor.', erros: [] });
  });

  it('usa mensagem genérica quando o erro não tem corpo JSON', async () => {
    fetchMock.mockResolvedValue(new Response('Bad Gateway', { status: 502 }));

    const erro = await tarefasApi.listar().catch((e: unknown) => e);

    expect(erro).toMatchObject({ status: 502, detail: 'Erro 502', erros: [] });
  });
});
```

- [ ] **Step 4: Rodar e ver falhar**

Run: `cd frontend && npm test`
Expected: FAIL com `Failed to resolve import "./tarefasApi"`.

- [ ] **Step 5: Implementar**

`frontend/src/api/tarefasApi.ts`:
```ts
import type { ErroCampo, ScoreSemanal, Tarefa, TarefaInput } from '../tipos';

/** Erro da API já convertido do problem+json. status 0 = falha de rede. */
export class ErroApi extends Error {
  readonly status: number;
  readonly detail: string;
  readonly erros: ErroCampo[];

  constructor(status: number, detail: string, erros: ErroCampo[] = []) {
    super(detail);
    this.name = 'ErroApi';
    this.status = status;
    this.detail = detail;
    this.erros = erros;
  }
}

const BASE = '/api';

async function converterErro(resposta: Response): Promise<ErroApi> {
  const generico = `Erro ${resposta.status}`;
  try {
    const corpo = await resposta.json();
    return new ErroApi(
      resposta.status,
      typeof corpo.detail === 'string' ? corpo.detail : generico,
      Array.isArray(corpo.erros) ? corpo.erros : [],
    );
  } catch {
    return new ErroApi(resposta.status, generico);
  }
}

async function requisitar<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  const opcoes: RequestInit = init.body
    ? { ...init, headers: { 'Content-Type': 'application/json' } }
    : init;
  let resposta: Response;
  try {
    resposta = await fetch(BASE + caminho, opcoes);
  } catch {
    throw new ErroApi(0, 'Não foi possível conectar ao servidor.');
  }
  if (!resposta.ok) {
    throw await converterErro(resposta);
  }
  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

export const tarefasApi = {
  listar: () => requisitar<Tarefa[]>('/tarefas'),
  criar: (dados: TarefaInput) =>
    requisitar<Tarefa>('/tarefas', { method: 'POST', body: JSON.stringify(dados) }),
  atualizar: (id: string, dados: TarefaInput) =>
    requisitar<Tarefa>(`/tarefas/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluir: (id: string) => requisitar<void>(`/tarefas/${id}`, { method: 'DELETE' }),
  scoreSemanal: () => requisitar<ScoreSemanal>('/produtividade/score-semanal'),
};
```

- [ ] **Step 6: Rodar e ver passar**

Run: `cd frontend && npm test`
Expected: `8 passed`.

- [ ] **Step 7: Commit**

```bash
git add frontend
git commit -F - <<'EOF'
feat: esqueleto do frontend React com tipos e cliente da API

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 8: Formatação, selo de situação e card do score

**Files:**
- Create: `frontend/src/formatacao.ts`, `frontend/src/componentes/SeloSituacao.tsx`, `frontend/src/componentes/ScoreCard.tsx`
- Test: `frontend/src/formatacao.test.ts`, `frontend/src/componentes/SeloSituacao.test.tsx`, `frontend/src/componentes/ScoreCard.test.tsx`

**Interfaces:**
- Consumes: `Situacao`, `Prioridade`, `ScoreSemanal` (Task 7).
- Produces:
  - `formatarData(iso: string): string`, que converte `'2026-10-16'` em `'16/10/2026'`.
  - `formatarPercentual(valor: number): string`, que converte `66.67` em `'66,67'`.
  - `ROTULOS_PRIORIDADE: Record<Prioridade, string>` e `ROTULOS_SITUACAO: Record<Situacao, string>`.
  - Componentes `<SeloSituacao situacao={Situacao} />` e `<ScoreCard score={ScoreSemanal} />`.

- [ ] **Step 1: Escrever os testes que vão falhar**

`frontend/src/formatacao.test.ts`:
```ts
import { describe, expect, it } from 'vitest';
import { formatarData, formatarPercentual } from './formatacao';

describe('formatarData', () => {
  it('converte ISO em dd/mm/aaaa sem passar por Date (sem deslocamento de fuso)', () => {
    expect(formatarData('2026-10-16')).toBe('16/10/2026');
    expect(formatarData('2026-01-01')).toBe('01/01/2026');
  });
});

describe('formatarPercentual', () => {
  it('usa vírgula e sempre duas casas', () => {
    expect(formatarPercentual(66.67)).toBe('66,67');
    expect(formatarPercentual(80)).toBe('80,00');
    expect(formatarPercentual(100)).toBe('100,00');
  });
});
```

`frontend/src/componentes/SeloSituacao.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Situacao } from '../tipos';
import { SeloSituacao } from './SeloSituacao';

describe('SeloSituacao', () => {
  it.each<[Situacao, string, string]>([
    ['ATRASADA', 'Atrasada', 'selo-atrasada'],
    ['PARA_HOJE', 'Para Hoje', 'selo-para_hoje'],
    ['NO_PRAZO', 'No Prazo', 'selo-no_prazo'],
    ['CONCLUIDA', 'Concluída', 'selo-concluida'],
  ])('mostra %s como "%s" com a classe %s', (situacao, texto, classe) => {
    render(<SeloSituacao situacao={situacao} />);

    const selo = screen.getByText(texto);
    expect(selo).toHaveClass('selo', classe);
  });
});
```

`frontend/src/componentes/ScoreCard.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { ScoreSemanal } from '../tipos';
import { ScoreCard } from './ScoreCard';

const semana = { inicioSemana: '2026-10-12', fimSemana: '2026-10-18' };

describe('ScoreCard', () => {
  it('mostra percentual formatado, contagem e semana', () => {
    const score: ScoreSemanal = { ...semana, totalConcluidas: 5, concluidasNoPrazo: 4, percentualNoPrazo: 80 };

    render(<ScoreCard score={score} />);

    expect(screen.getByText('80,00% no prazo (4 de 5)')).toBeInTheDocument();
    expect(screen.getByText('Semana de 12/10/2026 a 18/10/2026')).toBeInTheDocument();
  });

  it('mostra mensagem quando não houve conclusões na semana', () => {
    const score: ScoreSemanal = { ...semana, totalConcluidas: 0, concluidasNoPrazo: 0, percentualNoPrazo: null };

    render(<ScoreCard score={score} />);

    expect(screen.getByText('Nenhuma tarefa concluída nesta semana')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npm test`
Expected: FAIL com `Failed to resolve import` para `./formatacao`, `./SeloSituacao` e `./ScoreCard`.

- [ ] **Step 3: Implementar**

`frontend/src/formatacao.ts`:
```ts
import type { Prioridade, Situacao } from './tipos';

export const ROTULOS_PRIORIDADE: Record<Prioridade, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
};

export const ROTULOS_SITUACAO: Record<Situacao, string> = {
  ATRASADA: 'Atrasada',
  PARA_HOJE: 'Para Hoje',
  NO_PRAZO: 'No Prazo',
  CONCLUIDA: 'Concluída',
};

/** 'YYYY-MM-DD' -> 'dd/mm/aaaa'. Não usa Date: new Date('2026-10-16') seria meia-noite UTC e viraria dia 15 no Brasil. */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

export function formatarPercentual(valor: number): string {
  return valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
```

`frontend/src/componentes/SeloSituacao.tsx`:
```tsx
import { ROTULOS_SITUACAO } from '../formatacao';
import type { Situacao } from '../tipos';

export function SeloSituacao({ situacao }: { situacao: Situacao }) {
  return <span className={`selo selo-${situacao.toLowerCase()}`}>{ROTULOS_SITUACAO[situacao]}</span>;
}
```

`frontend/src/componentes/ScoreCard.tsx`:
```tsx
import { formatarData, formatarPercentual } from '../formatacao';
import type { ScoreSemanal } from '../tipos';

export function ScoreCard({ score }: { score: ScoreSemanal }) {
  return (
    <section className="score-card" aria-label="Score semanal">
      <h2>Produtividade da semana</h2>
      {score.percentualNoPrazo === null ? (
        <p>Nenhuma tarefa concluída nesta semana</p>
      ) : (
        <p>{`${formatarPercentual(score.percentualNoPrazo)}% no prazo (${score.concluidasNoPrazo} de ${score.totalConcluidas})`}</p>
      )}
      <p className="score-semana">
        {`Semana de ${formatarData(score.inicioSemana)} a ${formatarData(score.fimSemana)}`}
      </p>
    </section>
  );
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd frontend && npm test`
Expected: todos os testes passam (os 8 da Task 7 e os 8 novos).

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -F - <<'EOF'
feat: formatação, selo de situação e card do score semanal

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 9: Formulário de tarefa

**Files:**
- Create: `frontend/src/componentes/TarefaForm.tsx`
- Test: `frontend/src/componentes/TarefaForm.test.tsx`

**Interfaces:**
- Consumes: `ErroApi` (Task 7); `Tarefa`, `TarefaInput`, `Prioridade` (Task 7); `ROTULOS_PRIORIDADE` (Task 8).
- Produces: `<TarefaForm tarefaEmEdicao={Tarefa | null} onSalvar={(dados: TarefaInput) => Promise<void>} onCancelar={() => void} />`.
  - Se `onSalvar` lançar um `ErroApi`, o formulário mostra os erros dele.
  - Ao criar com sucesso, os campos são limpos.
  - Ao editar, o `status` da tarefa é preservado no payload.
  - Rótulos: "Título", "Descrição", "Vencimento", "Prioridade".
  - Botões: "Adicionar tarefa", "Salvar alterações" e "Cancelar" (só aparece na edição).

- [ ] **Step 1: Escrever o teste que vai falhar**

```tsx
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ErroApi } from '../api/tarefasApi';
import type { Tarefa } from '../tipos';
import { TarefaForm } from './TarefaForm';

const tarefaConcluida: Tarefa = {
  id: 't1',
  titulo: 'Pagar boleto',
  descricao: 'Luz',
  dataVencimento: '2026-10-20',
  prioridade: 'ALTA',
  status: 'CONCLUIDA',
  dataConclusao: '2026-10-14',
  situacao: 'CONCLUIDA',
};

function preencherVencimento(valor: string) {
  fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: valor } });
}

describe('TarefaForm', () => {
  it('mostra erros e não envia quando os campos obrigatórios estão vazios', async () => {
    const onSalvar = vi.fn();
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(screen.getByText('Título é obrigatório')).toBeInTheDocument();
    expect(screen.getByText('Data de vencimento é obrigatória')).toBeInTheDocument();
    expect(onSalvar).not.toHaveBeenCalled();
  });

  it('rejeita título com mais de 100 caracteres', async () => {
    const onSalvar = vi.fn();
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    fireEvent.change(screen.getByLabelText('Título'), { target: { value: 'a'.repeat(101) } });
    preencherVencimento('2026-10-20');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(screen.getByText('Título deve ter no máximo 100 caracteres')).toBeInTheDocument();
    expect(onSalvar).not.toHaveBeenCalled();
  });

  it('envia nova tarefa normalizada e limpa o formulário', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined);
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('Título'), '  Estudar  ');
    await userEvent.type(screen.getByLabelText('Descrição'), '   ');
    preencherVencimento('2026-10-20');
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'ALTA');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(onSalvar).toHaveBeenCalledWith({
      titulo: 'Estudar',
      descricao: null,
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
      status: 'PENDENTE',
    });
    await waitFor(() => expect(screen.getByLabelText('Título')).toHaveValue(''));
    expect(screen.getByLabelText('Prioridade')).toHaveValue('MEDIA');
  });

  it('preenche os campos ao editar e preserva o status', async () => {
    const onSalvar = vi.fn().mockResolvedValue(undefined);
    const onCancelar = vi.fn();
    render(<TarefaForm tarefaEmEdicao={tarefaConcluida} onSalvar={onSalvar} onCancelar={onCancelar} />);

    expect(screen.getByLabelText('Título')).toHaveValue('Pagar boleto');
    expect(screen.getByLabelText('Descrição')).toHaveValue('Luz');
    expect(screen.getByLabelText('Vencimento')).toHaveValue('2026-10-20');
    expect(screen.getByLabelText('Prioridade')).toHaveValue('ALTA');

    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));
    expect(onSalvar).toHaveBeenCalledWith({
      titulo: 'Pagar boleto',
      descricao: 'Luz',
      dataVencimento: '2026-10-20',
      prioridade: 'ALTA',
      status: 'CONCLUIDA',
    });

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onCancelar).toHaveBeenCalled();
  });

  it('não mostra Cancelar ao criar', () => {
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={vi.fn()} onCancelar={vi.fn()} />);

    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();
  });

  it('mostra erros de campo retornados pelo backend', async () => {
    const onSalvar = vi.fn().mockRejectedValue(
      new ErroApi(400, 'Um ou mais campos são inválidos.', [
        { campo: 'titulo', mensagem: 'deve ter no máximo 100 caracteres' },
      ]),
    );
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('Título'), 'Estudar');
    preencherVencimento('2026-10-20');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(await screen.findByText('deve ter no máximo 100 caracteres')).toBeInTheDocument();
    expect(screen.getByLabelText('Título')).toHaveValue('Estudar');
  });

  it('mostra erro geral quando o backend falha sem erros de campo', async () => {
    const onSalvar = vi.fn().mockRejectedValue(new ErroApi(500, 'Ocorreu um erro inesperado.'));
    render(<TarefaForm tarefaEmEdicao={null} onSalvar={onSalvar} onCancelar={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('Título'), 'Estudar');
    preencherVencimento('2026-10-20');

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(await screen.findByText('Ocorreu um erro inesperado.')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npm test -- TarefaForm`
Expected: FAIL com `Failed to resolve import "./TarefaForm"`.

- [ ] **Step 3: Implementar**

`frontend/src/componentes/TarefaForm.tsx`:
```tsx
import { useState, type FormEvent } from 'react';
import { ErroApi } from '../api/tarefasApi';
import { ROTULOS_PRIORIDADE } from '../formatacao';
import type { Prioridade, Tarefa, TarefaInput } from '../tipos';

type CampoFormulario = 'titulo' | 'descricao' | 'dataVencimento' | 'prioridade';
type ErrosCampos = Partial<Record<CampoFormulario, string>>;

interface Props {
  tarefaEmEdicao: Tarefa | null;
  onSalvar: (dados: TarefaInput) => Promise<void>;
  onCancelar: () => void;
}

const PRIORIDADES = Object.keys(ROTULOS_PRIORIDADE) as Prioridade[];

export function TarefaForm({ tarefaEmEdicao, onSalvar, onCancelar }: Props) {
  const [titulo, setTitulo] = useState(tarefaEmEdicao?.titulo ?? '');
  const [descricao, setDescricao] = useState(tarefaEmEdicao?.descricao ?? '');
  const [dataVencimento, setDataVencimento] = useState(tarefaEmEdicao?.dataVencimento ?? '');
  const [prioridade, setPrioridade] = useState<Prioridade>(tarefaEmEdicao?.prioridade ?? 'MEDIA');
  const [erros, setErros] = useState<ErrosCampos>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function validar(): ErrosCampos {
    const encontrados: ErrosCampos = {};
    const tituloLimpo = titulo.trim();
    if (tituloLimpo === '') {
      encontrados.titulo = 'Título é obrigatório';
    } else if (tituloLimpo.length > 100) {
      encontrados.titulo = 'Título deve ter no máximo 100 caracteres';
    }
    if (dataVencimento === '') {
      encontrados.dataVencimento = 'Data de vencimento é obrigatória';
    }
    return encontrados;
  }

  function limpar() {
    setTitulo('');
    setDescricao('');
    setDataVencimento('');
    setPrioridade('MEDIA');
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const encontrados = validar();
    setErros(encontrados);
    setErroGeral(null);
    if (Object.keys(encontrados).length > 0) {
      return;
    }
    setEnviando(true);
    try {
      await onSalvar({
        titulo: titulo.trim(),
        descricao: descricao.trim() === '' ? null : descricao,
        dataVencimento,
        prioridade,
        status: tarefaEmEdicao?.status ?? 'PENDENTE',
      });
      if (!tarefaEmEdicao) {
        limpar();
      }
    } catch (erro) {
      if (erro instanceof ErroApi && erro.erros.length > 0) {
        setErros(Object.fromEntries(erro.erros.map((e) => [e.campo, e.mensagem])) as ErrosCampos);
      } else {
        setErroGeral(erro instanceof ErroApi ? erro.detail : 'Erro inesperado ao salvar.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="tarefa-form" onSubmit={enviar} noValidate>
      <h2>{tarefaEmEdicao ? 'Editar tarefa' : 'Nova tarefa'}</h2>

      <div className="campo">
        <label htmlFor="campo-titulo">Título</label>
        <input id="campo-titulo" value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        {erros.titulo && <span className="erro-campo">{erros.titulo}</span>}
      </div>

      <div className="campo">
        <label htmlFor="campo-descricao">Descrição</label>
        <textarea id="campo-descricao" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        {erros.descricao && <span className="erro-campo">{erros.descricao}</span>}
      </div>

      <div className="campo">
        <label htmlFor="campo-vencimento">Vencimento</label>
        <input
          id="campo-vencimento"
          type="date"
          value={dataVencimento}
          onChange={(e) => setDataVencimento(e.target.value)}
        />
        {erros.dataVencimento && <span className="erro-campo">{erros.dataVencimento}</span>}
      </div>

      <div className="campo">
        <label htmlFor="campo-prioridade">Prioridade</label>
        <select
          id="campo-prioridade"
          value={prioridade}
          onChange={(e) => setPrioridade(e.target.value as Prioridade)}
        >
          {PRIORIDADES.map((p) => (
            <option key={p} value={p}>
              {ROTULOS_PRIORIDADE[p]}
            </option>
          ))}
        </select>
        {erros.prioridade && <span className="erro-campo">{erros.prioridade}</span>}
      </div>

      {erroGeral && (
        <p className="erro-campo" role="alert">
          {erroGeral}
        </p>
      )}

      <div className="acoes-form">
        <button type="submit" disabled={enviando}>
          {tarefaEmEdicao ? 'Salvar alterações' : 'Adicionar tarefa'}
        </button>
        {tarefaEmEdicao && (
          <button type="button" onClick={onCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `cd frontend && npm test`
Expected: todos os testes passam (7 novos em `TarefaForm`).

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -F - <<'EOF'
feat: formulário de criação e edição de tarefas

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 10: Lista de tarefas, App e ponto de entrada

**Files:**
- Create: `frontend/src/componentes/TarefaLista.tsx`, `frontend/src/App.tsx`, `frontend/src/main.tsx`
- Test: `frontend/src/componentes/TarefaLista.test.tsx`, `frontend/src/App.test.tsx`

**Interfaces:**
- Consumes: `tarefasApi` e `ErroApi` (Task 7); `formatarData`, `ROTULOS_PRIORIDADE`, `SeloSituacao` e `ScoreCard` (Task 8); `TarefaForm` (Task 9).
- Produces:
  - `<TarefaLista tarefas onEditar onAlternarStatus onExcluir />`. Os callbacks recebem a `Tarefa`, e `onExcluir` só é chamado depois de `window.confirm`.
  - `<App />`, a página completa.

- [ ] **Step 1: Escrever os testes que vão falhar**

`frontend/src/componentes/TarefaLista.test.tsx`:
```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Tarefa } from '../tipos';
import { TarefaLista } from './TarefaLista';

const pendente: Tarefa = {
  id: '1',
  titulo: 'Pagar boleto',
  descricao: null,
  dataVencimento: '2026-10-16',
  prioridade: 'MEDIA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacao: 'PARA_HOJE',
};

const concluida: Tarefa = { ...pendente, id: '2', status: 'CONCLUIDA', dataConclusao: '2026-10-15', situacao: 'CONCLUIDA' };

function renderizar(tarefas: Tarefa[]) {
  const callbacks = { onEditar: vi.fn(), onAlternarStatus: vi.fn(), onExcluir: vi.fn() };
  render(<TarefaLista tarefas={tarefas} {...callbacks} />);
  return callbacks;
}

describe('TarefaLista', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('mostra mensagem quando não há tarefas', () => {
    renderizar([]);

    expect(screen.getByText('Nenhuma tarefa cadastrada')).toBeInTheDocument();
  });

  it('mostra dados formatados e selo de situação', () => {
    renderizar([pendente]);

    expect(screen.getByText('Pagar boleto')).toBeInTheDocument();
    expect(screen.getByText('Média')).toBeInTheDocument();
    expect(screen.getByText('16/10/2026')).toBeInTheDocument();
    expect(screen.getByText('Para Hoje')).toHaveClass('selo-para_hoje');
  });

  it('oferece Concluir para pendente', async () => {
    const { onAlternarStatus } = renderizar([pendente]);

    await userEvent.click(screen.getByRole('button', { name: 'Concluir' }));

    expect(onAlternarStatus).toHaveBeenCalledWith(pendente);
  });

  it('oferece Reabrir para concluída', async () => {
    const { onAlternarStatus } = renderizar([concluida]);

    await userEvent.click(screen.getByRole('button', { name: 'Reabrir' }));

    expect(onAlternarStatus).toHaveBeenCalledWith(concluida);
  });

  it('chama onEditar com a tarefa', async () => {
    const { onEditar } = renderizar([pendente]);

    await userEvent.click(screen.getByRole('button', { name: 'Editar' }));

    expect(onEditar).toHaveBeenCalledWith(pendente);
  });

  it('exclui somente após confirmação', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    const { onExcluir } = renderizar([pendente]);

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    expect(onExcluir).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    expect(onExcluir).toHaveBeenCalledWith(pendente);
    expect(confirmar).toHaveBeenCalledWith('Excluir a tarefa "Pagar boleto"?');
  });
});
```

`frontend/src/App.test.tsx`:
```tsx
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { ErroApi, tarefasApi } from './api/tarefasApi';
import type { ScoreSemanal, Tarefa } from './tipos';

vi.mock('./api/tarefasApi', async (importOriginal) => {
  const original = await importOriginal<typeof import('./api/tarefasApi')>();
  return {
    ...original,
    tarefasApi: {
      listar: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn(),
      excluir: vi.fn(),
      scoreSemanal: vi.fn(),
    },
  };
});

const api = vi.mocked(tarefasApi);

const pendente: Tarefa = {
  id: '1',
  titulo: 'Pagar boleto',
  descricao: null,
  dataVencimento: '2026-10-16',
  prioridade: 'ALTA',
  status: 'PENDENTE',
  dataConclusao: null,
  situacao: 'PARA_HOJE',
};

const scoreVazio: ScoreSemanal = {
  inicioSemana: '2026-10-12',
  fimSemana: '2026-10-18',
  totalConcluidas: 0,
  concluidasNoPrazo: 0,
  percentualNoPrazo: null,
};

describe('App', () => {
  beforeEach(() => {
    api.listar.mockResolvedValue([pendente]);
    api.scoreSemanal.mockResolvedValue(scoreVazio);
    api.criar.mockResolvedValue(pendente);
    api.atualizar.mockResolvedValue({ ...pendente, status: 'CONCLUIDA' });
    api.excluir.mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();
  });

  it('mostra carregando e depois tarefas e score', async () => {
    render(<App />);

    expect(screen.getByText('Carregando…')).toBeInTheDocument();
    expect(await screen.findByText('Pagar boleto')).toBeInTheDocument();
    expect(screen.getByText('Nenhuma tarefa concluída nesta semana')).toBeInTheDocument();
  });

  it('conclui tarefa e recarrega lista e score', async () => {
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Concluir' }));

    expect(api.atualizar).toHaveBeenCalledWith('1', {
      titulo: 'Pagar boleto',
      descricao: null,
      dataVencimento: '2026-10-16',
      prioridade: 'ALTA',
      status: 'CONCLUIDA',
    });
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
    expect(api.scoreSemanal).toHaveBeenCalledTimes(2);
  });

  it('exclui tarefa após confirmar e recarrega', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));

    expect(api.excluir).toHaveBeenCalledWith('1');
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
  });

  it('cria tarefa pelo formulário e recarrega', async () => {
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.type(screen.getByLabelText('Título'), 'Estudar');
    fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: '2026-10-20' } });
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar tarefa' }));

    expect(api.criar).toHaveBeenCalledWith({
      titulo: 'Estudar',
      descricao: null,
      dataVencimento: '2026-10-20',
      prioridade: 'MEDIA',
      status: 'PENDENTE',
    });
    await waitFor(() => expect(api.listar).toHaveBeenCalledTimes(2));
  });

  it('edita tarefa existente', async () => {
    api.atualizar.mockResolvedValue(pendente);
    render(<App />);
    await screen.findByText('Pagar boleto');

    await userEvent.click(screen.getByRole('button', { name: 'Editar' }));
    expect(screen.getByLabelText('Título')).toHaveValue('Pagar boleto');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(api.atualizar).toHaveBeenCalledWith('1', expect.objectContaining({ status: 'PENDENTE' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Adicionar tarefa' })).toBeInTheDocument());
  });

  it('mostra banner quando o servidor não responde', async () => {
    api.listar.mockRejectedValue(new ErroApi(0, 'Não foi possível conectar ao servidor.'));
    render(<App />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar ao servidor.');
    expect(screen.queryByText('Carregando…')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd frontend && npm test`
Expected: FAIL com `Failed to resolve import` para `./TarefaLista` e `./App`.

- [ ] **Step 3: Implementar**

`frontend/src/componentes/TarefaLista.tsx`:
```tsx
import { formatarData, ROTULOS_PRIORIDADE } from '../formatacao';
import type { Tarefa } from '../tipos';
import { SeloSituacao } from './SeloSituacao';

interface Props {
  tarefas: Tarefa[];
  onEditar: (tarefa: Tarefa) => void;
  onAlternarStatus: (tarefa: Tarefa) => void;
  onExcluir: (tarefa: Tarefa) => void;
}

export function TarefaLista({ tarefas, onEditar, onAlternarStatus, onExcluir }: Props) {
  if (tarefas.length === 0) {
    return <p className="lista-vazia">Nenhuma tarefa cadastrada</p>;
  }

  function confirmarExclusao(tarefa: Tarefa) {
    if (window.confirm(`Excluir a tarefa "${tarefa.titulo}"?`)) {
      onExcluir(tarefa);
    }
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Título</th>
          <th>Prioridade</th>
          <th>Vencimento</th>
          <th>Situação</th>
          <th>Ações</th>
        </tr>
      </thead>
      <tbody>
        {tarefas.map((tarefa) => (
          <tr key={tarefa.id}>
            <td>{tarefa.titulo}</td>
            <td>{ROTULOS_PRIORIDADE[tarefa.prioridade]}</td>
            <td>{formatarData(tarefa.dataVencimento)}</td>
            <td>
              <SeloSituacao situacao={tarefa.situacao} />
            </td>
            <td className="acoes">
              <button type="button" onClick={() => onEditar(tarefa)}>
                Editar
              </button>
              <button type="button" onClick={() => onAlternarStatus(tarefa)}>
                {tarefa.status === 'PENDENTE' ? 'Concluir' : 'Reabrir'}
              </button>
              <button type="button" onClick={() => confirmarExclusao(tarefa)}>
                Excluir
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

`frontend/src/App.tsx`:
```tsx
import { useCallback, useEffect, useState } from 'react';
import { ErroApi, tarefasApi } from './api/tarefasApi';
import { ScoreCard } from './componentes/ScoreCard';
import { TarefaForm } from './componentes/TarefaForm';
import { TarefaLista } from './componentes/TarefaLista';
import type { ScoreSemanal, StatusTarefa, Tarefa, TarefaInput } from './tipos';

function mensagemDe(erro: unknown): string {
  return erro instanceof ErroApi ? erro.detail : 'Erro inesperado.';
}

function comStatus(tarefa: Tarefa, status: StatusTarefa): TarefaInput {
  return {
    titulo: tarefa.titulo,
    descricao: tarefa.descricao,
    dataVencimento: tarefa.dataVencimento,
    prioridade: tarefa.prioridade,
    status,
  };
}

export function App() {
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null);
  const [score, setScore] = useState<ScoreSemanal | null>(null);
  const [emEdicao, setEmEdicao] = useState<Tarefa | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    try {
      const [lista, scoreSemanal] = await Promise.all([tarefasApi.listar(), tarefasApi.scoreSemanal()]);
      setTarefas(lista);
      setScore(scoreSemanal);
      setErro(null);
    } catch (e) {
      setErro(mensagemDe(e));
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  // Erros de salvar sobem para o TarefaForm, que os exibe junto aos campos.
  async function salvar(dados: TarefaInput) {
    if (emEdicao) {
      await tarefasApi.atualizar(emEdicao.id, dados);
    } else {
      await tarefasApi.criar(dados);
    }
    setEmEdicao(null);
    await carregar();
  }

  async function executar(acao: () => Promise<unknown>) {
    try {
      await acao();
      await carregar();
    } catch (e) {
      setErro(mensagemDe(e));
    }
  }

  function alternarStatus(tarefa: Tarefa) {
    const novoStatus: StatusTarefa = tarefa.status === 'PENDENTE' ? 'CONCLUIDA' : 'PENDENTE';
    void executar(() => tarefasApi.atualizar(tarefa.id, comStatus(tarefa, novoStatus)));
  }

  function excluir(tarefa: Tarefa) {
    void executar(async () => {
      await tarefasApi.excluir(tarefa.id);
      if (emEdicao?.id === tarefa.id) {
        setEmEdicao(null);
      }
    });
  }

  return (
    <main className="app">
      <h1>To-Do Inteligente</h1>
      {erro && (
        <div className="banner-erro" role="alert">
          {erro}
        </div>
      )}
      {score && <ScoreCard score={score} />}
      <TarefaForm
        key={emEdicao?.id ?? 'nova'}
        tarefaEmEdicao={emEdicao}
        onSalvar={salvar}
        onCancelar={() => setEmEdicao(null)}
      />
      {tarefas === null ? (
        !erro && <p>Carregando…</p>
      ) : (
        <TarefaLista tarefas={tarefas} onEditar={setEmEdicao} onAlternarStatus={alternarStatus} onExcluir={excluir} />
      )}
    </main>
  );
}
```

`frontend/src/main.tsx`:
```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './estilos.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
```

- [ ] **Step 4: Rodar os testes e o build**

```bash
cd frontend && npm test && npm run build
```
Expected: todos os testes passam, e o `tsc` com o `vite build` termina sem erros e gera `dist/`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -F - <<'EOF'
feat: lista de tarefas e página principal integrando formulário e score

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 11: README e verificação ponta a ponta

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: tudo o que foi feito nas tarefas anteriores.
- Produces: instruções de execução e a confirmação manual dos critérios de pronto (seção 11 da spec).

- [ ] **Step 1: Escrever o `README.md`**

````markdown
# To-Do Inteligente (MVP)

Gerenciador de tarefas pessoais com situação de prazo calculada no backend e score semanal de produtividade.
Design: [`docs/superpowers/specs/2026-10-16-todo-inteligente-design.md`](docs/superpowers/specs/2026-10-16-todo-inteligente-design.md).

## Estrutura

| pasta | conteúdo |
|---|---|
| `backend/` | API REST em Spring Boot 3 (Java 21), com migrations Flyway |
| `frontend/` | Página única em React 18 + Vite + TypeScript |
| `database/` | `docker-compose.yml` com PostgreSQL 16 |

## Pré-requisitos

JDK 21 ou mais recente, Node 20 ou mais recente, e Docker Desktop.

## Como rodar

```bash
# 1. Banco
docker compose -f database/docker-compose.yml up -d

# 2. Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Variáveis opcionais do backend: `DB_URL` (padrão `jdbc:postgresql://localhost:5432/todo`), `DB_USER` (padrão `todo`) e `DB_PASSWORD` (padrão `todo`).

## Testes

```bash
cd backend && ./mvnw verify   # unitários + integração (Testcontainers; requer Docker)
cd frontend && npm test       # Vitest + Testing Library
```

## API

| método | rota | descrição |
|---|---|---|
| `GET` | `/api/tarefas` | lista: pendentes por vencimento, depois as concluídas |
| `GET` | `/api/tarefas/{id}` | busca uma tarefa |
| `POST` | `/api/tarefas` | cria (o `status` é opcional, padrão `PENDENTE`) |
| `PUT` | `/api/tarefas/{id}` | substitui todos os campos; mudar o `status` conclui ou reabre |
| `DELETE` | `/api/tarefas/{id}` | exclui |
| `GET` | `/api/produtividade/score-semanal` | percentual concluído no prazo na semana corrente (de segunda a domingo) |

Toda tarefa devolvida tem um campo `situacao` calculado: `ATRASADA`, `PARA_HOJE`, `NO_PRAZO` ou `CONCLUIDA`.
Os erros seguem o formato `application/problem+json` (RFC 9457). Nos erros de validação, a resposta traz a lista `erros: [{campo, mensagem}]`.

## Solução de problemas

- **Os testes de integração falham com erro de Docker.** O Docker Desktop precisa estar rodando.
- **O Maven falha com `PKIX path building failed`.** Um antivírus está interceptando o TLS. Rode `export MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (no PowerShell: `$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"`) e tente de novo.
- **O `npm install` falha com erro de certificado.** Rode `export NODE_OPTIONS=--use-system-ca`.
- **A porta 5432 está ocupada.** Pare o outro PostgreSQL ou container que estiver usando a porta.
````

- [ ] **Step 2: Rodar as suítes completas**

```bash
cd backend && ./mvnw verify
cd ../frontend && npm test
```
Expected: as duas terminam sem falhas. Anote as contagens de testes para o resumo final.

- [ ] **Step 3: Verificação manual no navegador**

Com o banco, o backend e o frontend rodando como descrito no README, abra `http://localhost:5173` e:
1. Crie as tarefas "Atrasada" (vencimento ontem), "Hoje" (vencimento hoje) e "Futura" (vencimento daqui a 3 dias). Confira os selos Atrasada (vermelho), Para Hoje (laranja) e No Prazo (azul). A ordem deve ser Atrasada, Hoje, Futura.
2. O score deve mostrar "Nenhuma tarefa concluída nesta semana".
3. Conclua "Hoje" e "Atrasada". O selo vira Concluída (verde), as duas vão para o fim da lista, e o score mostra "50,00% no prazo (1 de 2)".
4. Reabra "Atrasada". O score passa a mostrar "100,00% no prazo (1 de 1)".
5. Edite "Futura" com o título vazio. O erro aparece no campo e nada é enviado.
6. Exclua "Futura", confirmando no diálogo. Ela some da lista.
7. Pare o backend e recarregue a página. Deve aparecer o banner de erro.

Opcionalmente, use `playwright-core` com `chromium.launch({ channel: 'chrome' })` para automatizar esse roteiro. Esse script não entra no repositório.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -F - <<'EOF'
docs: README com execução local, API e solução de problemas

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```
