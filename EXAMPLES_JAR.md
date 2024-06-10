# Examples using a JAR file 
For easier deployment and management, Docker is the recommended approach for
running the tool. However, if Docker is unavailable, running with a JAR file
is also an option.

To run the tool use the [cloud shell](https://cloud.google.com/shell/docs/launching-cloud-shell#launch_from_the) terminal. It has all the
pre-requisites.

### Download repo and prebuilt jar.
```
# in cloud shell terminal
gcloud auth application-default login
git clone https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition.git
cd bigquery-antipattern-recognition
wget https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition/releases/download/v1.0.0/bigquery-antipattern-recognition.jar
```

### inline -> terminal
```
# in cloud shell terminal
java -jar  bigquery-antipattern-recognition.jar \
    --query "SELECT * FROM \`project.dataset.table1\`"
```

### INFORMATION_SCHEMA -> BQ Table
In the BigQuery console, run the DDL below to create the output table
```SQL 
CREATE OR REPLACE TABLE <my-project>.<my-dataset>.antipattern_output_table (
  job_id STRING,
  user_email STRING,
  query STRING,
  recommendation ARRAY<STRUCT<name STRING, description STRING>>,
  slot_hours FLOAT64,
  optimized_sql STRING,
  process_timestamp TIMESTAMP
);
```

To read from INFORMATION_SCHEMA and write to the output table, run the following
in the command line:
```
# in cloud shell terminal
gcloud auth login
java -jar  bigquery-antipattern-recognition.jar \
    --read_from_info_schema \
    --read_from_info_schema_days 1 \
    --processing_project_id <my-project> \
    --output_table "<my-project>.<my-dataset>.antipattern_output_table" \
    --info_schema_top_n_percentage_of_jobs 0.1  
```

Read output in BigQuery Console
```SQL
SELECT
  job_id, user_email, query, 
  recommendation, slot_hours
FROM 
  `<my-project>.<my-dataset>.antipattern_output_table`
ORDER BY
  process_timestamp DESC 
LIMIT 10000;
```

### BQ Table -> BQ Table
In the BigQuery console, run the DDL below to create the output table
```SQL 
CREATE OR REPLACE TABLE <my-project>.<my-dataset>.antipattern_output_table (
  job_id STRING,
  user_email STRING,
  query STRING,
  recommendation ARRAY<STRUCT<name STRING, description STRING>>,
  slot_hours FLOAT64,
  optimized_sql STRING,
  process_timestamp TIMESTAMP
);
```

In the BigQuery console, run the DDL below to create the input table
```SQL 
CREATE OR REPLACE TABLE <my-project>.<my-dataset>.antipattern_input_table (
  id STRING,
  query STRING
);
```
To read from a BQ table and write to the BQ output table, run the following
in the command line:
```
gcloud auth login

java -jar  bigquery-antipattern-recognition.jar \
  --input_bq_table <my-project>.<my-dataset>.antipattern_input_table \
  --output_table <my-project>.<my-dataset>.antipattern_output_table"
```

### local file -> terminal
Read from local file and write to terminal
```
java -jar  bigquery-antipattern-recognition.jar \
  --input_file_path ./samples/queries/input/multipleCTEs.sql
```

### local folder -> local CSV
Read from folder and write output to csv
```
java -jar  bigquery-antipattern-recognition.jar \
  --input_folder_path ./samples/queries/input \
  --output_file_path ./samples/queries/output/results.csv
```

### local csv -> local csv
Read from csv and output to csv
```
export INPUT_FOLDER=$(pwd)/samples/csv/input
export INPUT_CSV_FILENAME=input_queries.csv
export OUTPUT_FOLDER=$(pwd)/samples/csv/output
export OUTPUT_FILENAME=results.csv

java -jar  bigquery-antipattern-recognition.jar \
  --input_csv_file_path ./samples/csv/input/input_queries.csv \
  --output_file_path ./samples/csv/output/results.csv
```

### Run using advanced analysis  
The JoinOrder anti-pattern depends on table size.
The advanced analysis option allows the tool to fetch table metadata for the
JoinOrder anti-pattern. 

```
java -jar  bigquery-antipattern-recognition.jar \
  --advanced_analysis \
  --analyzer_default_project bigquery-public-data \
  --input_file_path ./samples/queries/input/joinOrder.sql 
```

### Dataform and DBT Query Extraction

Dataform and dbt both supply the [dataform](https://docs.dataform.co/dataform-cli#compile-your-code) compile and [dbt compile](https://docs.getdbt.com/reference/commands/compile) commands to view the compiled results in SQL.

This can be useful to run the SQL anti pattern across these compiled results.

For example, you can run the `dbt compile` command, navigate to the `target` directory in your project and upload the compiled model folder to GCS

```
gsutil cp * gs://my-bucket
```

Then, use that bucket as an input parameter for the antipattern tool

```
java -jar  bigquery-antipattern-recognition.jar \
  --input_folder_path gs://my-bucket \
  --output_table <my-project>.<my-dataset>.antipattern_output_table
```

Note that the tool will recursively search for SQL files recurseivly in the folder that you upload.

### information_schema views -> BQ Table
This example will check for anit pattern in the view definitions.

In the BigQuery console, run the DDL below to create the output table.
```SQL 
CREATE OR REPLACE TABLE <my-project>.<my-dataset>.antipattern_output_table (
  job_id STRING,
  user_email STRING,
  query STRING,
  recommendation ARRAY<STRUCT<name STRING, description STRING>>,
  slot_hours FLOAT64,
  optimized_sql STRING,
  process_timestamp TIMESTAMP
);
```

In the BigQuery console, run the DDL below to create the input table
```SQL 
CREATE OR REPLACE VIEW <my-project>.<my-dataset>.antipattern_input_views_def AS
SELECT 
  concat(table_catalog, '.', table_schema, '.', table_name) id,
  view_definition query
FROM <my-dataset>.INFORMATION_SCHEMA.VIEWS;

```
To read from the above view and write to the BQ output table, run the following
in the command line:
```
gcloud auth login

java -jar  bigquery-antipattern-recognition.jar \
  --input_bq_table <my-project>.<my-dataset>.antipattern_input_views_def \
  --output_table <my-project>.<my-dataset>.antipattern_output_table"
```

## License

```text
Copyright 2024 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```