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

variable "project_id" {
  type        = string
  description = "Your GCP project ID"
}

variable "region" {
  type        = string
  description = "The region in which to deploy resources"
  default     = "us-central1"
}

variable "service_name" {
  type        = string
  description = "The name of the cloud run service"
  default     = "antipattern-service"
}

variable "artifact_registry_name" {
  type    = string
  default = "bq-remote-functions"
}

variable "bq_dataset" {
  type    = string
  default = "fns"
}