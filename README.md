# ShopKart Backend

Spring Boot REST API for ShopKart, an e-commerce demo app. Pairs with the [ShopKart-UI](https://github.com/rabinarayanpadhy07/ShopKart-UI) React frontend. Cookie-based JWT auth, product catalog, cart, orders, wishlist, reviews, addresses, and Razorpay checkout.

**Stack:** Java 17, Spring Boot 4.1, Spring Security, Spring Data JPA, MySQL 8, Maven (wrapper included, no local Maven install needed).

## Prerequisites

- JDK 17+
- Either **Docker** (recommended - runs MySQL for you) or a local **MySQL 8** server

## Quick start (Docker Compose - easiest)

This runs both the database and the backend for you.

```bash
cp .env.example .env
# edit .env: set MYSQL_ROOT_PASSWORD and SEED_ADMIN_PASSWORD at minimum
docker compose up --build
```

The API is now at `http://localhost:9090` - check `http://localhost:9090/api/health`. With `SEED_DEMO_DATA=true` and `SEED_ADMIN=true` (both on by default in `.env.example`), you'll have a full product catalog and an admin login (`SEED_ADMIN_USERNAME` / `SEED_ADMIN_PASSWORD`) as soon as it starts.

Now start the [frontend](https://github.com/rabinarayanpadhy07/ShopKart-UI) - it expects the API here on port 9090.

## Quick start (without Docker)

You'll need a MySQL 8 server running locally.

```sql
CREATE DATABASE salessavvy;
```

```bash
cp .env.example .env
# edit .env: set DB_USERNAME/DB_PASSWORD to your local MySQL credentials,
# and SEED_ADMIN_PASSWORD

./mvnw spring-boot:run      # bash / macOS / Linux / Git Bash
.\mvnw.cmd spring-boot:run  # Windows PowerShell / cmd
```

The app loads `.env` from the project root automatically on startup (via `spring.config.import` in `application.properties`) - no need to export anything into your shell first, and this also works when launching from an IDE's Run button. It's a no-op if `.env` doesn't exist, so it never affects Docker or a real production deployment where env vars are set directly.

Same result: API at `http://localhost:9090`.

## Configuration

All configuration is via environment variables (see `.env.example` for the full list with explanations). The important ones:

| Variable | Local default | Notes |
|---|---|---|
| `DDL_AUTO` | `update` | Auto-creates tables from the JPA entities on a fresh database. **Must be `validate` or `none` in production** (enforced at startup - see below). |
| `AUTH_COOKIE_SECURE` / `AUTH_COOKIE_SAME_SITE` | `false` / `Lax` | Correct for plain `http://localhost`. Production behind HTTPS needs `true` / `None`. |
| `SEED_DEMO_DATA` / `SEED_ADMIN` | `true` | Populates products and creates an admin account on startup. **Must be `false` in production.** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5174` | Must match wherever the frontend is served from. |
| `JWT_SECRET` | (dev default) | Must be ≥ 64 bytes if you override it. Production must not use the `dev-only...` default. |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | - | From a free Razorpay test account. Checkout fails without these. |
| `GOOGLE_CLIENT_ID` | - | From a Google Cloud OAuth client. Google Sign-In is disabled without it. |

### Production

Setting `APP_PRODUCTION=true` turns on `ProductionConfigurationValidator`, which **refuses to start** unless:
- `JWT_SECRET`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `GOOGLE_CLIENT_ID` are all set (and `JWT_SECRET` isn't the dev default)
- `AUTH_COOKIE_SECURE=true` and `AUTH_COOKIE_SAME_SITE=None` (required for a cross-site frontend over HTTPS)
- `DDL_AUTO` is `validate` or `none` (never auto-migrate a production schema)
- `SEED_DEMO_DATA`, `SEED_ADMIN`, and `DB_PATCH_ENABLED` are all `false`

For a production database, create the schema once from a `DDL_AUTO=update` run against a scratch database (or from the JPA entities directly), then run `src/main/resources/schema-optimization.sql` to add the recommended indexes before switching that environment to `DDL_AUTO=validate`.

## Running tests

```bash
./mvnw test          # bash
.\mvnw.cmd test       # Windows
```

Tests run against an in-memory H2 database - no MySQL needed.

## Troubleshooting

- **`Access denied for user 'root'@'localhost' (using password: NO)`**: `.env` isn't being picked up - most likely it doesn't exist yet (`cp .env.example .env` first) or you're running the jar/IDE config from a different working directory than the project root, so it can't find `.env` there. Confirm the log line "using password: NO" changes to "YES" once `.env` exists with `DB_PASSWORD` set.
- **Login/session doesn't persist locally**: usually a cookie mismatch. Over plain `http://localhost`, `AUTH_COOKIE_SECURE` must be `false` and `AUTH_COOKIE_SAME_SITE` must be `Lax` (browsers drop `SameSite=None` cookies that aren't `Secure`).
- **CORS errors in the browser console**: `CORS_ALLOWED_ORIGINS` must exactly match the frontend's origin (scheme + host + port).
- **`Table 'salessavvy.users' doesn't exist` on startup**: your database is empty and `DDL_AUTO` is `validate`. Set `DDL_AUTO=update` for a first run against a fresh database.
- **Checkout/Google Sign-In silently fail**: `RAZORPAY_KEY_ID`/`RAZORPAY_KEY_SECRET`/`GOOGLE_CLIENT_ID` aren't set - these features are optional for local dev but need real (test-mode) credentials to work.
