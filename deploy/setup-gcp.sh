#!/usr/bin/env bash
#
# One-time GCP setup for Verdant backend.
# Prerequisites: gcloud CLI installed and authenticated.
#
# Usage: ./deploy/setup-gcp.sh <PROJECT_ID> <REGION> <DB_INSTANCE> [TIER] [STORAGE_TYPE]
#
# Examples:
#   ./deploy/setup-gcp.sh verdant-planner-staging europe-north1 verdant-staging
#   ./deploy/setup-gcp.sh verdant-prod europe-north2 verdant-prod db-f1-micro HDD
#
# DB_INSTANCE is required rather than defaulted: it used to be hardcoded to
# the staging instance, which is not a name you want to inherit by accident
# when pointing this script at a different project.
set -euo pipefail

PROJECT_ID="${1:?Usage: $0 <PROJECT_ID> <REGION> <DB_INSTANCE> [TIER] [STORAGE_TYPE]}"
REGION="${2:?Usage: $0 <PROJECT_ID> <REGION> <DB_INSTANCE> [TIER] [STORAGE_TYPE]}"
DB_INSTANCE="${3:?Usage: $0 <PROJECT_ID> <REGION> <DB_INSTANCE> [TIER] [STORAGE_TYPE]}"
TIER="${4:-db-f1-micro}"
STORAGE_TYPE="${5:-HDD}"
DB_NAME="verdant"
DB_USER="verdant"
REPO_NAME="verdant"

echo "==> Setting project to $PROJECT_ID"
gcloud config set project "$PROJECT_ID"

echo "==> Enabling required APIs"
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com

echo "==> Creating Artifact Registry repository"
gcloud artifacts repositories create "$REPO_NAME" \
  --repository-format=docker \
  --location="$REGION" \
  --description="Verdant container images" \
  2>/dev/null || echo "    (already exists)"

echo "==> Creating Cloud SQL instance (this takes a few minutes)"
gcloud sql instances create "$DB_INSTANCE" \
  --database-version=POSTGRES_17 \
  --tier="$TIER" \
  --region="$REGION" \
  --storage-type="$STORAGE_TYPE" \
  --storage-size=10GB \
  --no-assign-ip \
  2>/dev/null || echo "    (already exists)"

echo "==> Creating database"
gcloud sql databases create "$DB_NAME" \
  --instance="$DB_INSTANCE" \
  2>/dev/null || echo "    (already exists)"

echo "==> Setting database user password"
DB_PASSWORD=$(openssl rand -base64 24)
gcloud sql users set-password "$DB_USER" \
  --instance="$DB_INSTANCE" \
  --password="$DB_PASSWORD" \
  2>/dev/null || \
gcloud sql users create "$DB_USER" \
  --instance="$DB_INSTANCE" \
  --password="$DB_PASSWORD"

echo "==> Storing DB password in Secret Manager"
echo -n "$DB_PASSWORD" | gcloud secrets create verdant-db-password --data-file=- 2>/dev/null || \
echo -n "$DB_PASSWORD" | gcloud secrets versions add verdant-db-password --data-file=-

# JWT_PRIVATE_KEY and JWT_PUBLIC_KEY have no defaults in
# application.properties, so the service will not start without them, and
# the Cloud Run deploy fails outright if any --set-secrets secret is absent.
# Generate what can be generated; the rest are placeholders to be replaced.
ensure_secret() {
  local name="$1" value="$2"
  if gcloud secrets describe "$name" >/dev/null 2>&1; then
    echo "    ($name already exists, leaving it alone)"
  else
    printf '%s' "$value" | gcloud secrets create "$name" --data-file=- --quiet
    echo "    created $name"
  fi
}

echo "==> Creating JWT signing keypair"
if gcloud secrets describe verdant-jwt-private-key >/dev/null 2>&1; then
  echo "    (already exists, leaving it alone)"
else
  KEYDIR=$(mktemp -d)
  trap 'rm -rf "$KEYDIR"' EXIT
  openssl genrsa -out "$KEYDIR/privateKey.pem" 2048 2>/dev/null
  openssl rsa -in "$KEYDIR/privateKey.pem" -pubout -out "$KEYDIR/publicKey.pem" 2>/dev/null
  gcloud secrets create verdant-jwt-private-key --data-file="$KEYDIR/privateKey.pem" --quiet
  gcloud secrets create verdant-jwt-public-key --data-file="$KEYDIR/publicKey.pem" --quiet
  echo "    created verdant-jwt-private-key and verdant-jwt-public-key"
fi

echo "==> Creating admin password"
ensure_secret verdant-admin-password "$(openssl rand -base64 24)"

# Placeholders: the deploy mounts these, so they must exist, but only you can
# supply the real values. Both features stay broken until you do.
echo "==> Creating placeholders for operator-supplied secrets"
ensure_secret verdant-gemini-key "REPLACE_ME"
ensure_secret verdant-google-client-id "REPLACE_ME"

echo "==> Granting Cloud Build permissions"
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
CB_SA="$PROJECT_NUMBER@cloudbuild.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$CB_SA" \
  --role="roles/run.admin" \
  --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$CB_SA" \
  --role="roles/iam.serviceAccountUser" \
  --quiet

echo "==> Granting Cloud Run service account access to secrets"
COMPUTE_SA="$PROJECT_NUMBER-compute@developer.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$COMPUTE_SA" \
  --role="roles/secretmanager.secretAccessor" \
  --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$COMPUTE_SA" \
  --role="roles/cloudsql.client" \
  --quiet

CONNECTION_NAME=$(gcloud sql instances describe "$DB_INSTANCE" --format='value(connectionName)')

echo ""
echo "=== Setup complete ==="
echo ""
echo "Cloud SQL connection: $CONNECTION_NAME"
echo "DB password stored in: verdant-db-password"
echo ""
echo "Admin password:  gcloud secrets versions access latest --secret=verdant-admin-password --project=$PROJECT_ID"
echo ""
echo "Replace the placeholder secrets with real values:"
echo "  printf %s 'YOUR_GEMINI_KEY' | gcloud secrets versions add verdant-gemini-key --data-file=- --project=$PROJECT_ID"
echo "  printf %s 'YOUR_OAUTH_CLIENT_ID' | gcloud secrets versions add verdant-google-client-id --data-file=- --project=$PROJECT_ID"
echo ""
echo "Then deploy (the SQL instance must be named explicitly):"
echo "  ./deploy/deploy.sh $PROJECT_ID $REGION $CONNECTION_NAME"
