#!/bin/bash

# Create Docker artifact registry
gcloud artifacts repositories create "${ARTIFACT_REGISTRY_NAME}" \
    --repository-format=docker \
    --location="${REGION}" \
    --description="Docker repository for Bigquery Functions" \
    --project="${PROJECT_ID}"

# 1. Create Service Account
gcloud iam service-accounts create cloud-build-sa \
  --display-name "Cloud Build Service Account"

# 2. Grant Logging.logWriter Role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member "serviceAccount:cloud-build-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role "roles/logging.logWriter"

# 3. Grant Storage.objectViewer Role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member "serviceAccount:cloud-build-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role "roles/storage.objectViewer"

# 4. Grant ArtifactRegistry.writer Role
gcloud artifacts repositories add-iam-policy-binding \
  $ARTIFACT_REGISTRY_NAME \
  --location=$REGION \
  --member "serviceAccount:cloud-build-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role "roles/artifactregistry.writer"

# Build and push Docker image
gcloud builds submit . \
    --project=$PROJECT_ID \
    --config=cloudbuild-udf.yaml \
    --service-account=projects/$PROJECT_ID/serviceAccounts/cloud-build-sa@$PROJECT_ID.iam.gserviceaccount.com \
    --substitutions=_CONTAINER_IMAGE_NAME=${REGION}-docker.pkg.dev/$PROJECT_ID/$ARTIFACT_REGISTRY_NAME/$CLOUD_RUN_SERVICE_NAME:latest \
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

# Get the service account for the connection (modified jq query)
CONNECTION_SA=$(bq --project_id ${PROJECT_ID} --format json show --connection ${PROJECT_ID}.${REGION}.ext-${CLOUD_RUN_SERVICE_NAME} | jq -r '.cloudResource.serviceAccountId')

# Remove surrounding double quotes from the service account string
CONNECTION_SA="${CONNECTION_SA%\"}" # Remove trailing quote
CONNECTION_SA="${CONNECTION_SA#\"}" # Remove leading quote

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
