# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Budget Planner API — a Spring Boot 4.1.1 / Java 17 REST backend, currently in early development. Only auth (login) is implemented so far; the domain models for actual budget planning do not exist yet. Base package: `com.lelisdev.budget_planner_api`.

## Architecture decisions (read before proposing structural changes)

- The data model is **intentionally simple**: `User` authenticates directly, with no roles/permissions/accounts/menus system. An earlier iteration of this project had an enterprise-style RBAC system (`Account`, `Role`, `Menu`, `Permission`, etc., inherited from an unrelated template) — it was deliberately removed because it didn't fit a personal-finance domain. Do not reintroduce a generic roles system without a concrete reason (e.g. shared/collaborative budgets between multiple users).
- IDs are `UUID` (Postgres `gen_random_uuid()`), not `SERIAL`/auto-increment.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never auto-generates schema. Every schema change goes through a new Flyway migration, never edits to an already-applied one.

## Security rules — non-negotiable

- Never commit real credentials (passwords, connection strings with a password, signing secrets) to any file, including `application.properties`. Passwords/secrets are always `${ENV_VAR}` with no default for anything that matters (`DB_PASSWORD`, `JWT_SECRET`) — the app should fail to start rather than run with a weak fallback.
- `application-dev.properties` and `application-prod.properties` are gitignored — do not remove that entry. `application-dev.properties` may hold fixed, non-sensitive local-only values (e.g. a dev-only JWT secret) since it never leaves the machine.
- Flyway migrations (`db/migration/*.sql`) never contain `INSERT`s of user/login data, even with a hashed password. Dev user seeding is done in `config/DevSeedConfig.java`, active only under `@Profile("dev")`, with the password sourced from `DEV_SEED_PASSWORD` (env var, never in code).
- The `flyway-maven-plugin` (with `cleanDisabled=false`) only exists inside the Maven `local-dev` profile — never active by default. Running `flyway:clean` requires `-Plocal-dev` explicitly.
- Login error responses should stay generic (don't let the client distinguish "user not found" from "wrong password") to avoid user enumeration — current `E1000`/`E1001` split is acceptable for now since there are no real users yet, but should be unified before any public-facing use.

## Commands

Use the Maven wrapper (`mvnw`/`mvnw.cmd`), not a system `mvn`.

```
./mvnw spring-boot:run              # run the app (needs DB_PASSWORD env var, see below)
./mvnw test                         # run all tests
./mvnw test -Dtest=ClassName#method # run a single test
./mvnw compile                      # compile only
./mvnw package                      # build the jar
```

Local database migrations (outside of app startup) via the `local-dev` Maven profile, which points Flyway at `jdbc:postgresql://localhost:5432/budget-planner`:

```
./mvnw flyway:migrate -Plocal-dev
./mvnw flyway:clean -Plocal-dev     # wipes the local DB — never available outside this profile
```

### Running locally

- Requires a local PostgreSQL instance with a `budget-planner` database.
- `spring.datasource.password` has no default and must be supplied via the `DB_PASSWORD` env var; `DB_USERNAME` defaults to `postgres`.
- `security.jwt.token.secret` also has no default (`application.properties` maps it to `${JWT_SECRET}`) — the app fails fast at startup (`UserAuthProvider.init()`) if it's missing or blank. `application-dev.properties` sets a fixed local-only value so `dev` profile runs don't need `JWT_SECRET` exported; any other profile/run does.
- Activate the `dev` profile (`-Dspring-boot.run.profiles=dev` or `SPRING_PROFILES_ACTIVE=dev`) to enable SQL logging, the dev user seeder, and the dev JWT secret above.
- The dev seeder (`config/DevSeedConfig`) only runs under the `dev` profile, only when the `users` table is empty, and only if `DEV_SEED_PASSWORD` is set — it creates a user `hendrik` with that password.
- Running tests: `BudgetPlannerApiApplicationTests` (and any test without `@ActiveProfiles("dev")`) needs `JWT_SECRET` set in the environment, since it loads the base profile only.

## Architecture

Standard layered Spring MVC structure under `src/main/java/com/lelisdev/budget_planner_api/`:

- `controllers/` — REST endpoints (currently just `AuthController`, exposing `POST /api/login`).
- `services/` — business logic (`UserService`).
- `repositories/` — Spring Data JPA repositories.
- `models/` — JPA entities (`User`, UUID primary keys generated in Postgres via `gen_random_uuid()`).
- `dtos/` — request/response DTOs, kept separate from entities.
- `mappers/` — MapStruct interfaces (`componentModel = "spring"`) mapping entities <-> DTOs.
- `config/` — Spring configuration: security, CORS, JWT, password encoding, error handling, dev seeding.
- `utils/` — small enums/value types (`Error`, `ValidationResult`).
- `exceptions/` — `AppException`, a `RuntimeException` wrapping an `Error` enum value.

### Auth flow

- `POST /api/login` (`AuthController`) takes a `CredentialsDto` (username + char[] password), delegates to `UserService.login`, which verifies the password with `PasswordEncoder` (BCrypt, configured in `PasswordConfig`) and throws `AppException(Error.E1000/E1001)` on user-not-found / wrong-password.
- On success, `UserAuthProvider.createToken` issues an HMAC256 JWT (via `java-jwt`) with the username as issuer and `firstName`/`lastName`/`id` claims, expiring in 1 hour. The signing secret comes from `security.jwt.token.secret`, which has no default and must be set (see Running locally above) — `UserAuthProvider.init()` fails startup if it's missing.
- `JwtAuthFilter` (registered in `SecurityConfig` before `BasicAuthenticationFilter`) reads the `Authorization: Bearer <token>` header on every request: GET requests get a lightweight `validateToken` (issuer/claims only, no DB check), mutating requests (POST/PUT/DELETE/PATCH) get `validateTokenStrongly` (re-fetches the `User` from the DB, so a deleted/renamed user is rejected even with a still-valid signature). Any `RuntimeException` from either path is caught inside the filter and turned into a JSON error response with the right status (`AppException`'s own `HttpStatus`, or 401 for other JWT errors like expiry/bad signature) — it does not propagate to `RestExceptionHandler`, since that `@ControllerAdvice` only sees exceptions thrown inside the `DispatcherServlet`/controllers, not in filters that run before it. (`RestExceptionHandler.commence()`, the `AuthenticationEntryPoint` implementation, is currently dead code — it's never wired into `SecurityConfig`.)
- Covered by `JwtAuthFilterIntegrationTest`: valid login + authenticated request → 200; token of a since-deleted user on a write request → 401. Note the deleted-user case must use a non-GET request, since GET intentionally skips the DB check by design.
- `SecurityConfig` is stateless (no sessions), CSRF disabled, and only permits `POST /api/login`, `POST /api/register` (endpoint not yet implemented), and `GET /api/health-check` / `GET /api/login/validate` (also not yet implemented) without authentication; everything else requires a valid JWT.
- `WebConfig` sets up CORS allowing `http://localhost:4200` / `https://localhost:4200` (the Angular frontend's dev origin) with credentials.

### Error handling

`RestExceptionHandler` (`@ControllerAdvice`) centralizes exception -> HTTP response mapping. Throw `AppException(Error.SOME_CODE)` from services/controllers for domain errors — the handler unwraps the `Error` enum's message and `HttpStatus` into an `ErrorDto`. Add new domain error codes to `utils/Error.java` rather than throwing raw exceptions.

### Database migrations

Flyway-managed, SQL files in `src/main/resources/db/migration/`, following the `V<n>__description.sql` naming convention. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never auto-generates schema; every schema change must go through a new Flyway migration.

### Profiles

- `application.properties` — base config (Postgres connection, Flyway, Hibernate dialect).
- `application-dev.properties` — SQL logging, Flyway debug logging, dev-only JWT secret.
- `application-prod.properties` — disables `flyway.clean`.

## Next steps / known gaps

- Standardize API responses with an `ApiResponse<T>` envelope + centralized `GlobalExceptionHandler`/`RestExceptionHandler` wiring (currently `RestExceptionHandler.commence()` is unused dead code — decide whether to wire it up or remove it).
- Core business entities not yet created: `Expense`, `Income`, `Category` — all with a direct FK to `user_id`.
- Login error codes (`E1000`/`E1001`) should be unified into one generic "invalid credentials" response before any real users exist, to avoid user enumeration.
