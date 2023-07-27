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

// Specifies the minimum version of Terraform that can be used with this configuration, 
// and declares the Google provider and its version.
terraform {
  required_version = ">= 1.3.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.60.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = ">= 4.60.0"
    }
  }
}

// Provider configuration block for the Google provider.
// Sets the region and project to be used with the Google provider.
provider "google" {
  region  = var.region
  project = var.project_id
}

// Provider configuration block for the Google Beta provider.
// Sets the region and project to be used with the Google Beta provider.
provider "google-beta" {
  region  = var.region
  project = var.project_id
}