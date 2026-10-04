# Budget Planner API

REST backend for the Budget Planner application. Spring Boot 4 / Java 17 / PostgreSQL.

Version `0.1.0` covers authentication only:

| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/login` | public | Returns the user and a JWT (valid for 1 hour) |
| `GET /actuator/health` | public | Liveness check, returns `{"status":"UP"}` |
| anything else | `Authorization: Bearer <token>` | 401 without a valid token |

## Requirements

- JDK 17 (`JAVA_HOME` must point to a JDK, not a JRE)
- PostgreSQL with a `budget-planner` database

## Configuration

All configuration comes from environment variables; see [`.env.example`](.env.example).

| Variable | Required | Default | Notes |
|---|---|---|---|
| `DB_PASSWORD` | yes | — | The app does not start without it |
| `JWT_SECRET` | yes | — | At least 32 bytes; the app does not start without it |
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/budget-planner` | |
| `DB_USERNAME` | no | `postgres` | |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:4200,https://localhost:4200` | Comma-separated |

The schema is created and upgraded by Flyway when the app starts.

## Running locally

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile reads `src/main/resources/application-dev.properties` (gitignored), which holds a
local-only JWT secret and SQL logging. With `DEV_SEED_PASSWORD` set and an empty `users` table, it
also creates a user `hendrik` with that password.

## Tests

Tests run against a separate `budget-planner-test` database (create it once) and need only
`DB_PASSWORD`:

```
./mvnw test
```

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

- No brute-force protection on `POST /api/login` (planned for the next release).
- No refresh token: the session ends when the 1-hour token expires (planned for the next release).
- A deactivated user's token stays valid on `GET` requests until it expires.
