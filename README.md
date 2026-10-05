# Budget Planner API

REST backend for the Budget Planner application. Spring Boot 4 / Java 17 / PostgreSQL 17, packaged with Docker.

Version `0.1.0` covers authentication only:

| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/login` | public | Returns the user and a JWT (valid for 1 hour) |
| `GET /actuator/health` | public | Liveness check, returns `{"status":"UP"}` |
| anything else | `Authorization: Bearer <token>` | 401 without a valid token |

## Requirements

- JDK 17 (`JAVA_HOME` must point to a JDK, not a JRE)
- Docker with the Compose plugin

## Configuration

All environment-specific configuration comes from environment variables, read from a `.env` file
in the project root. Copy [`.env.example`](.env.example) to `.env` and fill it in. `.env` is
gitignored and must never be committed.

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` locally, `prod` in production |
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/budget_planner` |
| `DB_USERNAME` | Database user (also used by the Postgres container to create it) |
| `DB_PASSWORD` | Database password (also used by the Postgres container) |
| `JWT_SECRET` | At least 32 bytes, e.g. `openssl rand -hex 32` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated, e.g. `http://localhost:4200` |
| `DEV_SEED_PASSWORD` | `dev` only: password of the seeded `hendrik` user |

DEV_SEED_PASSWORD is optional (dev only). All the others are required and have no default: the app does not start if one is missing.

The schema is created and upgraded by Flyway when the app starts.

## Running locally

### Database in Docker, API from the IDE (day to day)

```
docker compose up -d postgres
```

Then run `BudgetPlannerApiApplication` from the IDE with `.env` loaded into the run configuration
(or `./mvnw spring-boot:run` with the variables exported in the shell). `DB_URL` points to
`localhost:5432`.

With the `dev` profile and `DEV_SEED_PASSWORD` set, an empty `users` table gets a user `hendrik`
with that password.

### Everything in Docker

```
docker compose up -d --build
```

Builds the API image from the `Dockerfile` and starts it next to the database. Inside Docker the
API reaches the database through the `postgres` service name, so the compose file overrides
`DB_URL` for the `api` service. Stop any API running from the IDE first, both use port 8080.

### Useful commands

```
docker compose ps                  # container status
docker compose logs -f api         # follow API logs
docker compose stop api            # stop the API, keep the database running
docker compose down                # remove containers, keep data
docker compose down -v             # remove containers and the database volume (wipes data)
```

Connect to the database with any client on `localhost:5432`, or:

```
docker exec -it budget-planner-db psql -U <DB_USERNAME> -d budget_planner
```

## Tests

```
./mvnw test
```

Tests use the `test` profile (`src/test/resources/application-test.properties`) and a separate
test database, which must exist in the running Postgres container.

## Deployment

Every merge into `main` triggers `.github/workflows/deploy.yml`:

1. Builds the image from the `Dockerfile`.
2. Pushes it to `ghcr.io/dasilvadeveloper/budget-planner-api`, tagged `latest` and with the commit SHA.
3. Connects to the server over SSH and runs `docker compose pull` and `docker compose up -d`.

The server only holds a production compose file and its own `.env`; no source code is deployed.

## Creating the first user

There is no registration endpoint yet. Outside the `dev` profile the first user is inserted by hand,
after the app has started once and created the schema:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (name, username, email, password_hash)
VALUES ('Full Name', 'username', 'user@example.com', crypt('the-password', gen_salt('bf', 10)));
```

`crypt(..., gen_salt('bf', 10))` produces a BCrypt hash compatible with the one the app verifies.
The password is part of the statement, so run it in a session whose statements are not logged and
clear the `psql` history afterwards.

## Error responses

Every error has the same shape: `{"message": "...", "code": "E0x"}`.

| Code | Status | Meaning |
|---|---|---|
| `E01` | 500 | Unexpected error |
| `E02` | 401 | Wrong username or password |
| `E03` | 401 | Invalid or expired token |
| `E04` | 400 | Invalid request |
| `E05` | 401 | Authentication required |
| `E06` | 404 | Resource not found |
| `E07` | 405 | Method not allowed |
| `E08` | 415 | Unsupported media type |
| `E09` | 403 | Access denied |

## Known limitations in 0.1.0

- No HTTPS yet: served over plain HTTP until a reverse proxy with TLS is in place.
- No brute-force protection on `POST /api/login` (planned for the next release).
- No refresh token: the session ends when the 1-hour token expires (planned for the next release).
- A deactivated user's token stays valid on `GET` requests until it expires.
