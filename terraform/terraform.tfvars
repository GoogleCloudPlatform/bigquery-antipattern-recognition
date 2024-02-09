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

project_id            = "afleisc-pso-sandbox"   // The ID of the Google Cloud Project where all resources will be created
region                = "us-central1"   // The region in which the Artifact Registry, Cloud Run and Cloud Scheduler services will be deployed
repository            = "example"   // The name of the Artifact Registry repository
cloud_run_job_name    = "job"   // The name of the Cloud Run job that will be created
cloud_run_job_name_hash    = "bigquery-antipattern-recognition"   // The name of the Cloud Run job that will be created
apply_scheduler       = false // Determines if a Cloud Scheduler job should be applied, default is false
apply_workflow        = false // Determines if a Cloud Workflow job should be applied on query hashes, default is false
input_table           = ""   // Intermediary name for raw query table used for antipattern tool if using hash workflow
output_table          = "antipattern_output"   // The BigQuery table that will be used for storing the results from the Anti Pattern Detector
scheduler_frequency   = ""   // Schedule frequency for the Cloud Scheduler job, in cron format
bigquery_dataset_name = "optimization_workshop"   // Name of the existing BigQuery dataset where output table will be created
create_output_table   = true // Determines whether the output table is created in the BigQuery Dataset. The default value is true.