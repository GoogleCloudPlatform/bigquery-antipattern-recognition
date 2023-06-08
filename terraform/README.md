# Google Cloud BigQuery Antipattern Recognition

This repository contains the Terraform scripts for the Google Cloud BigQuery Antipattern Recognition.

[![Open in Cloud Shell](https://gstatic.com/cloudssh/images/open-btn.svg)](https://shell.cloud.google.com/cloudshell/editor?cloudshell_git_repo=https%3A%2F%2Fgithub.com%2FGoogleCloudPlatform%2Fbigquery-antipattern-recognition&cloudshell_open_in_editor=terraform%2F&cloudshell_tutorial=terraform%2FREADME.md)

## Getting Started

These instructions will get you a copy of the project up and running on your Google Cloud Shell.

### Execution Pre-requisites:
When running through Cloud Shell, please clone the source repo with the mentioned steps.

**Note**: If running through any local terminal, please make sure you have terraform and gcloud already installed.


### Steps to Set Up


1. **Clone the repository**:
   
   Clone the repository to your Google Cloud Shell using the following command:

   ```shell
   git clone https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition.git
   ```

2. **Navigate to the Terraform directory**:
    
    ```shell
    cd bigquery-antipattern-recognition/terraform/
    ```

3. **Update the variables in the tfvars file**:

    Open the `terraform.tfvars` file in your preferred text editor and update the values as per your requirements.

    ```shell
    terraform.tfvars
    ```
    
    Here's an example of the variable declaration:

    ```shell
    project_id = "" # The ID of the Google Cloud Project where all resources will be created
    region = "" # The region in which the Artifact Registry, Cloud Run and Cloud Scheduler services will be deployed
    repository = "" # The name of the Artifact Registry repository
    cloud_run_job_name = "" # The name of the Cloud Run job that will be created
    output_table = "" # The BigQuery table that will be used for storing the results from the Anti Pattern Detector
    apply_scheduler = "" # Whether to apply scheduler or not (true or false)
    scheduler_frequency = "" # Schedule frequency for the Cloud Scheduler job, in cron format. Default value is "0 5 * * *"
    bigquery_dataset_name = "" # Name of the existing BigQuery dataset where output table will be created
    ```

    Eg:
    ```shell
    project_id = "demo-prj-873454"
    region = "us-central1"
    repository = "bigquery-antipattern-recognition"
    cloud_run_job_name = "bigquery-antipattern-recognition"
    output_table = "demo"
    apply_scheduler = true
    scheduler_frequency   = "0 5 * * *"
    bigquery_dataset_name = "antipattern"
    ```


4. **Initialize Terraform**:

    Run the terraform init command to download and initialize the necessary provider plugins for Terraform.

    ```shell
    terraform init
    ```

5. **Apply Terraform configuration**:

    Apply the Terraform configuration using the following command. This will create all the required resources in Google Cloud.

    ```shell
    terraform apply
    ```


    To review the changes before applying, you can use `terraform plan`.

    Note: Make sure to confirm the action by typing `yes` when Terraform asks for approval.
