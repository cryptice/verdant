# Verdant Backend

REST API for the Verdant gardening app. Built with Quarkus, Kotlin, and PostgreSQL.

## Tech Stack

- **Runtime**: Quarkus 3.x with Kotlin
- **Database**: PostgreSQL 17, managed with Flyway migrations
- **Auth**: JWT (SmallRye JWT) with Google OAuth sign-in
- **AI**: Gemini API for plant identification
- **Java**: 21

## Prerequisites

- JDK 21
- Docker (for Quarkus Dev Services — auto-starts PostgreSQL)
- `pg_dump` / `psql` (for database backup/restore tasks)

## Configuration

All secrets and environment-specific config live in `../.env.yaml` (project root):

```yaml
backend:
  gemini-api-key: <your-gemini-api-key>
  prod:
    db-username: verdant
    db-password: verdant
    db-url: jdbc:postgresql://localhost:5432/verdant
```

The `prod` database config is used for production builds. In dev mode, Quarkus Dev Services automatically provisions a PostgreSQL container — no manual database setup needed.

## Running

### Development

```bash
./gradlew quarkusDev
```

Starts the backend with hot-reload on `http://localhost:8080`. A PostgreSQL 17 container is started automatically via Dev Services.

### Production

```bash
./gradlew quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar
```

Requires `backend.prod.*` config in `.env.yaml` or equivalent environment variables.

## Database

### Migrations

Schema is managed by Flyway and runs automatically on startup. Migration files are in `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| V1 | Initial schema (users, gardens, beds, plants) |
| V2 | Garden locations, bed boundaries (GPS coordinates) |
| V3 | Plant lifecycle (species, species groups/tags, plant events, frequent comments) |

### Backup

```bash
./gradlew dbBackup
```

Creates a timestamped SQL backup in `db-backups/` (e.g. `verdant_20260314_120550.sql`).

### Restore

```bash
# Restore from the most recent backup
./gradlew dbRestore

# Restore from a specific file
./gradlew dbRestore -PbackupFile=db-backups/verdant_20260314_120550.sql
```

This terminates active connections, drops and recreates the database, then loads the backup.

### Connection resolution

The backup/restore tasks find the database in this order:

1. Explicit Gradle properties: `-PdbUrl=jdbc:postgresql://... -PdbUser=... -PdbPass=...`
2. Auto-detect the running Quarkus Dev Services container (via `docker ps`)
3. Fall back to `.env.yaml` prod config

## API Overview

All endpoints (except auth) require a `Bearer` JWT token in the `Authorization` header.

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/google` | Sign in with Google ID token |
| POST | `/api/auth/admin` | Admin login (email/password) |

### Users
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/me` | Get current user |
| PUT | `/api/users/me` | Update profile |
| DELETE | `/api/users/me` | Delete account |

### Gardens
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/gardens` | List user's gardens |
| GET | `/api/gardens/{id}` | Get garden detail |
| POST | `/api/gardens` | Create garden |
| POST | `/api/gardens/with-layout` | Create garden with beds |
| PUT | `/api/gardens/{id}` | Update garden |
| DELETE | `/api/gardens/{id}` | Delete garden |

### Beds
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/beds` | List all user's beds (with garden name) |
| GET | `/api/gardens/{id}/beds` | List beds in a garden |
| GET | `/api/beds/{id}` | Get bed detail |
| POST | `/api/gardens/{id}/beds` | Create bed |
| PUT | `/api/beds/{id}` | Update bed |
| DELETE | `/api/beds/{id}` | Delete bed |

### Plants
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/plants` | List all user's plants (optional `?status=` filter) |
| GET | `/api/beds/{bedId}/plants` | List plants in a bed |
| GET | `/api/plants/{id}` | Get plant detail |
| POST | `/api/beds/{bedId}/plants` | Create plant |
| PUT | `/api/plants/{id}` | Update plant |
| DELETE | `/api/plants/{id}` | Delete plant |
| GET | `/api/plants/{id}/events` | List plant events |
| POST | `/api/plants/{id}/events` | Add plant event |
| DELETE | `/api/plants/{id}/events/{eventId}` | Delete event |
| POST | `/api/plants/identify` | AI plant identification (image) |

### Species
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/species` | List user's species |
| POST | `/api/species` | Create species |
| PUT | `/api/species/{id}` | Update species |
| DELETE | `/api/species/{id}` | Delete species |
| GET | `/api/species/groups` | List species groups |
| POST | `/api/species/groups` | Create group |
| DELETE | `/api/species/groups/{id}` | Delete group |
| GET | `/api/species/tags` | List species tags |
| POST | `/api/species/tags` | Create tag |
| DELETE | `/api/species/tags/{id}` | Delete tag |

### Other
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard` | Dashboard summary |
| GET | `/api/stats/harvests` | Harvest statistics |
| GET | `/api/comments` | Frequent comments |
| POST | `/api/comments` | Record a comment |
| DELETE | `/api/comments/{id}` | Delete comment |

## Project Structure

```
src/main/kotlin/app/verdant/
  auth/         # JWT token generation, Google token verification
  dto/          # Request/response data classes
  entity/       # Database entities
  repository/   # SQL queries (JDBC)
  resource/     # JAX-RS REST endpoints
  service/      # Business logic, AI integration
src/main/resources/
  db/migration/ # Flyway SQL migrations
  application.properties
  privateKey.pem / publicKey.pem  # JWT signing keys
```
