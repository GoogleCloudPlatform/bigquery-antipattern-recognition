#
# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Provider configuration
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.0.0"
    }
  }
}
provider "google" {
  project = var.project_id
  region  = var.region
}

# Artifact Registry repository for the Docker image
resource "google_artifact_registry_repository" "image_registry" {
  location      = var.region
  repository_id = var.artifact_registry_name
  description   = "Docker repository for BigQuery anti-pattern function"
  format        = "DOCKER"
}
resource "google_service_account" "cloud_build_sa" {
  account_id   = "cloud-build-sa"
  display_name = "Cloud Build Service Account"
}

resource "google_project_iam_member" "log_writer_role" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.cloud_build_sa.email}"
}

resource "google_project_iam_member" "object_reader_role" {
  project = var.project_id
  role    = "roles/storage.objectViewer"
  member  = "serviceAccount:${google_service_account.cloud_build_sa.email}"
}
resource "google_artifact_registry_repository_iam_member" "artifact_registry_writer" {
  depends_on = [google_artifact_registry_repository.image_registry]
  project    = var.project_id
  location   = var.region
  repository = var.artifact_registry_name
  role       = "roles/artifactregistry.writer"
  member     = "serviceAccount:${google_service_account.cloud_build_sa.email}"
}

# Build image with `../../cloudbuild-udf.yaml`, uses AntiPatternApplication.java as main class
resource "null_resource" "build_function_image" {
  depends_on = [google_artifact_registry_repository.image_registry]

  triggers = {
    project_id      = var.project_id
    region          = var.region
    full_sa_path = "projects/${var.project_id}/serviceAccounts/${google_service_account.cloud_build_sa.email}"
    full_image_path = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.image_registry.name}/${var.service_name}:latest"
  }

  provisioner "local-exec" {
    when    = create
    command = <<EOF
cd ../../
gcloud builds submit \
--project ${var.project_id} \
--region ${var.region} \
--machine-type=e2-highcpu-8 \
--service-account=${self.triggers.full_sa_path} \
--config=cloudbuild-udf.yaml \
--substitutions=_CONTAINER_IMAGE_NAME=${self.triggers.full_image_path}
EOF
  }

}

# Cloud Run service to host the BigQuery remote function
resource "google_cloud_run_service" "antipattern_service" {
  name    = var.service_name
  project = var.project_id

  location   = var.region
  depends_on = [null_resource.build_function_image]


  template {
    spec {
      containers {
        image = null_resource.build_function_image.triggers.full_image_path
      }
    }
  }
}

# BigQuery connection to the Cloud Run service
resource "google_bigquery_connection" "external_bq_fn_connection" {
  project       = var.project_id
  connection_id = "ext-${var.service_name}"
  location      = var.region
  description   = "External Antipattern function connection"
  cloud_resource {}
}

resource "google_project_iam_binding" "grant_bq_connection_run_invoker_role" {
  project = var.project_id
  role    = "roles/run.invoker"
  members = [
    "serviceAccount:${google_bigquery_connection.external_bq_fn_connection.cloud_resource[0].service_account_id}"
  ]
}

resource "google_bigquery_dataset" "routines_dataset" {
  project    = var.project_id
  location   = var.region
  dataset_id = var.bq_dataset
}

# Creates remote function
resource "null_resource" "antipattern_function" {
  depends_on = [google_cloud_run_service.antipattern_service, google_bigquery_connection.external_bq_fn_connection, google_bigquery_dataset.routines_dataset]

  triggers = {
    project_id         = var.project_id
    region             = var.region
    dataset_id         = var.bq_dataset
    cloud_service_name = google_cloud_run_service.antipattern_service.id
    cloud_run_uri      = google_cloud_run_service.antipattern_service.status[0].url
  }

  provisioner "local-exec" {
    when    = create
    command = <<EOF
bq query --project_id "${self.triggers.project_id}" \
--use_legacy_sql=false \
"CREATE OR REPLACE FUNCTION ${self.triggers.dataset_id}.get_antipatterns(query STRING) RETURNS JSON \
REMOTE WITH CONNECTION \`${self.triggers.project_id}.${self.triggers.region}.${google_bigquery_connection.external_bq_fn_connection.connection_id}\` \
OPTIONS (endpoint = '${self.triggers.cloud_run_uri}');" \
EOF
  }
}