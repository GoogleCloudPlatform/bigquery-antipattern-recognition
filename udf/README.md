# BigQuery Anti-Pattern Recognition Tool (Remote Function UDF)

A [BigQuery remote function](https://cloud.google.com/bigquery/docs/remote-functions) lets you incorporate GoogleSQL functionality with software outside of BigQuery by providing a direct integration with Cloud Functions and Cloud Run. With BigQuery remote functions, you can deploy your functions in Cloud Functions or Cloud Run implemented with any supported language, and then invoke them from GoogleSQL queries.

This tool can be packaged and built to deploy as a service on Cloud Run. Then, a BigQuery remote function can be created to call the service:

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


## Objectives

* Deploy Cloud Run service providing Antipattern detection functionality
* Create BigQuery Remote function that uses the Cloud Run service
* Verify Antipattern detection in BigQuery using a SQL query

## Costs

This tutorial uses billable components of Google Cloud, including the following:

* [BigQuery](https://cloud.google.com/bigquery/pricing)
* [Cloud Build](https://cloud.google.com/build/pricing)
* [Cloud Run](https://cloud.google.com/run/pricing)

Use the [pricing calculator](https://cloud.google.com/products/calculator) to generate a cost estimate based on your
projected usage.

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
    SELECT dataset.get_antipatterns("SELECT * from dataset.table ORDER BY 1")

      ```


## Detailed Deployment steps