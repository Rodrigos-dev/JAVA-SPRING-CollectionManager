# Contexto do Projeto - UserLoginRefreshListTask

## Visão Geral

Backend **Spring Boot** de autenticação (login + refresh token) e tarefas (listagem), organizado como **Monólito Modular** — estrutura simples, sem frescuras.

## Stack

| Item | Versão |
|------|--------|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Build | Maven (wrapper `mvnw` / `mvnw.cmd`) |
| Lombok | sim (annotation processor no pom) |
| Banco de dados | **MySQL** (configurado via `.env` — Spring Data JPA + `mysql-connector-j`) |
| Senhas | BCrypt (`spring-security-crypto`) |
| Validação | `spring-boot-starter-validation` (Bean Validation nos DTOs) |
| JWT | `jjwt` 0.12.6 (access + refresh token) |
| Documentação | **Swagger UI** via `springdoc-openapi-starter-webmvc-ui` **3.1.1** (v3 = Spring Boot 4) |

## Arquitetura

**Simples / sem frescuras (decisão do usuário — 2026-10-17):**

- 4 áreas de topo: `core/`, `infrastructure/`, `shared/`, `modules/`
- Cada módulo: `entities/`, `dtos/`, `interfaces/`, `enums/` + `Controller` + `Service` + `Repositoy`
- Persistência: Spring Data JPA direto no módulo (`Repositoy extends JpaRepository`)
- Senha: BCrypt dentro do `Service` (sem camada de port/abstração)
- Erro de negócio: `ResponseStatusException` no `Service` (sem handler próprio)

**Guia de referência (skill):** `guia-monolito-modular-clean-architecture-e-modulos-desacoplados`
**Regras de código (skill):** `java-coding-rules`

## Estrutura do Projeto

Package raiz: `com.ars.UserLoginRefreshListTask` (mantido — padrão existente)

```
src/main/java/com/ars/UserLoginRefreshListTask/
├── core/                  # config, errors, logging, observability (futuro)
├── infrastructure/        # database, cache, http (futuro)
├── shared/                # types, utils, primitives, contracts (futuro)
├── modules/
│   └── user/              # cadastro de usuário (IMPLEMENTADO)
│       ├── entities/         User.java (@Entity tabela users)
│       ├── dtos/             CreateUserRequest, CreateUserResponse
│       ├── interfaces/       (futuro)
│       ├── enums/            (futuro)
│       ├── UserController.java   POST /users
│       ├── UserService.java      cadastro + BCrypt + 409
│       └── UserRepository.java   extends JpaRepository
└── UserLoginRefreshListTaskApplication.java   # entry point
```

`src/test/java/...` espelha a estrutura main.

## Módulos

| Módulo | Status | Responsabilidade |
|--------|--------|------------------|
| **user** | ✅ implementado | CRUD completo com soft delete, paginação, busca LIKE/query |
| **auth** | ✅ implementado | Login, refresh, JWT (access 1h / refresh 30h), `@PreAuthorize` por endpoint |
| **task** | ⏳ futuro (`modules/task`) | Tarefas (listagem/criação) — referencia `userId` |

## API

| Método | Rota | Body | Resposta |
|--------|------|------|----------|
| POST | `/users` | `{name, email, password, description?}` | `201 {id, name, email, description}` |

- Validação: nome 2–100, email válido, senha 6–72, descrição máx 500 (opcional)
- Email duplicado → `409 {error}`
- Senha **nunca** retorna na resposta

### Endpoints (13 operações)

- **Auth (público):** `POST /auth/login`, `POST /auth/refresh`
- **Users:** `POST /users` (público) · `GET /users`, `GET /users/paged`, `GET /users/like`, `GET /users/like/paged`, `GET /users/query`, `GET /users/query/paged`, `GET /users/{id}`, `PUT /users/{id}`, `PATCH /users/{id}`, `DELETE /users/{id}` (todos com `Authorization: Bearer <accessToken>`)

## Swagger / Documentação (springdoc 3.1.1)

| URL | O que é |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | UI visual (redireciona p/ `/swagger-ui/index.html`) |
| `http://localhost:8080/v3/api-docs` | JSON bruto da doc |
| `http://localhost:8080/v3/api-docs.yaml` | YAML da doc |

- **Arquivos:** `core/OpenApiConfig.java` (info + scheme `bearerAuth`), `SecurityConfig` (`permitAll` em swagger + `POST /users`), `application.properties` (props `springdoc.*`), anotações `@Tag`/`@Operation`/`@ApiResponses` nos controllers
- **Gotcha Authorize:** operação protegida precisa de `security = @SecurityRequirement(name = "bearerAuth")` no `@Operation`, senão o token **não é enviado** → 403
- Endpoint sem token/token inválido retorna **403** (Spring Security sem entry point custom)
- Mapa de estudo completo: `mapas/7 - SWAGER DOCMENTACAO/` (gitignorado)

## Regras Importantes

- Estrutura interna do módulo **sem frescuras**: entities/dtos/interfaces/enums + Controller/Service/Repositoy
- Constructor injection com `@RequiredArgsConstructor` (nunca `@Autowired` field)
- DTOs = `record` (Java 21); Entidade = class com Lombok `@Getter/@Setter`
- Tabela `users` (plural — `user` é palavra reservada do MySQL)
- Credenciais **somente** no `.env` (gitignorado; `.env.example` é o template)
- `.env` lido via `spring.config.import=optional:file:.env` + `${DB_URL}` etc.

## Comandos Úteis

```powershell
# Windows (PowerShell) — SEMPRE usar mvnw.cmd e JAVA_HOME do jdk-22 (Lombok quebra no jdk-27)
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"
.\mvnw.cmd spring-boot:run     # rodar app
.\mvnw.cmd compile             # compilar
.\mvnw.cmd test                # testes
.\mvnw.cmd clean install       # build completo

# Banco (uma vez)
mysql -u root -p -e "CREATE DATABASE user_login_refresh_list_task;"
```

## Decisões Pendentes

- [x] Criar database MySQL `user_login_refresh_list_task`
- [x] Testar `POST /users` via curl/Thunder Client
- [x] Módulo `auth` (login/refresh) em `modules/auth`
- [x] Swagger UI + documentação dos endpoints (2026-10-21)
- [ ] Módulo `task` em `modules/task`
- [ ] Naming do package raiz: `UserLoginRefreshListTask` tem maiúsculas (viola lowercase) — manter ou renomear
