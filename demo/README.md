# BigQuery Anti-Pattern Recognition - Demo

This demo provides a comprehensive guide to deploy and use the BigQuery Anti-Pattern Recognition tool across **three different deployment scenarios**. Each scenario is optimized for different use cases and integration patterns.

## 🎯 Deployment Scenarios

### 1. **Cloud Run Job (Batch Processing)**
- **Use Case**: Scheduled analysis of query history from INFORMATION_SCHEMA
- **Main Class**: `com.google.zetasql.toolkit.antipattern.Main`
- **Best For**: Regular audits, automated monitoring, processing historical data
- **Output**: Results written to BigQuery table
- **Configuration**: Uses `cloudbuild-batch.yaml`

### 2. **Cloud Run Service (REST API)**
- **Use Case**: Real-time anti-pattern detection via HTTP API
- **Main Class**: `com.google.zetasql.toolkit.antipattern.AntiPatternApplication`
- **Best For**: CI/CD integration, interactive tools, on-demand analysis
- **Output**: JSON response with anti-patterns and recommendations
- **Configuration**: Uses `cloudbuild-service.yaml`

### 3. **BigQuery Remote UDF**
- **Use Case**: SQL-native anti-pattern detection within BigQuery
- **Main Class**: `com.google.zetasql.toolkit.antipattern.AntiPatternApplication` (same as web service)
- **Best For**: Data analysts, SQL-based workflows, ad-hoc analysis
- **Output**: JSON result directly in SQL queries
- **Configuration**: Uses Cloud Run Service as backend

## 📁 Demo Structure

```
demo/
├── README.md                           # This file
├── 01_setup_and_deploy.ipynb          # Main deployment notebook
├── 02_cloud_run_api_demo.ipynb        # REST API usage examples
├── 03_bigquery_udf_demo.ipynb         # BigQuery UDF usage examples
├── 04_streamlit_frontend.ipynb        # Interactive web interface
├── cloudbuild-batch.yaml              # Build config for batch processing
├── cloudbuild-service.yaml            # Build config for web service/UDF
├── config.json                        # Shared configuration
├── requirements.txt                   # Python dependencies
├── utils.py                           # Shared utility classes
├── streamlit_app.py                   # Streamlit application
└── step_9_*.py                        # API testing fixes
```

## 🚀 Quick Start

### Option 1: Complete Interactive Setup (Recommended)
Run the main deployment notebook for a guided setup:

```bash
jupyter notebook 01_setup_and_deploy.ipynb
```

This notebook will:
- Configure your Google Cloud project settings
- Check prerequisites and enable required APIs
- Build optimized containers for each deployment type
- Deploy all three scenarios with proper configuration
- Test each deployment with real examples
- Provide comprehensive usage demonstrations

### Option 2: Manual CLI Deployment

If you prefer command-line deployment, follow the instructions in [CR_DEPLOY.md](../CR_DEPLOY.md) for Cloud Run Jobs, or use the configurations below.

## 🔧 Manual Deployment Instructions

### Prerequisites

1. **Google Cloud Project** with billing enabled
2. **gcloud CLI** installed and authenticated:
   ```bash
   gcloud auth login
   gcloud auth application-default login
   ```
3. **Required APIs** enabled:
   ```bash
   gcloud services enable cloudbuild.googleapis.com run.googleapis.com \
       artifactregistry.googleapis.com bigquery.googleapis.com \
       bigqueryconnection.googleapis.com --project=YOUR_PROJECT_ID
   ```

### 1. Setup Configuration

```bash
export PROJECT_ID="your-project-id"
export REGION="us-central1"
export REPOSITORY="antipattern-registry"
export BQ_DATASET="antipattern_demo"

# Container images
export BATCH_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/antipattern-batch:latest"
export SERVICE_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/antipattern-service:latest"

# Service names
export BATCH_JOB_NAME="antipattern-batch-job"
export API_SERVICE_NAME="antipattern-api-service"
```

### 2. Create Artifact Registry Repository

```bash
gcloud artifacts repositories create $REPOSITORY \
    --repository-format=docker \
    --location=$REGION \
    --project=$PROJECT_ID
```

### 3. Build Container Images

**For Batch Processing (Cloud Run Job):**
```bash
cd .. && \
gcloud builds submit . \
    --project=$PROJECT_ID \
    --config=demo/cloudbuild-batch.yaml \
    --substitutions=_CONTAINER_IMAGE_NAME=$BATCH_IMAGE \
    --machine-type=e2-highcpu-8
```

