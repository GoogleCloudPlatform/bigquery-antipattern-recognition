/**
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Declares the variable "services" which lists the Google Cloud services to be enabled.
variable "services" {
  type    = list(string)
  default = ["artifactregistry.googleapis.com", "cloudbuild.googleapis.com", "run.googleapis.com", "cloudscheduler.googleapis.com", "iam.googleapis.com"]
}

// Enables the Google Cloud services listed in the "services" variable for the project specified.
resource "google_project_service" "project_service" {
  for_each           = { for service in var.services : service => service }
  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

// Creates a service account for Google Cloud Scheduler.
resource "google_service_account" "cloud_scheduler_sa" {
  depends_on = [
    resource.google_project_service.project_service
  ]
  account_id   = "cloud-scheduler-sa"
  display_name = "Cloud Scheduler service account"
  project      = var.project_id
}

// Gives the Cloud Scheduler service account the "Cloud Run Invoker" role, allowing it to trigger Cloud Run services.
resource "google_project_iam_member" "cloudrun_invoker" {
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.cloud_scheduler_sa.email}"
}

// Creates a service account for Google Cloud Run Jobs.
resource "google_service_account" "cloud_run_job_sa" {
  depends_on = [
    resource.google_project_service.project_service
  ]
  account_id   = var.cloud_run_job_sa
  display_name = "Cloud Run Job service account"
  project      = var.project_id
}

// Gives the Cloud Run Job service account the "Cloud Run Admin" role, allowing it to manage Cloud Run services.
resource "google_project_iam_member" "cloud_run_service_account" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.cloud_run_job_sa.email}"
}

// Gives the Cloud Run Job service account the "Cloud Run Invoker" role, allowing it to trigger Cloud Run services.
resource "google_project_iam_member" "cloud_run_invoker" {
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.cloud_run_job_sa.email}"
}

// Gives the Cloud Run Job service account "Bigquery Admin" role, allowing it to read data from BigQuery and insert output into BigQuery.
resource "google_project_iam_member" "bigquery_admin" {
  project = var.project_id
  role    = "roles/bigquery.admin"
  member  = "serviceAccount:${google_service_account.cloud_run_job_sa.email}"
}

// Creates a output Table in existing BigQuery Dataset
resource "google_bigquery_table" "bq_table" {
  count = var.create_output_table == true ? 1 : 0
  depends_on = [
    google_project_iam_member.bigquery_admin
  ]
  dataset_id          = var.bigquery_dataset_name
  table_id            = var.output_table
  deletion_protection = false

  schema = <<EOF
[
    {
        "name": "job_id",
        "type": "STRING",
        "mode": "NULLABLE"
    },
    {
        "name": "query",
        "type": "STRING",
        "mode": "NULLABLE"
    },
    {
        "name": "recommendation",
        "type": "RECORD",
        "mode": "REPEATED",
        "fields": [
            {
                "name": "name",
                "type": "STRING",
                "mode": "NULLABLE"
            },
            {
                "name": "description",
                "type": "STRING",
                "mode": "NULLABLE"
            }
        ]
    },
    {
        "name": "slot_hours",
        "type": "FLOAT",
        "mode": "NULLABLE"
    },
    {
        "name": "process_timestamp",
        "type": "TIMESTAMP",
        "mode": "NULLABLE"
    }
]
EOF

}

// Sets up an Artifact Registry repository to store Docker images.
module "docker_artifact_registry" {
  source = "github.com/GoogleCloudPlatform/cloud-foundation-fabric/modules/artifact-registry"
  depends_on = [
    resource.google_project_service.project_service
  ]
  project_id = var.project_id
  location   = var.region
  format     = "DOCKER"
  id         = var.repository
}

// Builds and pushes a Docker image to the Artifact Registry repository.
resource "null_resource" "build_and_push_docker" {
  # Uncomment below lines to re-run build
  #    triggers = {
  #     always_run = "${timestamp()}"
  #     }
  depends_on = [
    module.docker_artifact_registry,
    resource.google_project_service.project_service
  ]
  provisioner "local-exec" {
    command = <<-EOF
        gcloud config set project ${var.project_id}
        gcloud builds submit .. --config cloudbuild.yaml --substitutions=_REGION=${var.region},_PROJECT_ID=${var.project_id},_REPOSITORY=${var.repository}
    EOF
  }
}

// Sets up a Cloud Run Job.
resource "google_cloud_run_v2_job" "default" {
  name     = var.cloud_run_job_name
  location = var.region
  depends_on = [
    resource.google_project_service.project_service,
    resource.null_resource.build_and_push_docker
  ]
  template {
    template {
      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.repository}/recognizer:0.1.1-SNAPSHOT"
        args  = ["--read_from_info_schema", "--read_from_info_schema_days", "1", "--processing_project_id", "${var.project_id}", "--output_table", "${var.project_id}.${var.bigquery_dataset_name}.${var.output_table}"]
      }
      max_retries     = 3
      timeout         = "900s"
      service_account = google_service_account.cloud_run_job_sa.email
    }
  }

  lifecycle {
    ignore_changes = [
      launch_stage,
    ]
  }
}

// Sets up a Cloud Scheduler Job to regularly trigger the Cloud Run Job.
resource "google_cloud_scheduler_job" "job" {
  count = var.apply_scheduler == true ? 1 : 0
  depends_on = [
    resource.google_project_service.project_service,
    resource.google_cloud_run_v2_job.default
  ]
  name     = var.cloud_run_job_name
  schedule = var.scheduler_frequency
  http_target {
    http_method = "POST"
    uri         = "https://${var.region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.project_id}/jobs/${var.cloud_run_job_name}:run"
    oauth_token {
      service_account_email = google_service_account.cloud_scheduler_sa.email
    }
  }
  retry_config {
    max_backoff_duration = "3600s"
    max_doublings        = 5
    max_retry_duration   = "0s"
    min_backoff_duration = "5s"
    retry_count          = 0
  }
}