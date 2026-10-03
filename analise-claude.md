# Análise do projeto — Cadastro de Usuários

- **Branch analisada:** `feature/demo_superpowers_v1` (commit `073af7d`)
- **Data:** 2026-10-02
- **Escopo:** todo o código versionado em `backend/`, `frontend/` e `database/`, mais configuração e README.
- **Verificação executada:** `mvn -o verify` na raiz terminou com sucesso — 91 testes de backend, 0 falhas, 0 erros, 0 ignorados. O build também executa a suíte do frontend (11 arquivos, cerca de 51 casos), que passou.

## Resumo

| Critério | Nota (1–5) |
|---|---|
| 1. Arquitetura e Design | **4** |
| 2. Qualidade do Código | **4** |
| 3. Segurança | **3** |

O projeto é um monolito modular pequeno, bem organizado e bem testado. Os fundamentos de segurança (hash de senha, CSRF, sessão, token de ativação, validação de entrada) estão corretos. A nota de segurança cai porque faltam proteções contra abuso: não há limite de tentativas em nenhum endpoint público, o cadastro permite descobrir se um CPF ou e-mail está cadastrado, e qualquer pessoa pode bloquear para sempre o e-mail ou CPF de um terceiro.

## Verificar com urgência

Estes são os itens que eu trataria antes de expor o sistema a usuários reais, em ordem de prioridade:

1. **Sem limite de tentativas** em login, cadastro e ativação (força bruta de senha e disparo de e-mails em massa).
2. **Bloqueio de e-mail/CPF de terceiros:** um cadastro pendente nunca expira e não há reenvio de ativação.
3. **Enumeração de CPF e e-mail** pela resposta 409 do cadastro e pelo tempo de resposta do login.
4. **Configuração de produção ausente:** cookies `Secure` atrás de proxy, HTTPS/HSTS, CSP e credenciais padrão do banco.
5. **Dados pessoais retidos** na tabela `event_publication` sem limpeza.

Os detalhes de cada um estão na seção 3.

---

## 1. Arquitetura e Design — nota 4

### Pontos fortes

