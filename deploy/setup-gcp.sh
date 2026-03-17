#!/usr/bin/env bash
#
# One-time GCP setup for Verdant backend.
# Prerequisites: gcloud CLI installed and authenticated.
#
# Usage: ./deploy/setup-gcp.sh <PROJECT_ID> <REGION>
# Example: ./deploy/setup-gcp.sh verdant-app europe-north1
#
set -euo pipefail

PROJECT_ID="${1:?Usage: $0 <PROJECT_ID> <REGION>}"
REGION="${2:-europe-north1}"
SERVICE_NAME="verdant-api"
DB_INSTANCE="verdant-staging"
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
  --tier=db-f1-micro \
  --region="$REGION" \
  --storage-type=HDD \
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

echo "==> Storing secrets in Secret Manager"
echo -n "$DB_PASSWORD" | gcloud secrets create verdant-db-password --data-file=- 2>/dev/null || \
echo -n "$DB_PASSWORD" | gcloud secrets versions add verdant-db-password --data-file=-

# Prompt for other secrets
echo ""
echo "Store additional secrets manually:"
echo "  gcloud secrets create verdant-gemini-key --data-file=<(echo -n 'YOUR_KEY')"
echo "  gcloud secrets create verdant-jwt-private-key --data-file=backend/src/main/resources/privateKey.pem"
echo "  gcloud secrets create verdant-jwt-public-key --data-file=backend/src/main/resources/publicKey.pem"
echo ""

CONNECTION_NAME=$(gcloud sql instances describe "$DB_INSTANCE" --format='value(connectionName)')

echo ""
echo "=== Setup complete ==="
echo ""
echo "Cloud SQL connection name: $CONNECTION_NAME"
echo "DB password stored in Secret Manager: verdant-db-password"
echo ""
echo "Next: run ./deploy/deploy.sh $PROJECT_ID $REGION"
