#!/bin/bash

PROJECT_ID="<PROJECT_ID>"
REGION="<REGION_ID>"
ARTIFACT_REGISTRY_NAME="<ARTIFACT_DOCKER_REGISTRY_NAME>"
CLOUD_RUN_SERVICE_NAME="antipattern-service"
BQ_FUNCTION_DATASET="fns"

# Create Docker artifact registry
gcloud artifacts repositories create "${ARTIFACT_REGISTRY_NAME}" \
    --repository-format=docker \
    --location="${REGION}" \
    --description="Docker repository for Bigquery Functions" \
    --project="${PROJECT_ID}"

# Build and push Docker image
gcloud builds submit . \
    --project="${PROJECT_ID}" \
    --config=cloudbuild-udf.yaml \
    --substitutions=_CONTAINER_IMAGE_NAME="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REGISTRY_NAME}/${CLOUD_RUN_SERVICE_NAME}:latest" \
    --machine-type=e2-highcpu-8

# Deploy Cloud Run service
gcloud run deploy ${CLOUD_RUN_SERVICE_NAME} \
    --image="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REGISTRY_NAME}/${CLOUD_RUN_SERVICE_NAME}:latest" \
    --region="${REGION}" \
    --no-allow-unauthenticated \
    --project ${PROJECT_ID}

# Get the Cloud Run service URL
RUN_URL="$(gcloud run services describe ${CLOUD_RUN_SERVICE_NAME} \
    --region ${REGION} \
    --project ${PROJECT_ID} \
    --format="get(status.address.url)")"

# Create BigQuery connection
bq mk --connection \
    --display_name='External antipattern function connection' \
    --connection_type=CLOUD_RESOURCE \
    --project_id="${PROJECT_ID}" \
    --location="${REGION}" \
    ext-${CLOUD_RUN_SERVICE_NAME}

# Get the service account for the connection
CONNECTION_SA="$(bq --project_id ${PROJECT_ID} --format json show --connection ${PROJECT_ID}.${REGION}.ext-${CLOUD_RUN_SERVICE_NAME} | jq '.cloudResource.serviceAccountId')"

# Grant the service account Run Invoker permissions
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${CONNECTION_SA}" \
    --role='roles/run.invoker'

# Create BigQuery dataset for functions
bq mk --dataset \
    --project_id ${PROJECT_ID} \
    --location ${REGION} \
    ${BQ_FUNCTION_DATASET}

# Create BigQuery function
bq query --project_id ${PROJECT_ID} \
    --use_legacy_sql=false \
    "CREATE OR REPLACE FUNCTION ${BQ_FUNCTION_DATASET}.get_antipatterns(query STRING)
    RETURNS JSON
    REMOTE WITH CONNECTION \`${PROJECT_ID}.${REGION}.ext-${CLOUD_RUN_SERVICE_NAME}\`
    OPTIONS (endpoint = '${RUN_URL}');"
