# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Budget Planner API — a Spring Boot 4 / Java 17 REST backend, currently in early development. Only auth (login) is implemented so far; the domain models for actual budget planning do not exist yet. Base package: `com.lelisdev.budget_planner_api`.

The app runs in Docker (Postgres + API via `docker-compose.yaml`) and is deployed to a VPS through GitHub Actions (see Deployment below).

## Architecture decisions (read before proposing structural changes)

- IDs are `UUID` (Postgres `gen_random_uuid()`), not `SERIAL`/auto-increment.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never auto-generates schema. Every schema change goes through a new Flyway migration, never edits to an already-applied one.
- Configuration is split by responsibility:
  - `application.properties` declares *what* is configured and reads every environment-specific value from an env var (`${VAR}`).
  - `application-dev.properties` / `application-prod.properties` only hold *behaviour* per environment (logging, actuator detail, Flyway safety). They are committed and must never contain URLs, usernames, passwords or secrets.
  - Values live in a `.env` file that is never committed (one on each machine). `.env.example` lists the keys with no values.
- The same image runs in every environment; only the `.env` changes.

## Security rules — non-negotiable

- Never commit real credentials (passwords, connection strings with a password, signing secrets) to any file — including `application*.properties`, `docker-compose.yaml`, the workflow and the docs.
- Env vars in `application.properties` have **no defaults** (`${DB_PASSWORD}`, not `${DB_PASSWORD:something}`). The app must fail to start rather than run with a wrong or weak fallback.
- `.env` is gitignored and must stay that way. `application-dev.properties` and `application-prod.properties` are **not** gitignored — do not add them back to `.gitignore`.
- `.dockerignore` must keep excluding `.env`, `target/`, `.idea/` and `.git`.
- Flyway migrations (`db/migration/*.sql`) never contain `INSERT`s of user/login data, even with a hashed password. Dev user seeding is done in `config/DevSeedConfig.java`, active only under `@Profile("dev")`, with the password sourced from `DEV_SEED_PASSWORD` (env var, never in code).
- `spring.flyway.clean-disabled=true` stays in `application-prod.properties`.
- Login error responses stay generic: unknown user and wrong password both return `E02`, and the password check runs against a dummy BCrypt hash when the user doesn't exist so response time doesn't reveal it either. Don't reintroduce separate codes or an early return.
- Error responses never carry `exception.getMessage()` or other internals — only the fixed message of an `ErrorCode`.
- The production compose must not publish the Postgres port.
- The container runs as a non-root user (`USER app` in the `Dockerfile`) — keep it that way.

## Commands

Use the Maven wrapper (`mvnw`/`mvnw.cmd`), not a system `mvn`.

```
./mvnw spring-boot:run              # run the app (needs the env vars from .env)
./mvnw test                         # run all tests
./mvnw test -Dtest=ClassName#method # run a single test
./mvnw compile                      # compile only
./mvnw package                      # build the jar
```

Docker:

```
docker compose up -d postgres       # only the database (day-to-day development)
docker compose up -d --build        # database + API built from the Dockerfile
docker compose stop api             # free port 8080 to run the API from the IDE again
docker compose logs -f api          # follow API logs
docker compose down                 # remove containers, keep data
docker compose down -v              # remove containers AND the database volume (wipes data)
```

### Running locally

Two modes, never both at the same time (they compete for port 8080):

1. **Day to day:** `docker compose up -d postgres`, then run the app from the IDE with the `.env` loaded into the run configuration. `DB_URL` in `.env` points to `localhost`.
2. **Full stack in Docker:** stop the IDE run, then `docker compose up -d --build`. The `api` service loads `.env` through `env_file` and overrides only `DB_URL` to use the `postgres` service name as host, because inside a container `localhost` is the container itself.

- Postgres 17, database `budget_planner`, data persisted in the named volume `postgres_data`. Postgres only reads `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` when the volume is empty, so changing them requires `docker compose down -v`.
- `security.jwt.token.secret` maps to `${JWT_SECRET}` — the app fails fast at startup (`UserAuthProvider.init()`) if it's missing, blank, or shorter than 32 bytes (HS256 minimum).
- `SPRING_PROFILES_ACTIVE` comes from `.env` (`dev` locally, `prod` on the VPS).
- The dev seeder (`config/DevSeedConfig`) only runs under the `dev` profile, only when the `users` table is empty, and only if `DEV_SEED_PASSWORD` is set — it creates a user `hendrik` with that password. It reads the value with `System.getenv`, so the variable must be a real environment variable (run configuration or container), not just a property.
- Running tests: every test is `@SpringBootTest` with `@ActiveProfiles("test")`, configured by `src/test/resources/application-test.properties` (separate test database, test-only JWT secret). New tests must use the `test` profile too, never `dev`. The `Dockerfile` builds with `-DskipTests`, so tests must run as a separate CI step.
- Schema migrations run at app startup through `spring-boot-starter-flyway`. Without that starter (plain `flyway-core`) Spring Boot 4 has no Flyway auto-configuration and the app starts against an unmigrated database.

## Docker image

`Dockerfile` is a multi-stage build:

- **Build stage** (`maven:3.9-eclipse-temurin-17-alpine`): copies `pom.xml` first and runs `mvn dependency:go-offline`, then copies `src` and runs `mvn clean package -DskipTests`. Keep this order — it lets Docker cache the dependency layer until `pom.xml` changes.
- **Runtime stage** (`eclipse-temurin:17-jre-alpine`): JRE only, copies just the jar, runs as the unprivileged `app` user.

