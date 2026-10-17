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
| **user** | ✅ implementado | Cadastro básico: nome, email, senha (BCrypt), descrição |
| **auth** | ⏳ futuro (`modules/auth`) | Login, logout, access/refresh token, rotação |
| **task** | ⏳ futuro (`modules/task`) | Tarefas (listagem/criação) — referencia `userId` |

## API

| Método | Rota | Body | Resposta |
|--------|------|------|----------|
| POST | `/users` | `{name, email, password, description?}` | `201 {id, name, email, description}` |

- Validação: nome 2–100, email válido, senha 6–72, descrição máx 500 (opcional)
- Email duplicado → `409 {error}`
- Senha **nunca** retorna na resposta

## Regras Importantes

- Estrutura interna do módulo **sem frescuras**: entities/dtos/interfaces/enums + Controller/Service/Repositoy
- Constructor injection com `@RequiredArgsConstructor` (nunca `@Autowired` field)
- DTOs = `record` (Java 21); Entidade = class com Lombok `@Getter/@Setter`
- Tabela `users` (plural — `user` é palavra reservada do MySQL)
- Credenciais **somente** no `.env` (gitignorado; `.env.example` é o template)
- `.env` lido via `spring.config.import=optional:file:.env` + `${DB_URL}` etc.

## Comandos Úteis

```powershell
# Windows (PowerShell) — SEMPRE usar mvnw.cmd
.\mvnw.cmd spring-boot:run     # rodar app
.\mvnw.cmd compile             # compilar
.\mvnw.cmd test                # testes
.\mvnw.cmd clean install       # build completo

# Banco (uma vez)
mysql -u root -p -e "CREATE DATABASE user_login_refresh_list_task;"
```

## Decisões Pendentes

- [ ] Criar database MySQL `user_login_refresh_list_task` (se ainda não existe)
- [ ] Testar `POST /users` via curl/Thunder Client
- [ ] Módulo `auth` (login/refresh) em `modules/auth`
- [ ] Módulo `task` em `modules/task`
- [ ] Naming do package raiz: `UserLoginRefreshListTask` tem maiúsculas (viola lowercase) — manter ou renomear
