# BigQuery Optimization via Anti-Pattern Recognition

This utility scans a BigQuery SQL in search for several possible anti-patterns. \
Identifying these anti-patterns is the first step in optimizing a SQL since these \
usually have high performance impact. 

For example:

Example:
Input:
```
SELECT 
    * 
FROM 
    `project.dataset.table1`
```

Output:
```
All columns on table: project.dataset.table1 are being selected. Please be sure that all columns are needed
```



# Quick Start

Prerequisites:
* [gcloud CLI](https://cloud.google.com/sdk/gcloud)
* Docker

Build ZetaSQL Toolkit 
```
cd ../zetasql-toolkit-core
mvn clean
mvn install
```

Build utility
```
cd ../bigquery-antipattern-recognition
mvn clean
mvn install
mvn compile jib:dockerBuild
```

Run simple inline query
```
docker run \
  -i bigquery-antipattern-recognition \
  --query "SELECT * FROM \`project.dataset.table1\`" 
```

Read from information schema and write to output table: 
1) Create a table with the following DDL:
```
CREATE TABLE dataset.antipattern_output_table (
  job_id STRING,
  query STRING,
  recommendation STRING,
  slot_hours FLOAT64,
  process_timestamp TIMESTAMP
);
```

2) Authenticate 
```
gcloud auth login
```

3) Run 
```
docker run \
  -v ~/.config:/root/.config \
  -i bigquery-antipattern-recognition \
  --read_from_info_schema \
  --read_from_info_schema_days 1 \
  --processing_project_id <my-project> \
  --output_table "my-project.dataset.antipattern_output_table" 
```

# Deploy to Cloud Run Jobs

## Overview

One option to deploy the BigQuery Anti Pattern Recognition tool is deploying to Cloud Run jobs. Cloud Run jobs can help you easily deploy the tool as a container and later trigger it, either manually or on a schedule.

In order to deploy the tool to Cloud Run Jobs, you'll need to:
* Package the tool into a container and push it to Artifact Registry
* Create the Cloud Run job using the container and your desired configuration
* Trigger the job, either manually or on a schedule

## Walkthrough

1. Setup deployment configuration

    ``` bash
    export PROJECT_ID=""  # Project ID where resources are created
    export REGION="us-central1"  # Region for Artifact Registry, Cloud Run and Cloud Scheduler
    export REPOSITORY="bigquery-antipattern-recognition"  # Artifact Registry repository name

    export CONTAINER_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/recognizer:0.1.1-SNAPSHOT"

    export CLOUD_RUN_JOB_NAME="bigquery-antipattern-recognition"  # Name for the Cloud Run job
    export CLOUD_RUN_JOB_SA=""  # Service account associated to the Cloud Run job
    export OUTPUT_TABLE=""  # BigQuery output table for the Anti Pattern Detector
    ```

2. Create an Artifact Registry Repository, if necessary

    ``` bash
    gcloud artifacts repositories create $REPOSITORY \
        --repository-format=docker \
        --location=$REGION \
        --project=$PROJECT_ID
    ```

3. Build and push the container image to Artifact Registry

    ``` bash
    gcloud auth configure-docker $REGION-docker.pkg.dev

    mvn clean compile jib:build \
        -DskipTests \
        -Djib.to.image=$CONTAINER_IMAGE
    ```

4. Create the Cloud Run Job

   This example configuration reads all queries for the previous day from `INFORMATION_SCHEMA`, runs antipattern detection and writes the result to the configured `OUTPUT_TABLE`.

    ``` bash
    gcloud run jobs create $CLOUD_RUN_JOB_NAME \
        --image=$CONTAINER_IMAGE \
        --max-retries=3 \
        --task-timeout=15m \
        --args="--read_from_info_schema" \
        --args="--read_from_info_schema_days" --args="1" \
        --args="--processing_project_id" --args="$PROJECT_ID" \
        --args="--output_table" --args="\\\`$OUTPUT_TABLE\\\`" \
        --service-account=$CLOUD_RUN_JOB_SA \
        --region=$REGION \
        --project=$PROJECT_ID
    ```

5. (Optional) Trigger the job manually

    ``` bash
    gcloud run jobs execute $CLOUD_RUN_JOB_NAME \
        --region=$REGION \
        --project=$PROJECT_ID \
        --wait
    ```

6. (Optional) Run the job on a schedule using Cloud Scheduler

    ``` bash
    export CLOUD_RUN_INVOKER_SA=""  # Service account that will invoke the job

    gcloud scheduler jobs create http $CLOUD_RUN_JOB_NAME-trigger \
        --location=$REGION \
        --schedule="0 5 * * *" \
        --uri="https://$REGION-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$PROJECT_ID/jobs/$CLOUD_RUN_JOB_NAME:run" \
        --http-method="POST" \
        --oauth-service-account-email="$CLOUD_RUN_INVOKER_SA"
    ```



