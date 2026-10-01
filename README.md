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

O `docker compose -f database/docker-compose.yml` lê o `.env` da pasta `database/` (não o da raiz).
Os valores padrão (`DB_USERNAME`/`DB_PASSWORD` = `cadastro`) funcionam sem configuração; para usar outros,
exporte-os no shell ou crie `database/.env`, e mantenha os mesmos valores no `.env` da raiz usado pela aplicação.

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
mvn -DskipTests -pl database,frontend install   # uma vez (e quando mudar database/ ou frontend/)
mvn -pl backend spring-boot:run                    # API em :8080
cd frontend && npm run dev                         # SPA em :5173
```

Os comandos `npm` exigem Node 24 no `PATH`. Se não houver um instalado, o build Maven do frontend baixa um em
`frontend/node/` (execute `mvn -DskipTests -pl database,frontend install` antes) e basta colocá-lo no `PATH`:
`export PATH="$PWD/frontend/node:$PATH"` (Git Bash/Linux/macOS) na raiz do projeto.

Para o link do e-mail apontar para o Vite, defina `APP_BASE_URL=http://localhost:5173` no `.env`.

## Testes

```bash
mvn verify        # backend (JUnit + Testcontainers + GreenMail) e frontend (Vitest); requer Docker
cd frontend && npm test   # também requer Node 24 no PATH (veja acima)
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
