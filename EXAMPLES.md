### inline -> log
```
docker run \
  -i bigquery-antipattern-recognition \
  --query "SELECT * FROM \`project.dataset.table1\`" 
```

### INFORMATION_SCHEMA -> BQ Table
In the BigQuery console, run the DDL bellow to create th output table
```SQL 
CREATE OR REPLACE TABLE <my-project>.<my-dataset>.antipattern_output_table (
  job_id STRING,
  user_email STRING,
  query STRING,
  recommendation ARRAY<STRUCT<name STRING, description STRING>>,
  slot_hours FLOAT64,
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
  --output_file_path $OUTPUT_FOLDER/$OUT_FILENAME
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
  --input_csv_file_path $INPUT_FOLDER/$INPUT_FILENAME \
  --output_file_path $OUTPUT_FOLDER/$OUT_FILENAME
```

### Run using advanced analytics 
The JoinOrder anti-pattern depends on table size.
The advanced analytics option allows the tool to fetch table metadata for the
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
