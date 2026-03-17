#!/usr/bin/env bash
#
# Build and deploy Verdant backend to Cloud Run.
#
# Usage: ./deploy/deploy.sh <PROJECT_ID> <REGION>
# Example: ./deploy/deploy.sh verdant-app europe-north1
#
set -euo pipefail

PROJECT_ID="${1:?Usage: $0 <PROJECT_ID> <REGION>}"
REGION="${2:-europe-north1}"
SERVICE_NAME="verdant-api"
DB_INSTANCE="verdant-staging"
REPO_NAME="verdant"
IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$SERVICE_NAME"

CONNECTION_NAME=$(gcloud sql instances describe "$DB_INSTANCE" --project="$PROJECT_ID" --format='value(connectionName)')

echo "==> Building container image"
cd "$(dirname "$0")/.."
docker build --platform linux/amd64 -t "$IMAGE" .

echo "==> Pushing to Artifact Registry"
docker push "$IMAGE"

echo "==> Deploying to Cloud Run"
gcloud run deploy "$SERVICE_NAME" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --image="$IMAGE" \
  --platform=managed \
  --allow-unauthenticated \
  --port=8080 \
  --memory=512Mi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=3 \
  --add-cloudsql-instances="$CONNECTION_NAME" \
  --set-env-vars="DB_USERNAME=verdant,DB_URL=jdbc:postgresql:///verdant?cloudSqlInstance=$CONNECTION_NAME&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
  --set-secrets="DB_PASSWORD=verdant-db-password:latest,GEMINI_API_KEY=verdant-gemini-key:latest,ADMIN_PASSWORD=verdant-admin-password:latest"

URL=$(gcloud run services describe "$SERVICE_NAME" --project="$PROJECT_ID" --region="$REGION" --format='value(status.url)')

echo ""
echo "=== Deployed ==="
echo "URL: $URL"
echo ""
echo "Update your Android .env.yaml:"
echo "  api-base-url: $URL/"
