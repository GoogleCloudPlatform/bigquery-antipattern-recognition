# BigQuery Anti-Pattern Recognition Tool (Remote Function UDF)


**What this tool does:**

1. **Cloud Run Service:** Deploys a Cloud Run service that houses the anti-pattern detection logic.
2. **BigQuery Remote Function:** Creates a BigQuery User-Defined Function (UDF) that acts as a bridge between your SQL queries and the Cloud Run service.
3. **Anti-Pattern Detection:** When you call the BigQuery UDF, it sends your SQL query to the Cloud Run service, which analyzes it for anti-patterns. The results are returned in JSON format.


## Costs

This tutorial uses billable components of Google Cloud, including the following:

* [BigQuery](https://cloud.google.com/bigquery/pricing)
* [Cloud Build](https://cloud.google.com/build/pricing)
* [Cloud Run](https://cloud.google.com/run/pricing)

Use the [pricing calculator](https://cloud.google.com/products/calculator) to generate a cost estimate based on your
projected usage.

## Before you begin

For this tutorial, you need a Google Cloud [project](https://cloud.google.com/resource-manager/docs/cloud-platform-resource-hierarchy#projects).

1.  [Create a Google Cloud project](https://console.cloud.google.com/projectselector2/home/dashboard).
1.  Make sure that [billing is enabled](https://support.google.com/cloud/answer/6293499#enable-billing) for your Google
    Cloud project.
1.  [Open Cloud Shell](https://console.cloud.google.com/?cloudshell=true).

    At the bottom of the Cloud Console, a [Cloud Shell](https://cloud.google.com/shell/docs/features) session opens and
    displays a command-line prompt. Cloud Shell is a shell environment with the Cloud SDK already installed, including
    the [gcloud](https://cloud.google.com/sdk/gcloud/) command-line tool, and with values already set for your current
    project. It can take a few seconds for the session to initialize.

1.  In Cloud Shell, clone the source repository and go to the directory for this tutorial:

        git clone https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition.git
        cd bigquery-antipattern-recognition/udf/terraform

2.  Enable all the required Google Cloud APIs

    ```shell
    gcloud services enable \
    artifactregistry.googleapis.com \
    bigquery.googleapis.com \
    bigqueryconnection.googleapis.com \
    cloudbuild.googleapis.com \
    run.googleapis.com \
    ```

## Deployment script

1.  Authenticate using User [Application Default Credentials ("ADCs")](https://cloud.google.com/sdk/gcloud/reference/auth/application-default) as a primary authentication method.
    ```shell
    gcloud auth application-default login
    ```

2.  Initialize and run the Terraform script to create all resources:

    ```shell
    terraform init && \
    terraform apply
    ```

3.  Once the script successfully completes resources creation,
    visit [BigQuery Console](https://console.cloud.google.com/bigquery)
    to run the test SQL script

    ```sql
    SELECT fns.get_antipatterns("SELECT * from dataset.table ORDER BY 1")
      ```


## Detailed Deployment steps

``` sql
CREATE OR REPLACE FUNCTION `project.dataset`.get_antipatterns(query STRING)
RETURNS JSON
REMOTE WITH CONNECTION `connection-id`
OPTIONS (
  endpoint = 'cloud-run-endpoint'
);
```

After which, the function can be invoked with:
```sql
SELECT fns.get_antipatterns("SELECT * from dataset.table ORDER BY 1")
```

The function returns a JSON string for each query representing the antipatterns found in each query, if any. For example the function would return the following response for the query above:

``` json
{"antipatterns":[{"name":"SimpleSelectStar","result":"SELECT * on table: dataset. Check that all columns are needed."},{"name":"OrderByWithoutLimit","result":"ORDER BY clause without LIMIT at line 1."}]}
```