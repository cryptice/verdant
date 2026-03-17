# Verdant

A garden planning and plant lifecycle tracking system with three components: a Quarkus backend, an Android app, and a React admin UI.

## Components

### Backend (`backend/`)

Quarkus + Kotlin REST API with PostgreSQL.

- **Plant lifecycle**: Sow seeds (in beds or portable trays), pot up, plant out, harvest, recover, discard — each as a tracked event
- **Species management**: System-wide and user-created species with Swedish/English names, growing conditions, photos, and seed provider links
- **Garden structure**: Gardens with geo-located beds and boundary polygons
- **Seed inventory**: Track seed batches with collection/expiration dates, auto-decrement on sowing
- **Scheduled tasks**: Recurring garden activities with deadlines and progress tracking
- **AI integration**: Gemini-powered extraction of species info from seed packet photos
- **Storage**: Google Cloud Storage for images
- **Auth**: JWT-based authentication with Google OAuth for the app and email/password for admin

### Android App (`android/`)

Kotlin Android app with Jetpack Compose.

- **My World**: Dashboard with gardens, tray plant summary, and harvest stats
- **Plants**: Species-grouped view with current locations, batch actions (pot up, plant out, harvest, recover, discard) via modal
- **Tasks**: Scheduled activities with species-specific workflows
- **Sowing**: Select species, choose bed or portable tray, auto-creates individual plants
- **Gardens**: Map-based garden/bed creation with boundary drawing, inline editing
- **Seed inventory**: Track and manage seed batches with FAB to add
- **Swedish-first**: All UI strings localized, Swedish names shown as primary

### Admin UI (`admin/`)

React + TypeScript + Tailwind CSS (Notion-inspired design).

- **Species CRUD**: Create, edit, delete species with image uploads, AI extraction, provider linking
- **Users/Gardens**: View and manage registered users and their gardens
- **Providers**: Manage seed providers (Impecta, Florea, Wexthuset, etc.)
- **Import/Export**: JSON export/import of species data including provider info
- **Bundled with backend**: Built into the Quarkus JAR and served as static files at `/`

## Development

### Prerequisites

- JDK 21
- Node.js 22+
- Android Studio (for the Android app)
- Docker (for local PostgreSQL and deployment builds)

### Backend

```bash
cd backend
cp .env.yaml.template .env.yaml  # edit with your keys
./gradlew quarkusDev              # starts on port 8081, auto-provisions PostgreSQL
```

### Admin UI

```bash
cd admin
npm install
npm run dev    # starts on port 5174, proxies /api to backend
```

### Android App

```bash
cd android
cp .env.yaml.template .env.yaml  # set your laptop IP and API keys
```

Open in Android Studio and run on device/emulator.

### Database

```bash
cd backend
./gradlew dbBackup                # backup to db-backups/
./gradlew dbRestore               # restore latest backup
```

## Deployment

Deployed to Google Cloud Run with Cloud SQL (PostgreSQL).

```bash
# One-time setup
./deploy/setup-gcp.sh <PROJECT_ID> <REGION>

# Store secrets
gcloud secrets create verdant-gemini-key --data-file=<(echo -n 'KEY')
gcloud secrets create verdant-admin-password --data-file=<(echo -n 'PASSWORD')

# Configure Docker auth
gcloud auth configure-docker <REGION>-docker.pkg.dev

# Build and deploy (backend + admin UI in one container)
./deploy/deploy.sh <PROJECT_ID> <REGION>
```

## Configuration

Each module has its own `.env.yaml` (not checked in):

- `backend/.env.yaml` — Gemini API key, GCS credentials, admin password, database connection
- `android/.env.yaml` — Backend API URL, Google OAuth client ID, Maps API key
