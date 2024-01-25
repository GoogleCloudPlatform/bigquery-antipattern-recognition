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

// Variable that holds the ID of the GCP project where resources are to be created.
variable "project_id" {
  type        = string
  description = "Project ID where resources are created"
}

// Variable that holds the geographical region for GCP resources like Artifact Registry, Cloud Run, and Cloud Scheduler.
variable "region" {
  type        = string
  description = "Region for Artifact Registry, Cloud Run and Cloud Scheduler"
}

// Variable that holds the name of the Artifact Registry repository.
variable "repository" {
  type        = string
  description = "Artifact Registry repository name"
}

// Variable that holds the name of the Cloud Run job to be created.
variable "cloud_run_job_name" {
  type        = string
  description = "Name for the Cloud Run job"
}

// Variable that holds the service account to be associated with the Cloud Run job.
variable "cloud_run_job_sa" {
  type        = string
  description = "Service account associated to the Cloud Run job"
  default     = "cloud-run-job-sa"
}

// Variable that holds the BigQuery output table name for the Anti Pattern Detector.
variable "output_table" {
  type        = string
  description = "BigQuery output table for the Anti Pattern Detector"
}

// Variable that holds the BigQuery output table name for the Anti Pattern Detector.
variable "input_table" {
  type        = string
  description = "BigQuery output table for the Anti Pattern Detector"
}

// Boolean variable to determine if a Cloud Scheduler job is to be applied.
variable "apply_workflow" {
  type    = bool
  default = false
}

// Boolean variable to determine if a Cloud Scheduler job is to be applied.
variable "apply_scheduler" {
  type    = bool
  default = false
}

// This is a Boolean variable used to determine if an output table should be created in the BigQuery Dataset.
variable "create_output_table" {
  type    = bool
  default = true
}

// Variable that holds the Scheduler frequency for Cloud Scheduler in cron format
variable "scheduler_frequency" {
  type    = string
  default = "0 5 * * *"
}

// Variable that holds the Name of the existing BigQuery dataset
variable "bigquery_dataset_name" {
  type        = string
  description = "The name of the existing BigQuery dataset"
}