- **Módulos por funcionalidade, com fronteiras verificadas.** O backend é dividido em `usuario`, `cadastro`, `ativacao`, `autenticacao`, `perfil` e `shared` (Spring Modulith). O teste [ModularidadeTest.java](backend/src/test/java/br/com/demo/cadastro/ModularidadeTest.java) quebra o build se um módulo acessar o interior de outro.
- **Encapsulamento real.** Controladores, repositórios e a maioria dos serviços são package-private. Só `UsuarioService`, os DTOs e o evento são públicos.
- **Camadas claras em cada módulo:** controlador → serviço → repositório → entidade. Os controladores são finos e não acessam repositórios.
- **Cadastro desacoplado do e-mail por evento.** [CadastroService.java](backend/src/main/java/br/com/demo/cadastro/cadastro/CadastroService.java) publica `UsuarioCadastrado`; [AtivacaoService.java:38](backend/src/main/java/br/com/demo/cadastro/ativacao/AtivacaoService.java#L38) o consome de forma assíncrona, com o evento persistido em `event_publication`. Uma falha de SMTP não desfaz o cadastro, e isso está coberto por teste.
- **Banco versionado com Flyway**, `ddl-auto: none`, restrições de unicidade e `CHECK` no próprio banco. `open-in-view: false`.
- **Entidades com comportamento** (`Usuario.ativar`, `TokenAtivacao.usar`) e sem setters públicos. `Clock` injetado torna o tempo testável.
- **DTOs separados das entidades** nas respostas (`PerfilResposta`, `CadastroResposta`); o hash da senha não tem getter público.
- **Erros em formato único** (`application/problem+json` com `codigo` e `erros` por campo), consumido de forma uniforme pelo cliente em [cliente.ts](frontend/src/api/cliente.ts).
- **Build Maven multi-módulo** (`database`, `frontend`, `backend`) que gera um único JAR com a SPA embutida.
- **Frontend simples e coerente:** `api/`, `paginas/`, `componentes/`, `validacao/`, com esquemas Zod e formulários reutilizados (`Campo`, `CamposEndereco`).

### Riscos e pontos de atenção

- **Domínio acoplado ao HTTP.** `NegocioException` carrega `HttpStatus`, e a entidade [TokenAtivacao.java](backend/src/main/java/br/com/demo/cadastro/ativacao/TokenAtivacao.java) importa `org.springframework.http.HttpStatus`. A tradução para status HTTP deveria ficar só no `ApiExceptionHandler`.
- **Entidade JPA `Usuario` atravessa módulos.** `autenticacao` e `perfil` recebem a entidade diretamente de `UsuarioService`. Funciona hoje, mas expõe o modelo de persistência; uma projeção ou DTO de leitura isolaria melhor.
- **Login feito à mão no controlador.** [AutenticacaoController.java:40](backend/src/main/java/br/com/demo/cadastro/autenticacao/AutenticacaoController.java#L40) cria o `SecurityContext` e manipula a sessão sem passar por `AuthenticationManager`. Com isso perdem-se os eventos de autenticação do Spring Security e os pontos de extensão (bloqueio de conta, auditoria).
- **Envio de e-mail dentro da transação do listener.** A chamada SMTP ocorre com a transação de banco aberta e a conexão retida. Além disso, um evento que falha só é reprocessado ao reiniciar a aplicação (`republish-outstanding-events-on-restart`); não há reprocessamento agendado.
- **Contrato da API mantido em duplicidade.** Os tipos em [tipos.ts](frontend/src/api/tipos.ts) e as regras de validação (CPF, senha, UF, mensagens) são reescritos à mão no frontend e no backend, sem OpenAPI nem geração de tipos. Podem divergir sem que nenhum teste acuse.
- **Sem estrutura de operação:** não há perfil de produção, Actuator/health check, Dockerfile da aplicação nem pipeline de CI.
- **Frontend sem estado de sessão central.** Cada página descobre o 401 por conta própria. Adequado para seis rotas, mas não escala.

---

## 2. Qualidade do Código — nota 4

### Pontos fortes

- **Código curto e legível.** Classes pequenas, nomes consistentes em português, uso idiomático de records, text blocks e `Optional`. TypeScript em modo `strict` com `noUnusedLocals`/`noUnusedParameters`.
- **Tratamento de exceções centralizado** em [ApiExceptionHandler.java](backend/src/main/java/br/com/demo/cadastro/shared/ApiExceptionHandler.java): validação, JSON malformado e erros de negócio viram respostas previsíveis, com um erro por campo.
- **Casos de borda tratados de propósito:**
  - corrida entre dois cadastros simultâneos, convertida em 409 no campo correto;
  - limite de 72 bytes do BCrypt validado em bytes UTF-8, no cadastro e no login;
  - normalização de e-mail (espaços e caixa);
  - `StrictMode` do React não dispara a ativação duas vezes.
- **Testes em vários níveis:**
  - unitários (`CpfTest`, `SenhaTest`, `TokenAtivacaoTest`, `GeradorTokenTest`, validação Bean Validation);
  - integração com PostgreSQL real (Testcontainers) e SMTP real (GreenMail);
  - um teste de concorrência com bloqueio real no banco ([CadastroConcorrenciaIntegrationTest.java](backend/src/test/java/br/com/demo/cadastro/cadastro/CadastroConcorrenciaIntegrationTest.java));
  - frontend com Vitest, Testing Library e MSW.
- **Testes verificam comportamento de segurança**, não só o caminho feliz: fixação de sessão, CSRF ausente, campos imutáveis ignorados, escape de HTML no e-mail, hash diferente do token, mesma resposta para senha errada e e-mail inexistente.
- **Acessibilidade básica** nos formulários (`label`, `aria-invalid`, `aria-describedby`, `role="alert"`).
- **README completo:** execução, testes, roteiro de verificação manual e limitação conhecida.

### Riscos e pontos de atenção

- **Cobertura não é medida.** Não há JaCoCo nem cobertura do Vitest configurados. A suíte parece ampla, mas não existe número nem limite mínimo no build.
- **Sem lint nem formatador.** Não há ESLint/Prettier no frontend, nem Checkstyle/Spotless/análise estática no backend.
- **Sem teste ponta a ponta em navegador.** A integração real entre a SPA e a API (cookies, CSRF, redirecionamentos) só é coberta pelo roteiro manual do README.
- **Tradução de violação de unicidade por texto.** [UsuarioService.java:71](backend/src/main/java/br/com/demo/cadastro/usuario/UsuarioService.java#L71) procura `uk_usuario_email` na mensagem do driver. O nome da constraint obtido da `ConstraintViolationException` do Hibernate seria mais robusto.
- **JSON montado à mão** em `CsrfInvalidoHandler` e `NaoAutenticadoEntryPoint`, duplicando o formato do `ApiExceptionHandler`.
- **Sem tratador genérico.** Erros 500, 405 e 415 saem no formato padrão do Spring Boot, não em `problem+json` com `codigo`.
- **Respostas inconsistentes para usuário inexistente** em [PerfilController.java](backend/src/main/java/br/com/demo/cadastro/perfil/PerfilController.java): o `GET` devolve 401 e o `PUT` devolve 404.
- **Fragilidade nos testes.** `csrf()` do Spring Security Test altera o filtro compartilhado, o que exigiu `@DirtiesContext` e um contorno em `CadastroIntegrationTest`. Os testes também compartilham o banco sem limpeza (dependem de dados únicos).
- **Validações divergentes entre camadas.** O frontend exige ponto no domínio do e-mail; o `@Email` do backend aceita `a@b`. A data de nascimento só exige estar no passado (o ano 0001 é aceito; não há idade mínima).
- **Detalhes menores:** `CadastroConcluidoPagina` fixa "24 horas" no texto, embora a validade seja configurável; o `select` de UF não liga o erro via `aria-describedby`.

---

## 3. Segurança — nota 3

### O que está correto (itens da sua lista)

| Item | Situação |
|---|---|
| Hash de senha | BCrypt com salt (`BCryptPasswordEncoder`, custo padrão 10). A senha nunca é devolvida nem registrada. O limite de 72 bytes é tratado explicitamente. |
| SQL Injection | Não encontrei ponto vulnerável. Todo acesso usa Spring Data JPA com consultas derivadas e parâmetros. Não há SQL concatenado nem banco NoSQL. |
| Validação de entrada | Bean Validation em todos os corpos de requisição (tamanhos, formatos, CPF com dígito verificador, UF por lista), com limites espelhados nas colunas do banco. |
| XSS | React escapa a saída e não há `dangerouslySetInnerHTML`. O nome é escapado no HTML do e-mail (com teste). |
| Sessão | Sessão de servidor com cookie `HttpOnly` e `SameSite=Lax`, sem JWT. A sessão anterior é invalidada no login (fixação de sessão, com teste). O logout invalida a sessão. |
| CSRF | Ativo em todas as mutações, incluindo login e cadastro, via cookie `XSRF-TOKEN` + cabeçalho. |
| Controle de acesso | `/api/**` exige autenticação por padrão. O perfil usa o id da sessão, sem id na URL, portanto sem acesso a dados de outro usuário. |
| Atribuição em massa | A atualização de perfil só aceita telefone e endereço; campos extras são ignorados (com teste). |
| Token de ativação | 256 bits de `SecureRandom`, guardado apenas como SHA-256, uso único e com expiração. |
| Mensagem de login | Senha errada e e-mail inexistente devolvem a mesma resposta. "Conta pendente" só aparece com a senha correta. |
| Segredos | `.env` está no `.gitignore`; as credenciais de e-mail não têm valor padrão. |

### Riscos identificados

**Alta prioridade**

1. **Nenhum limite de tentativas.** Login, cadastro e ativação aceitam requisições ilimitadas. Consequências:
   - força bruta e *credential stuffing* contra `/api/auth/login`, sem bloqueio de conta nem atraso;
   - `/api/usuarios` pode ser usado para disparar e-mails a endereços arbitrários a partir da sua conta Gmail, e para encher o banco;
   - cada tentativa custa um BCrypt, o que facilita esgotar CPU.
2. **Bloqueio de e-mail e CPF de terceiros.** Qualquer pessoa pode cadastrar o e-mail ou o CPF de outra. O cadastro pendente nunca expira e não há reenvio de ativação, então a vítima fica impedida de se cadastrar para sempre. O README cita a falta de reenvio como limitação, mas não esse abuso. Correção: expirar cadastros pendentes, ou permitir que um novo cadastro substitua um pendente vencido.
3. **Enumeração de CPF e e-mail.**
   - O cadastro responde 409 com `CPF_JA_CADASTRADO` ou `EMAIL_JA_CADASTRADO` ([UsuarioService.java:30](backend/src/main/java/br/com/demo/cadastro/usuario/UsuarioService.java#L30)). Sem limite de tentativas, dá para testar listas de CPFs — dado pessoal sob a LGPD.
   - O login só executa o BCrypt quando o e-mail existe ([AutenticacaoService.java:19](backend/src/main/java/br/com/demo/cadastro/autenticacao/AutenticacaoService.java#L19)). A diferença de tempo revela quais e-mails estão cadastrados, apesar da mensagem idêntica.

**Média prioridade**

4. **Endurecimento para produção ausente.**
   - Não há `server.forward-headers-strategy`. Atrás de um proxy que termina o TLS, a aplicação enxerga HTTP e os cookies de sessão e CSRF saem sem `Secure`.
   - Não há redirecionamento para HTTPS, HSTS nem Content-Security-Policy configurados.
   - O tempo de sessão não é definido (vale o padrão de 30 minutos, sem limite absoluto).
5. **Credenciais padrão do banco.** [application.yml:9](backend/src/main/resources/application.yml#L9) usa `cadastro`/`cadastro` quando as variáveis não existem, e o `docker-compose.yml` publica a porta 5432 em todas as interfaces. É aceitável em desenvolvimento, mas em produção a aplicação deveria falhar ao iniciar sem as variáveis.
6. **Dados pessoais na tabela de eventos.** `event_publication` guarda nome e e-mail em texto no evento serializado, e os registros concluídos nunca são removidos.
7. **Sem troca nem recuperação de senha.** Uma senha comprometida não pode ser trocada pelo usuário, e não há como encerrar as outras sessões.
8. **Sem trilha de auditoria.** Tentativas de login, ativações e alterações de perfil não são registradas, o que impede detectar um ataque em andamento.

**Baixa prioridade**

9. **Política de senha só por composição.** `Senha@123` é aceita. Verificar contra uma lista de senhas vazadas ou comuns traria mais proteção que as regras de caracteres.
10. **Custo do BCrypt no padrão (10)** e codificador fixo. Um `DelegatingPasswordEncoder` permitiria aumentar o custo ou migrar para Argon2 sem invalidar os hashes existentes.
11. **CPF em texto puro** no banco e devolvido sem máscara em `/api/perfil`. Avaliar criptografia da coluna e exibição mascarada.
12. **Token CSRF sem mascaramento.** [SegurancaConfig.java:18](backend/src/main/java/br/com/demo/cadastro/autenticacao/SegurancaConfig.java#L18) usa `CsrfTokenRequestAttributeHandler` em vez da variante XOR, que protege contra ataques do tipo BREACH. Só importa se a compressão HTTP for ativada.
13. **Token de ativação na URL** (`/ativar?token=`). Pode ficar em histórico do navegador e em logs de proxy. O risco é reduzido pelo uso único e pela expiração.
14. **Logout não remove o cookie `XSRF-TOKEN`**, e o status `ATIVO` só é verificado no login, não a cada requisição. Sem efeito hoje, porque não existe desativação de conta.

---

## Recomendações em ordem

1. Adicionar limite de tentativas por IP e por conta em login, cadastro e ativação, com atraso progressivo no login.
2. Expirar cadastros pendentes e implementar o reenvio do e-mail de ativação.
3. Reduzir a enumeração: responder ao cadastro de forma neutra (ou ao menos limitar as tentativas) e igualar o tempo do login executando um BCrypt fictício quando o e-mail não existe.
4. Criar um perfil de produção: cabeçalhos de proxy, cookies `Secure`, HSTS, CSP, tempo de sessão e credenciais de banco obrigatórias.
5. Limpar eventos concluídos de `event_publication` e reprocessar os pendentes de forma agendada.
6. Implementar troca e recuperação de senha e registrar eventos de autenticação.
7. Medir cobertura (JaCoCo e Vitest), adicionar lint e um pipeline de CI que rode `mvn verify`.
8. Tirar `HttpStatus` do domínio e gerar os tipos do frontend a partir de um contrato OpenAPI.
