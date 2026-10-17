# Contexto do Projeto - UserLoginRefreshListTask

## Visão Geral

Backend **Spring Boot** de autenticação (login + refresh token) e tarefas (listagem), organizado como **Monólito Modular** seguindo o guia de arquitetura backend.

## Stack

| Item | Versão |
|------|--------|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Build | Maven (wrapper `mvnw` / `mvnw.cmd`) |
| Lombok | sim (annotation processor no pom) |
| Banco de dados | PostgreSQL (previsto — **não configurado ainda**) |

## Arquitetura

**Guia de referência (skill):** `guia-monolito-modular-clean-architecture-e-modulos-desacoplados`

- Monólito Modular + Clean Architecture + Módulos Desacoplados
- Direção das dependências: `presentation → application → domain`
- Infraestrutura **implementa** contratos (ports) definidos pelas camadas internas
- Composição em `bootstrap/` (o caso de uso nunca instancia repositório concreto)
- Comunicação entre módulos: contratos públicos (síncrono), eventos (assíncrono), identificadores

**Regras de código (skill):** `java-coding-rules`

## Estrutura do Projeto

Package raiz: `com.ars.UserLoginRefreshListTask` (mantido — padrão existente)

```
src/main/java/com/ars/UserLoginRefreshListTask/
├── core/             # config, errors, logging, observability (SEM regra de negócio)
├── shared/           # types, utils, primitives, contracts (reutilizável genuíno)
├── infrastructure/   # database, cache, http (implementações técnicas)
├── auth/             # login, logout, tokens, sessões
│   ├── domain/          {entities, value-objects, errors}
│   ├── application/     {use-cases, ports, dto}
│   ├── infrastructure/  {persistence}
│   └── presentation/    {http}
├── user/             # conta, perfil, credenciais
│   ├── domain/          {entities, value-objects, errors}
│   ├── application/     {use-cases, ports, dto}
│   ├── infrastructure/  {persistence}
│   └── presentation/    {http}
├── task/             # tarefas
│   ├── domain/          {entities, value-objects, errors}
│   ├── application/     {use-cases, ports, dto}
│   ├── infrastructure/  {persistence}
│   └── presentation/    {http}
├── bootstrap/        # composição: conecta contratos ↔ implementações
└── UserLoginRefreshListTaskApplication.java   # entry point
```

`src/test/java/...` espelha a estrutura main.

## Módulos

| Módulo | Responsabilidade (guia §6) |
|--------|---------------------------|
| **auth** | Login, logout, access/refresh token, rotação, sessões |
| **user** | Cadastro, perfil, estado da conta, hash de senha |
| **task** | Tarefas (listagem/criação) — referencia `userId`, não a entidade User |

## Regras Importantes

- Módulo **não importa detalhes internos** de outro módulo — só contratos públicos
- Domínio **não depende** de Spring/JPA/HTTP — regras puras em `domain/`
- Repositório: interface (contrato) no módulo + implementação JPA em `infrastructure/`
- Constructor injection com `@RequiredArgsConstructor` (nunca `@Autowired` field)
- DTOs = `record` (Java 21); Entidades = class com Lombok
- Ver guia completa: skill `guia-monolito-modular-clean-architecture-e-modulos-desacoplados`

## Comandos Úteis

```powershell
# Windows (PowerShell) — SEMPRE usar mvnw.cmd
.\mvnw.cmd spring-boot:run     # rodar app
.\mvnw.cmd compile             # compilar
.\mvnw.cmd test                # testes
.\mvnw.cmd clean install       # build completo
```

## Decisões Pendentes

- [ ] Configurar PostgreSQL (dependency + `application.properties` + driver)
- [ ] Definir portas/contratos do módulo `auth` (login/refresh)
- [ ] Naming do package raiz: `UserLoginRefreshListTask` tem maiúsculas (viola lowercase) — decidir se mantém ou renomeia
- [ ] Primeiro caso de uso: `CreateTask`/`ListTasks` ou fluxo de login