Pin image versions; never use `latest` for base images.

## Deployment

- `.github/workflows/deploy.yml` runs on every push to `main` (i.e. every merged PR) and on manual `workflow_dispatch`. `concurrency` prevents two deploys from running at once.
- Steps: checkout → log in to GHCR with `GITHUB_TOKEN` → build and push `ghcr.io/dasilvadeveloper/budget-planner-api` tagged `latest` and the commit SHA → SSH into the VPS as the `dedicated` user and run `docker compose pull api && docker compose up -d && docker image prune -f` in `the application folder on the server`.
- Repository secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` (a deploy-only key, never a personal one).
- The VPS holds only the production `docker-compose.yaml` (image from GHCR instead of `build`, no Postgres port, `restart: unless-stopped`) and its own `.env`. No source code lives there.
- Deploy only through PRs into `main`; never push directly to `main`.
- Commits must use the GitHub noreply email, never a personal address.

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
- On success, `UserAuthProvider.createToken` issues an HMAC256 JWT (via `java-jwt`) with issuer `budget-planner-api` (verified on every token), the username as subject and `name`/`id` claims, expiring in 1 hour. The signing secret comes from `security.jwt.token.secret` (`JWT_SECRET`).
- `JwtAuthFilter` (registered in `SecurityConfig` before `BasicAuthenticationFilter`) reads the `Authorization: Bearer <token>` header on every request: GET requests get a lightweight `validateToken` (signature/claims only, no DB check), mutating requests (POST/PUT/DELETE/PATCH) get `validateTokenStrongly` (re-fetches the `User` from the DB, so a deleted/renamed user is rejected even with a still-valid signature). Any `RuntimeException` from either path is caught inside the filter and turned into a JSON error response via `ErrorResponseWriter` (`AppException`'s own `ErrorCode`, or `E03`/401 for other JWT errors like expiry/bad signature) — it does not propagate to `RestExceptionHandler`, since that `@ControllerAdvice` only sees exceptions thrown inside the `DispatcherServlet`/controllers, not in filters that run before it.
- Requests with no token to a protected endpoint get 401 `E05` from `RestExceptionHandler.commence()`, the `AuthenticationEntryPoint` wired into `SecurityConfig`.
- Covered by `JwtAuthFilterIntegrationTest`: valid login + authenticated request → 200; token of a since-deleted user on a write request → 401. Note the deleted-user case must use a non-GET request, since GET intentionally skips the DB check by design.
- `SecurityConfig` is stateless (no sessions), CSRF disabled, and only permits `POST /api/login` and `GET /actuator/health`; everything else requires a valid JWT.
- `WebConfig` sets up CORS with credentials for the origins in `app.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, comma-separated).

### Error handling

`RestExceptionHandler` (`@ControllerAdvice`) centralizes exception -> HTTP response mapping. Throw `AppException(ErrorCode.SOME_CODE)` from services/controllers for domain errors — the handler unwraps the `ErrorCode` enum's message and `HttpStatus` into an `ErrorDto` (`message` + `code`). Add new domain error codes to `enums/ErrorCode.java` rather than throwing raw exceptions.

It extends `ResponseEntityExceptionHandler`, so Spring MVC's own errors (invalid body, failed validation, 404, 405, 415) keep their status but are answered as an `ErrorDto` too. Anything unexpected is logged with `log.error` and answered as a generic `E01`. Responses written outside the `DispatcherServlet` (JWT filter, entry point) use `ErrorResponseWriter` to produce the same JSON shape.

### Database migrations

Flyway-managed, SQL files in `src/main/resources/db/migration/`, following the `V<n>__description.sql` naming convention. Never edit a migration that has already been applied; add a new one.

### Profiles

- `application.properties` — base config. Every environment-specific value is an env var with no default: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.
- `application-dev.properties` — SQL logging (`org.hibernate.SQL=DEBUG`), Flyway logging, `management.endpoint.health.show-details=always`.
- `application-prod.properties` — `spring.flyway.clean-disabled=true`, `INFO` logging, `show-details=never`, no SQL formatting.
- `application-test.properties` (in `src/test/resources`) — test database and test-only JWT secret.

## Next steps / known gaps

- Tests depend on a locally running database; move them to Testcontainers and add a test step to the deploy workflow before the build.
- No HTTPS yet: the API is exposed directly on port 8080. Add a reverse proxy (Nginx/Caddy) with TLS once there is a domain, and stop publishing 8080.
- No automated database backups on the VPS yet (`pg_dump` schedule).
- `spring.jpa.open-in-view` is still enabled by default; set it to `false`.
- Spring Security still logs a generated default password at startup; provide an explicit `UserDetailsService`/configuration so the default user is not created.
- Standardize success responses with an `ApiResponse<T>` envelope (error responses are already uniform via `ErrorDto`).
- No way to create a user through the API: outside the `dev` profile the first user is inserted by hand (see README).
- No brute-force protection on `POST /api/login` yet (rate limit / lockout) — planned for the release after `0.1.0`.
- No refresh token yet — planned for the release after `0.1.0`.
- Core business entities not yet created: `Expense`, `Income`, `Category` — all with a direct FK to `user_id`.
