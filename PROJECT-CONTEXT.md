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
│   ├── user/                # cadastro de usuário (IMPLEMENTADO)
│   │   ├── entities/         User.java (@Entity tabela users, 1 -> N collections)
│   │   ├── dtos/             CreateUserDto, UpdateUserDto, PatchUserDto, UserResponse
│   │   ├── interfaces/       (futuro)
│   │   ├── enums/            UserRoleEnum
│   │   ├── UserController.java   CRUD /users (10 rotas)
│   │   ├── UserService.java      CRUD + BCrypt + soft delete + 409
│   │   └── UserRepository.java   extends JpaRepository
│   ├── collection/          # coleções do usuário (IMPLEMENTADO)
│   │   ├── entities/         Collection.java (@Entity tabela collections, N -> 1 user, 1 -> N items)
│   │   ├── dtos/             CreateCollectionDto, UpdateCollectionDto, PatchCollectionDto, CollectionResponse
│   │   ├── interfaces/       (futuro)
│   │   ├── enums/            (futuro)
│   │   ├── CollectionController.java   CRUD /collections (9 rotas)
│   │   ├── CollectionService.java      CRUD + owner do JWT + soft delete + 409
│   │   └── CollectionRepository.java   extends JpaRepository (scoped por owner)
│   └── collectionitem/       # itens das coleções (IMPLEMENTADO)
│       ├── entities/         CollectionItem.java (@Entity tabela collection_items, N -> 1 collection)
│       ├── dtos/             CreateCollectionItemDto, UpdateCollectionItemDto, PatchCollectionItemDto, CollectionItemResponse
│       ├── CollectionItemController.java   CRUD /collection-items (9 rotas)
│       ├── CollectionItemService.java      CRUD + dono herdado da collection + soft delete + 409
│       └── CollectionItemRepository.java   extends JpaRepository (scoped por owner da collection)
└── UserLoginRefreshListTaskApplication.java   # entry point
```

`src/test/java/...` espelha a estrutura main.

## Módulos

| Módulo | Status | Responsabilidade |
|--------|--------|------------------|
| **user** | ✅ implementado | CRUD completo com soft delete, paginação, busca LIKE/query |
| **auth** | ✅ implementado | Login, refresh, JWT (access 1h / refresh 30h), `@PreAuthorize` por endpoint |
| **collection** | ✅ implementado | Coleções do usuário autenticado — `user (1) → collections (N)`, dono vem do JWT |
| **collectionitem** | ✅ implementado | Itens das coleções — `collections (1) → collection_items (N)`, dono herdado da collection |
| **task** | ⏳ futuro (`modules/task`) | Tarefas (listagem/criação) — referencia `userId` |

### Relacionamento user → collection

- Tabela `collections` com FK `user_id` → `users(id)` (`@ManyToOne owner` no lado N, `@OneToMany(mappedBy = "owner")` no lado 1)
- Tabela criada **automaticamente** pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`) — sem migration
- `code` é **unique global** → `409` em duplicidade (mesma regra do `email` em users)
- Todo acesso é **escopado ao usuário do token**: listar/busca só retorna as próprias coleções; id de outro usuário → `404` (sem vazamento)

### Relacionamento collection → collection_item

- Tabela `collection_items` com FK `collection_id` → `collections(id)` (`@ManyToOne collection` no lado N, `@OneToMany(mappedBy = "collection")` no lado 1)
- **Sem `user_id` próprio** — o dono do item é o `owner` da collection (evita duplicidade de dado)
- `code` é único **por collection** (não global) → `409` em duplicidade na mesma coleção
- Todo acesso é escopado ao usuário do token: collection informada/listada que não é dele → `404`
- Tabela criada automaticamente pelo Hibernate (`ddl-auto=update`) — sem migration

## API

| Método | Rota | Body | Resposta |
|--------|------|------|----------|
| POST | `/users` | `{name, email, password, description?}` | `201 {id, name, email, description}` |

- Validação: nome 2–100, email válido, senha 6–72, descrição máx 500 (opcional)
- Email duplicado → `409 {error}`
- Senha **nunca** retorna na resposta

### Endpoints (31 operações)

- **Auth (público):** `POST /auth/login`, `POST /auth/refresh`
- **Users:** `POST /users` (público) · `GET /users`, `GET /users/paged`, `GET /users/like`, `GET /users/like/paged`, `GET /users/query`, `GET /users/query/paged`, `GET /users/{id}`, `PUT /users/{id}`, `PATCH /users/{id}`, `DELETE /users/{id}` (todos com `Authorization: Bearer <accessToken>`)
- **Collections (todos com token):** `POST /collections`, `GET /collections`, `GET /collections/paged`, `GET /collections/like`, `GET /collections/like/paged`, `GET /collections/{id}`, `PUT /collections/{id}`, `PATCH /collections/{id}`, `DELETE /collections/{id}`
- **Collection Items (todos com token):** `POST /collection-items`, `GET /collection-items` (`?collectionId=` opcional), `GET /collection-items/paged`, `GET /collection-items/like`, `GET /collection-items/like/paged`, `GET /collection-items/{id}`, `PUT /collection-items/{id}`, `PATCH /collection-items/{id}`, `DELETE /collection-items/{id}`

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
- [x] Módulo `collection` (tabela `collections`, user 1 → N) — 2026-10-24
- [ ] Módulo `task` em `modules/task`
- [ ] Naming do package raiz: `UserLoginRefreshListTask` tem maiúsculas (viola lowercase) — manter ou renomear
