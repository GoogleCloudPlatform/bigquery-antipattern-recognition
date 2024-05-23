#
# Copyright 2023 Google LLC
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
#

## Define variables

variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "artifact_registry_name" {
  type = string
  default = "bq-remote-functions"
}

variable "bq_dataset" {
  type = string
  default = "fns"
}

variable "dlp_deid_template_json_file" {
  type = string
  default = "sample_dlp_deid_config.json"
}

variable "dlp_inspect_template_full_path" {
  default = ""
}

variable "service_name" {
  default = "bq-transform-fns"
}

variable "user_os" {
  type = string
  default = "linux"
  description = "The OS of the person running the Terraform script. Options: [linux, darwin]"
  validation {
    condition = contains(["linux","darwin"], var.user_os)
    error_message = "Supported OS Options: [linux, darwin]"
  }
}