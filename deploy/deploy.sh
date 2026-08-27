#!/usr/bin/env bash
#
# Deploy Verdant backend via Cloud Build.
#
# Usage: ./deploy/deploy.sh <PROJECT_ID> <REGION> <SQL_CONNECTION_NAME> [MIN_INSTANCES]
#
# Examples:
#   ./deploy/deploy.sh verdant-planner-staging europe-north1 \
#       verdant-planner-staging:europe-north2:verdant-staging
#   ./deploy/deploy.sh verdant-prod europe-north2 \
#       verdant-prod:europe-north2:verdant-prod 1
#
# SQL_CONNECTION_NAME is required. It was previously hardcoded in
# cloudbuild.yaml, so a build submitted against any project deployed that
# project's image wired to the STAGING database.
set -euo pipefail

PROJECT_ID="${1:?Usage: $0 <PROJECT_ID> <REGION> <SQL_CONNECTION_NAME> [MIN_INSTANCES]}"
REGION="${2:?Usage: $0 <PROJECT_ID> <REGION> <SQL_CONNECTION_NAME> [MIN_INSTANCES]}"
SQL_INSTANCE="${3:?Usage: $0 <PROJECT_ID> <REGION> <SQL_CONNECTION_NAME> [MIN_INSTANCES]}"
MIN_INSTANCES="${4:-0}"
GCS_BUCKET="${GCS_BUCKET:-verdant-prod-media}"
SERVICE_NAME="verdant-api"

# The connection name must belong to the project being deployed to, or the
# service comes up talking to another environment's database.
case "$SQL_INSTANCE" in
  "$PROJECT_ID:"*) ;;
  *)
    echo "ERROR: SQL instance '$SQL_INSTANCE' does not belong to project '$PROJECT_ID'." >&2
    echo "       Expected the form $PROJECT_ID:REGION:INSTANCE." >&2
    exit 1
    ;;
esac

cd "$(dirname "$0")/.."

echo "==> Submitting build to Cloud Build"
echo "    project:  $PROJECT_ID"
echo "    region:   $REGION"
echo "    database: $SQL_INSTANCE"
gcloud builds submit \
  --project="$PROJECT_ID" \
  --substitutions="_REGION=$REGION,_SQL_INSTANCE=$SQL_INSTANCE,_MIN_INSTANCES=$MIN_INSTANCES,_GCS_BUCKET=$GCS_BUCKET"

URL=$(gcloud run services describe "$SERVICE_NAME" --project="$PROJECT_ID" --region="$REGION" --format='value(status.url)')

echo ""
echo "=== Deployed ==="
echo "URL: $URL"
