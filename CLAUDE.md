# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Budget Planner API — a Spring Boot 4.1.1 / Java 17 REST backend, currently in early development. Only auth (login) is implemented so far; the domain models for actual budget planning do not exist yet. Base package: `com.lelisdev.budget_planner_api`.

## Architecture decisions (read before proposing structural changes)

- IDs are `UUID` (Postgres `gen_random_uuid()`), not `SERIAL`/auto-increment.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never auto-generates schema. Every schema change goes through a new Flyway migration, never edits to an already-applied one.

## Security rules — non-negotiable

- Never commit real credentials (passwords, connection strings with a password, signing secrets) to any file, including `application.properties`. Passwords/secrets are always `${ENV_VAR}` with no default for anything that matters (`DB_PASSWORD`, `JWT_SECRET`) — the app should fail to start rather than run with a weak fallback.
- `application-dev.properties` and `application-prod.properties` are gitignored — do not remove that entry. `application-dev.properties` may hold fixed, non-sensitive local-only values (e.g. a dev-only JWT secret) since it never leaves the machine.
- Flyway migrations (`db/migration/*.sql`) never contain `INSERT`s of user/login data, even with a hashed password. Dev user seeding is done in `config/DevSeedConfig.java`, active only under `@Profile("dev")`, with the password sourced from `DEV_SEED_PASSWORD` (env var, never in code).
- The `flyway-maven-plugin` (with `cleanDisabled=false`) is declared in the main build for manual local use only: its URL is fixed to `localhost` and it is bound to no build phase. Don't point it at a non-local database or bind `clean` to a lifecycle phase.
- Login error responses stay generic: unknown user and wrong password both return `E02`, and the password check runs against a dummy BCrypt hash when the user doesn't exist so response time doesn't reveal it either. Don't reintroduce separate codes or an early return.
- Error responses never carry `exception.getMessage()` or other internals — only the fixed message of an `ErrorCode`.

## Commands

Use the Maven wrapper (`mvnw`/`mvnw.cmd`), not a system `mvn`.

```
./mvnw spring-boot:run              # run the app (needs DB_PASSWORD env var, see below)
./mvnw test                         # run all tests
./mvnw test -Dtest=ClassName#method # run a single test
./mvnw compile                      # compile only
./mvnw package                      # build the jar
```

Local database migrations (outside of app startup) via the `flyway-maven-plugin`, which points Flyway at `jdbc:postgresql://localhost:5432/budget-planner` and reads the password from the `DB_PASSWORD` env var:

```
./mvnw flyway:migrate
./mvnw flyway:clean     # wipes the local DB
```

### Running locally

- Requires a local PostgreSQL instance with a `budget-planner` database.
- `spring.datasource.password` has no default and must be supplied via the `DB_PASSWORD` env var; `DB_USERNAME` defaults to `postgres`.
- `security.jwt.token.secret` also has no default (`application.properties` maps it to `${JWT_SECRET}`) — the app fails fast at startup (`UserAuthProvider.init()`) if it's missing, blank, or shorter than 32 bytes (HS256 minimum). `application-dev.properties` sets a fixed local-only value so `dev` profile runs don't need `JWT_SECRET` exported; any other profile/run does.
- Activate the `dev` profile (`-Dspring-boot.run.profiles=dev` or `SPRING_PROFILES_ACTIVE=dev`) to enable SQL logging, the dev user seeder, and the dev JWT secret above.
- The dev seeder (`config/DevSeedConfig`) only runs under the `dev` profile, only when the `users` table is empty, and only if `DEV_SEED_PASSWORD` is set — it creates a user `hendrik` with that password.
- Running tests: every test is `@SpringBootTest` with `@ActiveProfiles("test")`. `src/test/resources/application-test.properties` points them at a separate local database, `budget-planner-test` (overridable with `TEST_DB_URL`), and sets a fixed test-only JWT secret, so the only env var they need is `DB_PASSWORD`. New tests must use the `test` profile too, never `dev`.
- Schema migrations run at app startup through `spring-boot-starter-flyway`. Without that starter (plain `flyway-core`) Spring Boot 4 has no Flyway auto-configuration and the app starts against an unmigrated database.

## Architecture

Standard layered Spring MVC structure under `src/main/java/com/lelisdev/budget_planner_api/`:

- `controllers/` — REST endpoints (currently just `AuthController`, exposing `POST /api/login`).
- `services/` — business logic (`AuthService`).
- `repositories/` — Spring Data JPA repositories.
- `models/` — JPA entities (`User`, UUID primary keys generated in Postgres via `gen_random_uuid()`).
- `dtos/` — request/response DTOs, kept separate from entities.
- `mappers/` — MapStruct interfaces (`componentModel = "spring"`) mapping entities <-> DTOs.
- `config/` — Spring configuration: security, CORS, JWT, password encoding, error handling, dev seeding.
- `enums/` — `ErrorCode` (code + fixed message + `HttpStatus`).
- `exceptions/` — `AppException`, a `RuntimeException` wrapping an `ErrorCode` enum value.

