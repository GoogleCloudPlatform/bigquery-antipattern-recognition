#!/bin/bash

result=$(bq query --use_legacy_sql=false "SELECT COUNT(*) = 0 AS is_empty FROM \`bigquery-antipattern-test.antipattern_test_data.test_results\`")
# Check the 'is_empty' column in the result
if [[ $result == *"true"* ]]; then
echo "true"  # Table is empty
else
echo "false" # Table is not empty
exit 1
fi