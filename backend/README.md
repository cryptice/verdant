# Verdant Backend

Quarkus + Kotlin REST API for Verdant's web, Android, and admin clients. Uses JDBC repositories, PostgreSQL, Flyway, JWT authentication, Google OAuth, Gemini, and Google Cloud Storage. Requires JDK 21; local Dev Services and tests use PostgreSQL 17, while the documented production instance runs PostgreSQL 18.

## Local development

From `backend/`:

```bash
cp .env.yaml.template .env.yaml
./gradlew quarkusDev
```

The local configuration file is `backend/.env.yaml`, not the repository root. It supplies Gemini/GCS settings and the admin password to Gradle's dev task. Dev Services starts PostgreSQL through Docker. The API listens on `http://localhost:8081` in dev and port 8080 in production.

Dev JWT signing requires locally generated, gitignored `src/main/resources/privateKey.pem` and `publicKey.pem`. On a fresh checkout, generate these before starting the backend:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/privateKey.pem
openssl rsa -pubout -in src/main/resources/privateKey.pem -out src/main/resources/publicKey.pem
```

Set `GOOGLE_CLIENT_ID` to the OAuth web client ID when testing Google sign-in. The browser app also needs the matching `VITE_GOOGLE_CLIENT_ID`.

## Production

Use the root Dockerfile and deployment scripts to bundle the API, web app at `/`, and admin app at `/admin`. A standalone API build is also possible:

```bash
./gradlew quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar
```

Production runtime configuration comes from environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `GOOGLE_CLIENT_ID`, `ADMIN_PASSWORD`, `GEMINI_API_KEY`, and `GCS_BUCKET`. The deployment injects credentials from Secret Manager. A standalone `java -jar` process does not read `.env.yaml`.

## Database

Versioned SQL migrations in [src/main/resources/db/migration](src/main/resources/db/migration/) run automatically at startup. The history includes organizations, workflows, inventory, sales, garden areas, and recurring maintenance. Add new migrations rather than modifying applied ones.

```bash
./gradlew dbBackup
./gradlew dbRestore
./gradlew dbRestore -PbackupFile=db-backups/verdant_20260314_120550.sql
```

Backups are written to `backend/db-backups/`. Restore terminates connections and recreates the selected database. Connection resolution is: explicit `-PdbUrl/-PdbUser/-PdbPass`, a running PostgreSQL 17 Dev Services container, then `.env.yaml` production settings. Use explicit connection properties when multiple databases are running.

## API and authorization

The generated OpenAPI document is available at `/api/openapi`; dev Swagger UI is at `/api/swagger-ui`. Resource classes in `src/main/kotlin/app/verdant/resource/` define the current routes.

- Sign in through `POST /api/auth/google` or `POST /api/auth/admin`.
- Authenticated endpoints require `Authorization: Bearer <JWT>`.
- Organization-scoped endpoints also require `X-Organization-Id`; the user must belong to that organization.
- Account, organization/invitation, and admin routes manage their own scope. Admin routes require the `ADMIN` role.
- `/api/dev/seed` and `/api/dev/wipe` require an admin JWT and exist only in the dev build profile.

## Tests and scheduling

```bash
./gradlew test
# Or, from the repository root:
./scripts/run-tests.sh backend
```

The Docker runner provisions an isolated PostgreSQL database. Cloud Build runs the same backend test task before packaging the image. Tests cover business rules, DTO shapes, and persistence; they do not replace browser end-to-end tests.

Recurring maintenance checks run hourly at minute 30, using the Stockholm calendar date. Pending-task uniqueness is enforced by the database. Cloud Run deployment requires at least one instance and background CPU allocation; see the [root deployment notes](../README.md#release-checks-and-recurring-maintenance).

Weather remains experimental: historical parsing and alert evaluation are intentionally incomplete. A `DONE` backfill status does not guarantee historical observations were collected.

## Structure

`auth/` handles tokens; `filter/` handles organization scope; `resource/` defines HTTP endpoints; `dto/` defines contracts; `service/` holds business logic; `repository/` contains JDBC queries; `entity/` holds persisted models.