Dependencies are injected with `@Autowired` fields; values needed at startup are computed in `@PostConstruct`.

### Auth flow

- `POST /api/login` (`AuthController`) takes a `@Valid` `CredentialsDto` (username + char[] password, both required) and delegates to `AuthService.login`, which verifies the password with `PasswordEncoder` (BCrypt, configured in `PasswordConfig`) and throws `AppException(ErrorCode.E02)` for both user-not-found and wrong-password.
- On success, `UserAuthProvider.createToken` issues an HMAC256 JWT (via `java-jwt`) with issuer `budget-planner-api` (verified on every token), the username as subject and `name`/`id` claims, expiring in 1 hour. The signing secret comes from `security.jwt.token.secret`, which has no default and must be set (see Running locally above) — `UserAuthProvider.init()` fails startup if it's missing.
- `JwtAuthFilter` (registered in `SecurityConfig` before `BasicAuthenticationFilter`) reads the `Authorization: Bearer <token>` header on every request: GET requests get a lightweight `validateToken` (signature/claims only, no DB check), mutating requests (POST/PUT/DELETE/PATCH) get `validateTokenStrongly` (re-fetches the `User` from the DB, so a deleted/renamed user is rejected even with a still-valid signature). Any `RuntimeException` from either path is caught inside the filter and turned into a JSON error response via `ErrorResponseWriter` (`AppException`'s own `ErrorCode`, or `E03`/401 for other JWT errors like expiry/bad signature) — it does not propagate to `RestExceptionHandler`, since that `@ControllerAdvice` only sees exceptions thrown inside the `DispatcherServlet`/controllers, not in filters that run before it.
- Requests with no token to a protected endpoint get 401 `E05` from `RestExceptionHandler.commence()`, the `AuthenticationEntryPoint` wired into `SecurityConfig`.
- Covered by `JwtAuthFilterIntegrationTest`: valid login + authenticated request → 200; token of a since-deleted user on a write request → 401. Note the deleted-user case must use a non-GET request, since GET intentionally skips the DB check by design.
- `SecurityConfig` is stateless (no sessions), CSRF disabled, and only permits `POST /api/login`, and `GET /actuator/health`  everything else requires a valid JWT.
- `WebConfig` sets up CORS with credentials for the origins in `app.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, comma-separated), defaulting to `http://localhost:4200` / `https://localhost:4200` (the Angular frontend's dev origin).

### Error handling

`RestExceptionHandler` (`@ControllerAdvice`) centralizes exception -> HTTP response mapping. Throw `AppException(ErrorCode.SOME_CODE)` from services/controllers for domain errors — the handler unwraps the `ErrorCode` enum's message and `HttpStatus` into an `ErrorDto` (`message` + `code`). Add new domain error codes to `enums/ErrorCode.java` rather than throwing raw exceptions.

It extends `ResponseEntityExceptionHandler`, so Spring MVC's own errors (invalid body, failed validation, 404, 405, 415) keep their status but are answered as an `ErrorDto` too. Anything unexpected is logged with `log.error` and answered as a generic `E01`. Responses written outside the `DispatcherServlet` (JWT filter, entry point) use `ErrorResponseWriter` to produce the same JSON shape.

### Database migrations

Flyway-managed, SQL files in `src/main/resources/db/migration/`, following the `V<n>__description.sql` naming convention. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never auto-generates schema; every schema change must go through a new Flyway migration.

### Profiles

- `application.properties` — base config (Postgres connection, Flyway, Hibernate dialect, CORS origins, forwarded headers). Everything environment-specific is an env var (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`), so a deployment needs no extra properties file.
- `application-dev.properties` — SQL logging, Flyway debug logging, dev-only JWT secret.
- `application-prod.properties` — disables `flyway.clean`. Gitignored, so it does not reach a deployment; nothing may depend on it.
- `application-test.properties` (in `src/test/resources`) — test database and test-only JWT secret.

## Next steps / known gaps

- Standardize success responses with an `ApiResponse<T>` envelope (error responses are already uniform via `ErrorDto`).
- No way to create a user through the API: outside the `dev` profile the first user is inserted by hand (see README).
- No brute-force protection on `POST /api/login` yet (rate limit / lockout) — planned for the release after `0.1.0`.
- No refresh token yet — planned for the release after `0.1.0`.
- Core business entities not yet created: `Expense`, `Income`, `Category` — all with a direct FK to `user_id`.
