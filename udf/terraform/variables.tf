variable "project_id" {
  type = string
  description = "Your GCP project ID"
}

variable "region" {
  type = string
  description = "The region in which to deploy resources"
  default     = "us-central1"
}

variable "service_name" {
    type = string
    description= "The name of the cloud run service"
    default = "antipattern-service"
}

variable "artifact_registry_name" {
  type = string
  default = "bq-remote-functions"
}

variable "bq_dataset" {
  type = string
  default = "fns"
}