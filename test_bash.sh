#!/bin/bash

# Replace placeholders with your project and dataset
PROJECT_ID="bigquery-antipattern-test"
DATASET="antipattern_test_data"
TABLE="test_results"

# Run the BigQuery query and capture the output
result=$(bq query --use_legacy_sql=false \
   "SELECT COUNT(*) = 0 AS is_empty FROM \`$PROJECT_ID.$DATASET.$TABLE\`")

# Check the 'is_empty' column in the result
if [[ $result == *"true"* ]]; then
    echo "true"  # Table is empty
else
    echo "false" # Table is not empty
fi