**For API Service & UDF (Cloud Run Service):**
```bash
cd .. && \
gcloud builds submit . \
    --project=$PROJECT_ID \
    --config=demo/cloudbuild-service.yaml \
    --substitutions=_CONTAINER_IMAGE_NAME=$SERVICE_IMAGE \
    --machine-type=e2-highcpu-8
```

### 4. Deploy Cloud Run Job (Batch Processing)

```bash
# Create output table
export OUTPUT_TABLE="$PROJECT_ID.$BQ_DATASET.antipattern_batch_results"

# Deploy job
gcloud run jobs create $BATCH_JOB_NAME \
    --image=$BATCH_IMAGE \
    --max-retries=3 \
    --task-timeout=15m \
    --memory=2Gi \
    --cpu=2 \
    --args="--read_from_info_schema" \
    --args="--read_from_info_schema_days" --args="1" \
    --args="--info_schema_table_name" --args="\`region-us\`.INFORMATION_SCHEMA.JOBS" \
    --args="--processing_project_id" --args="$PROJECT_ID" \
    --args="--output_table" --args="$OUTPUT_TABLE" \
    --region=$REGION \
    --project=$PROJECT_ID

# Execute job
gcloud run jobs execute $BATCH_JOB_NAME \
    --region=$REGION \
    --project=$PROJECT_ID \
    --wait
```

### 5. Deploy Cloud Run Service (REST API)

```bash
gcloud run deploy $API_SERVICE_NAME \
    --image=$SERVICE_IMAGE \
    --region=$REGION \
    --no-allow-unauthenticated \
    --memory=2Gi \
    --cpu=2 \
    --timeout=300 \
    --port=8080 \
    --project=$PROJECT_ID

# Get service URL
export SERVICE_URL=$(gcloud run services describe $API_SERVICE_NAME \
    --region=$REGION \
    --project=$PROJECT_ID \
    --format="value(status.address.url)")

echo "Service URL: $SERVICE_URL"
```

### 6. Create BigQuery Remote UDF

```bash
# Create BigQuery dataset
bq mk --dataset \
    --project_id=$PROJECT_ID \
    --location=$REGION \
    --description="Anti-Pattern Recognition Demo" \
    $BQ_DATASET

# Create BigQuery connection
export CONNECTION_NAME="ext-$API_SERVICE_NAME"
bq mk --connection \
    --display_name='Anti-Pattern Recognition Connection' \
    --connection_type=CLOUD_RESOURCE \
    --project_id=$PROJECT_ID \
    --location=$REGION \
    $CONNECTION_NAME

# Get connection service account
export CONNECTION_SA=$(bq --project_id=$PROJECT_ID --format=json show \
    --connection $PROJECT_ID.$REGION.$CONNECTION_NAME | \
    jq -r '.cloudResource.serviceAccountId')

# Grant permissions
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$CONNECTION_SA" \
    --role='roles/run.invoker'

# Create remote function
bq query --project_id=$PROJECT_ID --use_legacy_sql=false \
"CREATE OR REPLACE FUNCTION $BQ_DATASET.get_antipatterns(query STRING)
RETURNS JSON
REMOTE WITH CONNECTION \`$PROJECT_ID.$REGION.$CONNECTION_NAME\`
OPTIONS (endpoint = '$SERVICE_URL');"
```

## 📚 Usage Examples

### Cloud Run Job (Batch Processing)
```bash
# Execute batch analysis
gcloud run jobs execute $BATCH_JOB_NAME \
    --region=$REGION \
    --project=$PROJECT_ID \
    --wait

# Check results
bq query --use_legacy_sql=false \
"SELECT * FROM \`$OUTPUT_TABLE\` LIMIT 10"
```

### REST API
```bash
# Test API endpoint
curl -X POST "$SERVICE_URL/analyze" \
    -H "Authorization: Bearer $(gcloud auth print-access-token)" \
    -H "Content-Type: application/json" \
    -d '{
        "query": "SELECT * FROM dataset.table ORDER BY column",
        "rewrite_sql": false
    }'
```

### BigQuery UDF
```sql
-- Use UDF in SQL queries
SELECT
    'SELECT * FROM dataset.table ORDER BY column' as test_query,
    antipattern_demo.get_antipatterns('SELECT * FROM dataset.table ORDER BY column') as antipatterns;

-- Analyze multiple queries
WITH sample_queries AS (
    SELECT 'SELECT * FROM table1' as query
    UNION ALL
    SELECT 'SELECT col1 FROM table2 ORDER BY col1' as query
)
SELECT
    query,
    antipattern_demo.get_antipatterns(query) as antipatterns
FROM sample_queries;
```