# Flags and arguments
## Specify Input
### To read inline query
`--query="SELECT ... FROM ..."`
<ul>
To parse SQL string provided via CLI.
</ul>

### To read from INFORMATION_SCHEMA
`--read_from_info_schema`
<ul>
To read input queries from INFORMATION_SCHEMA.JOBS.
</ul>

`--read_from_info_schema_days=n`
<ul>
Specifies how many days of INFORMATION_SCHEMA to read <br> 
Must be set along with `--read_from_info_schema`. <br>
Defaults to 1.
</ul>

``--read_from_info_table="\`region-us\`.INFORMATION_SCHEMA.JOBS"``
<ul>
Specifies what variant of INFORMATION_SCHEMA.JOBS to read from.
</ul>

### To read from a files
`--input_file_path=/path/to/file.sql`
<ul>
Specifies path to file with SQL string to be parsed. Can be local file or GCS file.
</ul>

`--input_folder_path=/path/to/folder/with/sql/files`
<ul>
Specifies path to folder with SQL files to be parsed. Will parse all .sql in directory.<br>
Can be a local path or a GCS path
</ul>

`--input_csv_file_path=/path/to/input/file.csv`
<ul>
Specifies a CSV file as input, each row is a SQL string to be parsed.<br>
Columns must be ""id,query"
</ul>


## Specify output
`--output_file_path=/path/to/output/file.csv`
<ul>
Specifies a CSV file as output, each row is a SQL string to be parsed.<br>
Columns are "id,recommendation"
</ul>

`--output_table="my-project.dataset.antipattern_output_table" `
<ul>
Specifies table to which write results to. Assumes that the table already exits.
</ul>

## Specify compute project
`--processing_project_id=<my-processing-project>`
<ul>
Specifies what project provides the compute used to read from INFORMATION_SCHEMA <br> 
and/or to write to output table (i.e. project where BQ jobs will execute) <br>
Only needed if the input is INFORMATION_SCHEMA or if the output is a BQ table. 
</ul>



# Anti patterns
## Anti Pattern 1: Selecting all columns 
Example:
```
SELECT 
    * 
FROM 
    `project.dataset.table1`
```

Output: 
```
All columns on table: project.dataset.table1 are being selected. Please be sure that all columns are needed
```


## Anti Pattern 2: Using CROSS JOINs when INNER JOINs are an option
Example:
```
SELECT
   t1.col1
FROM 
   `project.dataset.table1` t1 
cross JOIN " +
    `project.dataset.table2` t2
WHERE
   t1.col1 = t2.col1;
```

Output:
```
CROSS JOIN between tables: project.dataset.table1 and project.dataset.table2. Try to change for a INNER JOIN if possible.
```

## Anti Pattern 3: Not aggregating subquery in the WHERE clause
Example:
```
SELECT 
   t1.col1 
FROM 
   `project.dataset.table1` t1 
WHERE 
    t1.col2 not in (select col2 from `project.dataset.table2`);
```

Output:
```
You are using an IN filter with a subquery without a DISTINCT on the following columns: project.dataset.table1.col2
```

## Anti Pattern 4: Multiple CTEs referenced more than twice
Example:
```
WITH
  a AS (
  SELECT col1,col2 FROM test WHERE col1='abc' 
  ),
  b AS ( 
    SELECT col2 FROM a 
  ),
  c AS (
  SELECT col1 FROM a 
  )
SELECT
  b.col2,
  c.col1
FROM
  b,c;
```

Output:
```
CTE with multiple references: alias a defined at line 2 is referenced 2 times
```

## Anti Pattern 5: Using NTILE when APPROX_QUANTILE IS AN OPTION
Example:
```
SELECT
  taxi_id,
  fare,
  payment_type,
  NTILE(4) OVER (PARTITION BY payment_type ORDER BY fare ASC) AS fare_rank
FROM
  `taxi_trips` trips
WHERE
  EXTRACT(YEAR
  FROM
    trips.trip_start_timestamp AT TIME ZONE "UTC") = 2013;
```

Output:
```
Use of NTILE window function detected at line 5. Prefer APPROX_QUANTILE if approximate bucketing is sufficient.
```

## Anti Pattern 6: Using ORDER BY WITHOUT LIMIT
Example:
```
SELECT
  t.dim1,
  t.dim2,
  t.metric1
FROM
  `dataset.table` t
ORDER BY
  t.metric1 DESC;
```

Output:
```
ORDER BY clause without LIMIT at line 8.
```

## Anti Pattern 7: Using REGEXP_CONTAINS WHEN LIKE IS AN OPTION
Example:
```
SELECT
  dim1
FROM
  `dataset.table`
WHERE
  REGEXP_CONTAINS(dim1, ‘.*test.*’)
```

Output:
```
REGEXP_CONTAINS at line 6. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
```
# Licensing 
Copyright 2023 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
