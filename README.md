# BigQuery Optimization via Anti-Pattern Recognition

This utility scans a BigQuery SQL in search for several possible anti-patterns. 
Anti-patterns are specific SQL syntaxes that in some cases might cause 
performance impact.

We recommend using this tool to scan the top 10% slot consuming jobs of your 
workload. Addressing these anti-patterns in most cases will provide performance 
significant benefits. 

Example of tool input and output :

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


# Quick Start Docker

To run the tool use the [cloud shell](https://cloud.google.com/shell/docs/launching-cloud-shell#launch_from_the) terminal. It has all the 
pre-requisites.

Build utility in [Cloud Shell](https://cloud.google.com/shell/docs/launching-cloud-shell)
```
# in cloud shell terminal

gcloud auth application-default login
git clone https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition.git
cd bigquery-antipattern-recognition
mvn clean package jib:dockerBuild -DskipTests
```

Run tool for simple inline query
```
# in cloud shell terminal
docker run \
  -i bigquery-antipattern-recognition \
  --query "SELECT * FROM \`project.dataset.table1\`" 
```

In the BigQuery console, run the DDL bellow to create th output table 
```SQL
-- in BQ console 
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
docker run \
    -v ~/.config:/root/.config \
    -i bigquery-antipattern-recognition \
    --read_from_info_schema \
    --info_schema_project <project-name-with-info-schema-to-be-read> \
    --info_schema_region us \
    --read_from_info_schema_days 1 \
    --processing_project_id <my-processing-project> \
    --output_table "<my-project>.<my-dataset>.antipattern_output_table" \
    --info_schema_top_n_percentage_of_jobs 0.1  

```

Read output in BigQuery Console
```SQL
-- in BQ console
SELECT
  job_id, user_email, query, 
  recommendation, slot_hours
FROM 
  `<my-project>.<my-dataset>.antipattern_output_table`
ORDER BY
  process_timestamp DESC 
LIMIT 10000;
```

### Other input / output options
* [local file -> terminal](./EXAMPLES.md#local-file---terminal)
* [local folder -> csv](./EXAMPLES.md#local-folder---local-csv)
* [csv -> csv](./EXAMPLES.md#local-csv---local-csv)
* [bq -> bq](./EXAMPLES.md#bq-table---bq-table)


# Quick Start JAR
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

Run tool for simple inline query
```
# in cloud shell terminal
java -jar  bigquery-antipattern-recognition.jar \
    --query "SELECT * FROM \`project.dataset.table1\`"
```

### Other input / output options (JAR)
* [information_schema -> bq table](./EXAMPLES_JAR.md#information_schema---bq-table)
* [local file -> terminal](./EXAMPLES_JAR.md#local-file---terminal)
* [local folder -> csv](./EXAMPLES_JAR.md#local-folder---local-csv)
* [csv -> csv](./EXAMPLES_JAR.md#local-csv---local-csv)
* [bq -> bq](./EXAMPLES_JAR.md#bq-table---bq-table)



# Rewrite Using AI
This tool has an AI based feature that will output the rewritten query.\
This functionality is activated using the `--rewrite_sql` flag.\
(This feature is available in all outputs: logs, files, BigQuery table).

**Prerequisite**: enable [Vertex AI API](https://pantheon.corp.google.com/vertex-ai)

Input
```
docker run \
    -v ~/.config:/root/.config \
    -i bigquery-antipattern-recognition \
    --query "select col1 from table1 where col2 like '%abc%' and col3=1" \
    --rewrite_sql \
    --processing_project_id <my-project>
```

Output
```
--------------------------------------------------
Recommendations for query: query provided by cli:
* WhereOrder: LIKE filter in line 1 precedes a more selective filter.
* Optimized query:
select col1 from table1 where col3=1 and col2 like '%abc%'
--------------------------------------------------
```

# Deploy as a Remote Function UDF
Deploying the anti-pattern recognition tool as a remote function UDF allows you to easily call the Antipattern tool within SQL. 

For example: 

```sql
SELECT fns.get_antipatterns("SELECT * from dataset.table ORDER BY 1")
```

The function returns a JSON string for each query representing the antipatterns found in each query, if any. For example the function would return the following response for the query above:

``` json
{
  "antipatterns": [
    {
      "name": "SimpleSelectStar",
      "result": "SELECT * on table: dataset.table. Check that all columns are needed."
    },
    {
      "name": "OrderByWithoutLimit",
      "result": "ORDER BY clause without LIMIT at line 1."
    }
  ]
}
```

The remote function is built using Cloud Build, Artifact Registry, and Cloud Run. It can be deployed using terraform or a bash scipt. See more details in the [README](./udf/README.md).

# Deploy to Cloud Run Jobs
Deploying the anti-pattern recognition tool to a [Cloud Run](https://cloud.google.com/run?hl=en) job provides an easy mechanism to periodically scan INFORMATION_SCHEMA for occurrences of anti-patterns in top slot consuming queries.

Refer to this [step-by-step](./CR_DEPLOY.md) on how to deploy this tool using Cloud Run.

# Dataform and DBT
See the [examples page](./EXAMPLES.md#dataform-and-dbt-query-extraction) for information on extracting SQL from Dataform and DBT

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

`--read_from_info_schema_days n`
<ul>
Specifies how many days of INFORMATION_SCHEMA to read <br> 
Must be set along with `--read_from_info_schema`. <br>
Defaults to 1.
</ul>


`--info_schema_project <project-name>`
<ul>
Name of the project for which information schema will be read. <br>
This is the project with the queries that you want to optimize.
</ul>

`--info_schema_region us`
<ul>
Region from which to read information schema  
</ul>

`--read_from_info_schema_start_time "start-timestamp"` <br>
`--read_from_info_schema_end_time "end-timestamp"`
<ul>
Alternative to `read_from_info_schema_days` option,<br>
to specify start and end date or timestamp of INFORMATION_SCHEMA to read.<br>
Must be set along with `--read_from_info_schema`. <br>
Defaults to `--read_from_info_schema_days` option.
</ul>

`--read_from_info_schema_timeout_in_secs n`
<ul>
Specifies timeout, in secs, to query INFORMATION SCHEMA<br>
Must be set along with `--read_from_info_schema`. <br>
Defaults to 60.
</ul>

``--info_schema_table_name "\`<my-project>.region-us\`.INFORMATION_SCHEMA.JOBS" \``
<ul>
Specifies what variant of INFORMATION_SCHEMA.JOBS to read from.
</ul>

`--info_schema_top_n_percentage_of_jobs n`
<ul>
Number between 0 and 1. Uses to specify what fraction of top slot consuming jobs
the tool should consider, e.g. if equal to 0.1 only top 10% slot consuming jobs 
will be checked por anti patterns.
</ul>

### To read from a BigQuery Table
`--input_bq_table project_id.dataset_id.table_name`
<ul>
Specifies a BigQuery table as input, each row is a SQL string to be parsed.<br>
Columns must be "id (String), query (String)"
</ul>

### To read from a files
`--input_file_path /path/to/file.sql`
<ul>
Specifies path to file with SQL string to be parsed. Can be local file or GCS file.
</ul>

`--input_folder_path /path/to/folder/with/sql/files`
<ul>
Specifies path to folder with SQL files to be parsed. Will parse all .sql in directory.<br>
Can be a local path or a GCS path
</ul>

`--input_csv_file_path /path/to/input/file.csv`
<ul>
Specifies a CSV file as input, each row is a SQL string to be parsed.<br>
Columns must be ""id,query"
</ul>


## Specify output
`--output_file_path /path/to/output/file.csv`
<ul>
Specifies a CSV file as output, each row is a SQL string to be parsed.<br>
Columns are "id,recommendation"
</ul>

`--output_table "my-project.dataset.antipattern_output_table" `
<ul>
Specifies table to which write results to. Assumes that the table already exits.
</ul>

## Specify compute project
`--processing_project_id <my-processing-project>`
<ul>
Specifies what project provides the compute used to read from INFORMATION_SCHEMA <br> 
and/or to write to output table (i.e. project where BQ jobs will execute) <br>
Needed if the input is INFORMATION_SCHEMA or if the output is a BQ table. <br>
Needed if using sql rewrite. 
</ul>


## Using AI for rewrite
`--rewrite_sql` 
<ul>
If used a rewritted SQL will be provided. The rewrite will be performed using an LLM.<br>
This is an experimental feature. Requires processing_project_id to be specified.
</ul>

## Using a service account
`--service_account_keyfile_path`
<ul>
Path to service account json keyfile.
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

## Anti Pattern 2: SEMI-JOIN without aggregation
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

## Anti Pattern 3: Multiple CTEs referenced more than twice
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

## Anti Pattern 4: Using ORDER BY without LIMIT
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

## Anti Pattern 5: Using REGEXP_CONTAINS when LIKE is an option
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



## Anti Pattern 6: Using an analytic functions to determine latest record
Example:
```
SELECT
  taxi_id, trip_seconds, fare
FROM
  (
  SELECT
    taxi_id, trip_seconds, fare,
    row_number() over(partition by taxi_id order by fare desc) rn
  FROM
    `bigquery-public-data.chicago_taxi_trips.taxi_trips`
)
WHERE
  rn = 1;
```

Output:
```
LatestRecordWithAnalyticFun: Seems like you might be using analytical function row_number in line 7 to filter the latest record in line 12.
```

## Anti Pattern 7: Convert Dynamic Predicates into Static
Example:
```
SELECT
 *
FROM 
  comments c
JOIN 
  users u ON c.user_id = u.id
WHERE 
  u.id IN (
    SELECT id 
    FROM users
    WHERE location LIKE '%New York'
    GROUP BY id
    ORDER BY SUM(up_votes) DESC
    LIMIT 10
  )
;
```

Output:
```
Dynamic Predicate: Using subquery in filter at line 10. Converting this dynamic predicate to static might provide better performance.
```


## Anti Pattern 8: Where order, apply most selective expression first
Example:
```
SELECT 
  repo_name, 
  id,
  ref
FROM 
  `bigquery-public-data.github_repos.files` 
WHERE
  ref like '%master%'
  and repo_name = 'cdnjs/cdnjs'
;
```

Output:
```
WhereOrder: LIKE filter in line 8 precedes a more selective filter.
```

## Anti Pattern 9: Join Order 
As a [best practice](https://cloud.google.com/bigquery/docs/best-practices-performance-compute#optimize_your_join_patterns)
the table with the largest number of rows should be placed first in a JOIN. 

This anti-pattern checks the join order based on the number of rows of each 
table. To do so this tool must fetch table metadata, for which the `advanced_analysis`
flag must be used.

Details can be found [here](./EXAMPLES.md#run-using-advanced-analysis).

Example:
```
SELECT  
  t1.station_id,
  COUNT(1) num_trips_started
FROM
  `bigquery-public-data.austin_bikeshare.bikeshare_stations` t1
JOIN
  `bigquery-public-data.austin_bikeshare.bikeshare_trips` t2 ON t1.station_id = t2.start_station_id
GROUP BY
  t1.station_id
;
```

Output:
```
JoinOrder: JOIN on tables: [bikeshare_stations, bikeshare_trips] might perform 
better if tables where joined in the following order: 
[bikeshare_trips, bikeshare_stations]
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
