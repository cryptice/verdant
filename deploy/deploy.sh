#!/usr/bin/env bash
#
# Deploy Verdant backend via Cloud Build.
#
# Usage: ./deploy/deploy.sh <PROJECT_ID> <REGION>
# Example: ./deploy/deploy.sh verdant-planner-staging europe-north1
#
set -euo pipefail

PROJECT_ID="${1:?Usage: $0 <PROJECT_ID> <REGION>}"
REGION="${2:-europe-north1}"
SERVICE_NAME="verdant-api"

cd "$(dirname "$0")/.."

echo "==> Submitting build to Cloud Build"
gcloud builds submit \
  --project="$PROJECT_ID" \
  --substitutions="_REGION=$REGION"

URL=$(gcloud run services describe "$SERVICE_NAME" --project="$PROJECT_ID" --region="$REGION" --format='value(status.url)')

echo ""
echo "=== Deployed ==="
echo "URL: $URL"
