# Deploy to Cloud Run Jobs

## Overview

One option to deploy the BigQuery Anti Pattern Recognition tool is deploying to Cloud Run jobs. Cloud Run jobs can help you easily deploy the tool as a container and later trigger it, either manually or on a schedule.

In order to deploy the tool to Cloud Run Jobs, you'll need to:
* Package the tool into a container and push it to Artifact Registry
* Create the Cloud Run job using the container and your desired configuration
* Trigger the job, either manually or on a schedule

## Deploy using Terraform

Terraform module builds and deploys the BigQuery Antipattern Recognition tool to Cloud Run Jobs. The tool will be configured to, in each execution, perform antipattern recognition of all jobs run during the previous 24 hours and write the results to a BigQuery table. Optionally, it can deploy a Cloud Scheduler cron to run the job on a schedule.

[Click here](./terraform/) to access the Terraform code and for instructions on deploying using Terraform.

To deploy using the **gcloud CLI** follow the instructions below.

## Walkthrough

1. Setup deployment configuration

    ``` bash
    export PROJECT_ID=""  # Project ID where resources are created
    export REGION="us-central1"  # Region for Artifact Registry, Cloud Run and Cloud Scheduler
    export REPOSITORY="bigquery-antipattern-recognition"  # Artifact Registry repository name

    export CONTAINER_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/recognizer:0.1.1-SNAPSHOT"

    export CLOUD_RUN_JOB_NAME="bigquery-antipattern-recognition"  # Name for the Cloud Run job
    export CLOUD_RUN_JOB_SA=""  # Service account associated to the Cloud Run job
    export OUTPUT_TABLE=""  # Ex: "project.dataset.table" BigQuery output table for the Anti Pattern Detector
    ```

2. Create an Artifact Registry Repository, if necessary

    ``` bash
    gcloud artifacts repositories create $REPOSITORY \
        --repository-format=docker \
        --location=$REGION \
        --project=$PROJECT_ID
    ```

3. Build and push the container image to Artifact Registry

    ``` bash
    gcloud auth configure-docker $REGION-docker.pkg.dev

    mvn clean package jib:build \
        -DskipTests \
        -Djib.to.image=$CONTAINER_IMAGE
    ```

4. Create the Cloud Run Job

   This example configuration reads all queries for the previous day from `INFORMATION_SCHEMA`, runs antipattern detection and writes the result to the configured `OUTPUT_TABLE`.

    ``` bash
    gcloud run jobs create $CLOUD_RUN_JOB_NAME \
        --image=$CONTAINER_IMAGE \
        --max-retries=3 \
        --task-timeout=15m \
        --args="--read_from_info_schema" \
        --args="--read_from_info_schema_days" --args="1" \
        --args="--info_schema_table_name" --args="\`region-us\`.INFORMATION_SCHEMA.JOBS" \
        --args="--processing_project_id" --args="$PROJECT_ID" \
        --args="--output_table" --args="$OUTPUT_TABLE" \
        --service-account=$CLOUD_RUN_JOB_SA \
        --region=$REGION \
        --project=$PROJECT_ID
    ```

5. (Optional) Trigger the job manually

    ``` bash
    gcloud run jobs execute $CLOUD_RUN_JOB_NAME \
        --region=$REGION \
        --project=$PROJECT_ID \
        --wait
    ```

6. (Optional) Run the job on a schedule using Cloud Scheduler

    ``` bash
    export CLOUD_RUN_INVOKER_SA=""  # Service account that will invoke the job

    gcloud scheduler jobs create http $CLOUD_RUN_JOB_NAME-trigger \
        --location=$REGION \
        --schedule="0 5 * * *" \
        --uri="https://$REGION-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$PROJECT_ID/jobs/$CLOUD_RUN_JOB_NAME:run" \
        --http-method="POST" \
        --oauth-service-account-email="$CLOUD_RUN_INVOKER_SA"
    ```
