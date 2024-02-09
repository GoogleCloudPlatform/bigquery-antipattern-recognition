# Google Cloud BigQuery Antipattern Recognition

This repository contains the Terraform scripts that build and deploy the BigQuery Antipattern Recognition tool to Cloud Run Jobs. On each execution, the tool is configured to perform antipattern recognition for all jobs run during the previous 24 hours and to write the results to a BigQuery table. 


There are two optional flags when running the terraform scripts.

1. Optionally, a Cloud Scheduler cron job can be deployed to run the tool on a schedule by setting apply_scheduler = true.
2. Optionally, a Cloud Workflow can be deployed obtain query hashes, run the antipattern tool, and join the recommendation back on to the hashes. This is useful to run the antipattern tool across queries that may be repeated many times. Read more about query hashes [here](https://github.com/GoogleCloudPlatform/bigquery-utils/tree/master/scripts/optimization#query-analysis). When you want to run the Cloud Workflow, set apply_workflow = true, specify an input table name that will be used for intermediary extraction of query hashes, and the cloud_run_job_name_hash of the Cloud Run job.

Following resources are created when running the code:
1. Cloud Run Job
2. Service Account for Cloud Run Job
3. Cloud Scheduler (optional)
4. Cloud Workflow (optional)
5. Service Account for Cloud Scheduler
6. Service Account for Cloud Workflow (optional)
5. Artifact Registry
6. Table in BigQuery Dataset (optional)

[![Open in Cloud Shell](https://gstatic.com/cloudssh/images/open-btn.svg)](https://shell.cloud.google.com/cloudshell/editor?cloudshell_git_repo=https%3A%2F%2Fgithub.com%2FGoogleCloudPlatform%2Fbigquery-antipattern-recognition&cloudshell_open_in_editor=terraform%2F&cloudshell_tutorial=terraform%2FREADME.md)

## Getting Started

Follow the instructions below to set up the tool through Google Cloud Shell or a local terminal.

### Execution Pre-requisites
Before you begin, ensure you have met the following requirements:

- **Terraform:** Terraform should already be installed in the Google Cloud Shell. If running locally, follow the instructions [here](https://learn.hashicorp.com/tutorials/terraform/install-cli) to install it. Make sure you're using Terraform version 1.3.0 or later.

- **Google Cloud SDK:** You should have the Google Cloud SDK installed and configured on your local terminal. If you don't have it installed, you can do so [here](https://cloud.google.com/sdk/docs/install).

- **Google Cloud Project:** You should have a Google Cloud project. If you don't have one, you can create one [here](https://cloud.google.com/resource-manager/docs/creating-managing-projects).

- **Permissions:** Ensure that you have the necessary permissions to create and manage resources in the Google Cloud project.

- **BigQuery:** You should have BigQuery enabled with Dataset in your Google Cloud project. You can enable it [here](https://console.cloud.google.com/bigquery).


### Steps to Set Up


1. **Clone the repository**:
   
   Clone the repository using the following command:

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
    apply_workflow = "" # Determines if a Cloud Workflow job should be applied on query hashes, default is false
    input_table= ""    # Intermediary name for raw query table used for antipattern tool if using hash workflow
    output_table = "" # The BigQuery table that will be used for storing the results from the Anti Pattern Detector
    apply_scheduler = "" # Whether to apply scheduler or not (true or false)
    scheduler_frequency = "" # Schedule frequency for the Cloud Scheduler job, in cron format. Default value is "0 5 * * *"
    apply_workflow = "" # Determines if a Cloud Workflow should be run for query hashes, default is false
    input_table = ""    # Intermediary name for raw query table used for antipattern tool if using hash workflow
    cloud_run_job_name_hash = "" # The name of the Cloud Run job that will be created for query hashes
    bigquery_dataset_name = "" # Name of the existing BigQuery dataset where output table will be created
    create_output_table   = "" # Determines whether the output table is created in the BigQuery Dataset. The default value is true.
    ```

    Eg:
    ```shell
    project_id = "demo-prj-873454"
    region = "us-central1"
    repository = "bigquery-antipattern-recognition"
    cloud_run_job_name = "bigquery-antipattern-recognition"
    output_table = "antipattern_output_table"
    apply_scheduler = true
    scheduler_frequency   = "0 5 * * *"
    apply_workflow = false 
    input_table = ""    
    cloud_run_job_name_hash = "" 
    bigquery_dataset_name = "antipattern"
    create_output_table   = true
    ```

    Or if using Cloud Worflow for query hashes:
    ```shell
    project_id = "demo-prj-873454"
    region = "us-central1"
    repository = "bigquery-antipattern-recognition"
    cloud_run_job_name = "bigquery-antipattern-recognition"
    output_table = "antipattern_output_table"
    apply_scheduler = true
    scheduler_frequency   = "0 5 * * *"
    apply_workflow = true 
    input_table = "hash_raw"    
    cloud_run_job_name_hash = "bq-hash-antipattern" 
    bigquery_dataset_name = "optimization_workshop"
    create_output_table   = true
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

## Contributing
If you have suggestions or improvements, feel free to submit a pull request or create an issue.
