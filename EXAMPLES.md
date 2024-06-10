### inline -> terminal
```
docker run \
  -i bigquery-antipattern-recognition \
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
gcloud auth login

docker run \
    -v ~/.config:/root/.config \
    -i bigquery-antipattern-recognition \
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

docker run -i bigquery-antipattern-recognition \
  --input_bq_table <my-project>.<my-dataset>.antipattern_input_table \
  --output_table <my-project>.<my-dataset>.antipattern_output_table"
```

### local file -> terminal
Read from local file and write to terminal
```
export INPUT_FOLDER=$(pwd)/samples/queries/input
export INPUT_FILE_NAME=multipleCTEs.sql
docker run \
  -v $INPUT_FOLDER:$INPUT_FOLDER \
  -i bigquery-antipattern-recognition \
  --input_file_path $INPUT_FOLDER/$INPUT_FILE_NAME
```
### local folder -> local CSV
Read from folder and write output to csv
```
export INPUT_FOLDER=$(pwd)/samples/queries/input
export OUTPUT_FOLDER=$(pwd)/samples/queries/output
export OUTPUT_FILENAME=results.csv

docker run \
  -v $INPUT_FOLDER:$INPUT_FOLDER \
  -v $OUTPUT_FOLDER:$OUTPUT_FOLDER \
  -i bigquery-antipattern-recognition \
  --input_folder_path $INPUT_FOLDER \
  --output_file_path $OUTPUT_FOLDER/$OUTPUT_FILENAME
```

### local csv -> local csv
Read from csv and output to csv
```
export INPUT_FOLDER=$(pwd)/samples/csv/input
export INPUT_CSV_FILENAME=input_queries.csv
export OUTPUT_FOLDER=$(pwd)/samples/csv/output
export OUTPUT_FILENAME=results.csv

docker run \
  -v $INPUT_FOLDER:$INPUT_FOLDER \
  -v $OUTPUT_FOLDER:$OUTPUT_FOLDER \
  -i bigquery-antipattern-recognition \
  --input_csv_file_path $INPUT_FOLDER/$INPUT_CSV_FILENAME \
  --output_file_path $OUTPUT_FOLDER/$OUTPUT_FILENAME
```

### Run using advanced analysis  
The JoinOrder anti-pattern depends on table size.
The advanced analysis option allows the tool to fetch table metadata for the
JoinOrder anti-pattern. 

```
docker run \
  -v $(pwd)/samples/queries:/samples/queries \
  -v ~/.config:/root/.config \
  -i bigquery-antipattern-recognition \
  --advanced_analysis \
  --analyzer_default_project bigquery-public-data \
  --input_file_path /samples/queries/input/joinOrder.sql 
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
docker run \
  -i bigquery-antipattern-recognition \
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
FROM dataset.INFORMATION_SCHEMA.VIEWS;

```
To read from the above view and write to the BQ output table, run the following
in the command line:
```
gcloud auth login

docker run \
  -i bigquery-antipattern-recognition \
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