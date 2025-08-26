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

# Create a service account for Workflows
resource "google_service_account" "cloud_workflow_sa" {
  count = var.apply_workflow == true ? 1 : 0
  account_id   = "workflows-service-account"
  display_name = "Workflows Service Account"
}

// Gives the Workflows service account the "Cloud Run Admin" role, allowing it to manage Cloud Run services.
resource "google_project_iam_member" "cloud_run_service_account_workflow" {
  count = var.apply_workflow == true ? 1 : 0
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.cloud_workflow_sa[0].email}"
}
// Gives the Cloud Worfklows service account the "Cloud Run Invoker" role, allowing it to trigger Cloud Run services.
resource "google_project_iam_member" "cloudrun_invoker_workflow" {
  count = var.apply_workflow == true ? 1 : 0
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.cloud_workflow_sa[0].email}"
}

// Gives the Cloud Worfklows service account the "Bigquery admin" role, allowing it to create BigQuery resources.
resource "google_project_iam_member" "bq_admin_workflow" {
  count = var.apply_workflow == true ? 1 : 0
  project = var.project_id
  role    = "roles/bigquery.admin"
  member  = "serviceAccount:${google_service_account.cloud_workflow_sa[0].email}"
}


// Sets up a Cloud Run Job for query hashes in a BQ table.
resource "google_cloud_run_v2_job" "hash" {
  count = var.apply_workflow == true ? 1 : 0
  name     = var.cloud_run_job_name_hash
  location = var.region
  depends_on = [
    resource.google_project_service.project_service,
    resource.null_resource.build_and_push_docker,
    resource.google_service_account.cloud_run_job_sa
  ]
  template {
    template {
      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.repository}/recognizer:0.1.1-SNAPSHOT"
        args  = ["${var.project_id}", "--input_bq_table", "${var.project_id}.${var.bigquery_dataset_name}.${var.input_table}", "--output_table", "${var.project_id}.${var.bigquery_dataset_name}.${var.output_table}"]
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

// Define and deploys the query hash workflow
resource "google_workflows_workflow" "hash_workflow" {
  count = var.apply_workflow == true ? 1 : 0
  name            = "hash_workflow"
  region          = var.region
  description     = "A sample workflow"
  service_account = google_service_account.cloud_workflow_sa[0].id
  source_contents = templatefile("${path.module}/query_hash_workflow.yaml", {project_id = var.project_id, input_table=var.input_table, output_table=var.output_table, cloud_run_job_name_hash=var.cloud_run_job_name_hash})

  depends_on = [resource.google_project_service.project_service,
    resource.null_resource.build_and_push_docker, resource.google_service_account.cloud_workflow_sa, resource.google_cloud_run_v2_job.hash]
}