## 🔍 Anti-Pattern Detection Capabilities

The tool detects the following anti-patterns:

- **SimpleSelectStar**: Using `SELECT *` when specific columns would suffice
- **OrderByWithoutLimit**: Using `ORDER BY` without `LIMIT` clause
- **RegexpContains**: Using `REGEXP_CONTAINS` when `LIKE` would be more efficient
- **WhereOrder**: Inefficient ordering of WHERE clause conditions
- **IdentifyCTEsEvalMultipleTimes**: CTEs that are evaluated multiple times
- **IdentifyInSubqueryWithoutAgg**: IN subqueries without proper aggregation
- **JoinOrder**: Inefficient join ordering
- **DynamicPredicate**: Dynamic predicates that prevent optimization
- **LatestRecord**: Inefficient patterns for getting latest records
- **MissingDropStatement**: Missing DROP statements for temporary tables

## 📊 Monitoring and Maintenance

### Cloud Run Logs
```bash
# View job logs
gcloud logging read "resource.type=cloud_run_job AND resource.labels.job_name=$BATCH_JOB_NAME" \
    --project=$PROJECT_ID \
    --limit=50

# View service logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=$API_SERVICE_NAME" \
    --project=$PROJECT_ID \
    --limit=50
```

### Scheduled Execution
Set up Cloud Scheduler for automated batch processing:

```bash
export CLOUD_RUN_INVOKER_SA="your-invoker-service-account@project.iam.gserviceaccount.com"

gcloud scheduler jobs create http $BATCH_JOB_NAME-trigger \
    --location=$REGION \
    --schedule="0 5 * * *" \
    --uri="https://$REGION-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$PROJECT_ID/jobs/$BATCH_JOB_NAME:run" \
    --http-method="POST" \
    --oauth-service-account-email="$CLOUD_RUN_INVOKER_SA"
```

### Cost Monitoring
- Monitor BigQuery slot usage and query costs
- Set up billing alerts for Cloud Run usage
- Use BigQuery's cost estimation features

## 🧪 Interactive Demos

Explore the additional notebooks for hands-on examples:

1. **[02_cloud_run_api_demo.ipynb](02_cloud_run_api_demo.ipynb)**: REST API usage patterns
2. **[03_bigquery_udf_demo.ipynb](03_bigquery_udf_demo.ipynb)**: BigQuery UDF examples
3. **[04_streamlit_frontend.ipynb](04_streamlit_frontend.ipynb)**: Interactive web interface

## 🛠️ Troubleshooting

### Common Issues

1. **Authentication Errors**
   ```bash
   gcloud auth login
   gcloud auth application-default login
   ```

2. **Build Failures**
   - Ensure you're running from the correct directory
   - Check that cloudbuild files exist in demo/ directory
   - Verify Cloud Build API is enabled

3. **Permission Issues**
   - Ensure your account has necessary IAM roles
   - Check that billing is enabled on the project
   - Verify service account permissions for BigQuery connections

4. **UDF Endpoint Issues**
   - BigQuery Remote Functions don't support URL paths
   - Use base service URL only (no `/analyze` path)
   - Ensure the service has `@PostMapping("/")` endpoint

### Clean Up Resources

```bash
# Delete Cloud Run Job
gcloud run jobs delete $BATCH_JOB_NAME --region=$REGION --project=$PROJECT_ID --quiet

# Delete Cloud Run Service
gcloud run services delete $API_SERVICE_NAME --region=$REGION --project=$PROJECT_ID --quiet

# Delete BigQuery UDF
bq query --use_legacy_sql=false "DROP FUNCTION $BQ_DATASET.get_antipatterns"

# Delete BigQuery dataset
bq rm -r -d $PROJECT_ID:$BQ_DATASET

# Delete Artifact Registry repository
gcloud artifacts repositories delete $REPOSITORY --location=$REGION --project=$PROJECT_ID --quiet
```

## 📖 Additional Resources

- **[CR_DEPLOY.md](../CR_DEPLOY.md)**: Detailed Cloud Run Job deployment guide
- **[cloudbuild-udf.yaml](../cloudbuild-udf.yaml)**: Reference UDF build configuration
- **[Terraform Module](../terraform/)**: Infrastructure as Code deployment
- **[Main Documentation](../README.md)**: Complete project documentation

## 🤝 Contributing

For issues, feature requests, or contributions, please refer to the main project repository.

## 📄 License

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

---

**🎉 Ready to get started? Run `jupyter notebook 01_setup_and_deploy.ipynb` for a guided deployment experience!**
