# 🔍 BigQuery Anti-Pattern Recognition - Demo Package

A comprehensive demonstration package for the BigQuery Anti-Pattern Recognition tool, featuring interactive Jupyter notebooks, a Streamlit web application, and complete deployment automation.

## 📋 Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Package Contents](#package-contents)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage Guide](#usage-guide)
- [Deployment Options](#deployment-options)
- [Troubleshooting](#troubleshooting)
- [Advanced Configuration](#advanced-configuration)
- [Contributing](#contributing)

## 🎯 Overview

This demo package provides multiple ways to interact with the BigQuery Anti-Pattern Recognition tool:

### 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Demo Package Architecture                │
├─────────────────────────────────────────────────────────────┤
│  📓 Jupyter Notebooks  │  🎨 Streamlit Frontend            │
│  • Setup & Deploy      │  • Web-based interface            │
│  • API Testing         │  • Query analysis                 │
│  • UDF Integration     │  • AI rewriting                   │
│  • Frontend Deploy     │  • Visualizations                 │
├─────────────────────────────────────────────────────────────┤
│  ☁️  Cloud Run API     │  📊 BigQuery UDF                  │
│  • REST endpoints      │  • Direct SQL integration         │
│  • Batch processing    │  • Remote function calls          │
│  • Scalable service    │  • Native BigQuery experience     │
└─────────────────────────────────────────────────────────────┘
```

### 🎯 Key Features

- **🚀 One-Click Setup**: Automated deployment with interactive notebooks
- **🔄 Multiple Interfaces**: Choose between API, UDF, or web frontend
- **📊 Rich Visualizations**: Performance impact charts and cost analysis
- **🤖 AI Integration**: Automated query rewriting with Vertex AI
- **📈 Monitoring**: Built-in analytics and usage tracking
- **🔧 Customizable**: Easy to extend and modify for specific needs

## ⚡ Quick Start

### 1. Clone and Setup

```bash
# Clone the repository
git clone https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition.git
cd bigquery-antipattern-recognition/demo

# Install dependencies
pip install -r requirements.txt

# Start Jupyter
jupyter notebook
```

### 2. Run the Notebooks (Recommended)

Execute the notebooks in order:

1. **📓 01_setup_and_deploy.ipynb** - Deploy the Cloud Run service
2. **📓 02_cloud_run_api_demo.ipynb** - Test the REST API
3. **📓 03_bigquery_udf_demo.ipynb** - Test the BigQuery UDF
4. **📓 04_streamlit_frontend.ipynb** - Deploy the web frontend

### 3. Alternative: Direct Streamlit Launch

```bash
# Quick start with Streamlit (requires existing deployment)
streamlit run streamlit_app.py
```

## 📦 Package Contents

### 📁 Core Files

| File | Description | Purpose |
|------|-------------|---------|
| `requirements.txt` | Python dependencies | Package management |
| `utils.py` | Shared utility classes | Common functionality |
| `streamlit_app.py` | Web application | User-friendly interface |

### 📓 Interactive Notebooks

| Notebook | Description | Duration |
|----------|-------------|----------|
| `01_setup_and_deploy.ipynb` | Complete setup and Cloud Run deployment | ~15 minutes |
| `02_cloud_run_api_demo.ipynb` | REST API testing and batch analysis | ~10 minutes |
| `03_bigquery_udf_demo.ipynb` | BigQuery UDF integration and testing | ~10 minutes |
| `04_streamlit_frontend.ipynb` | Web frontend deployment and usage | ~10 minutes |

### 🔧 Generated Files

| File | Description | Created By |
|------|-------------|------------|
| `config.json` | Configuration storage | Setup notebook |
| `Dockerfile` | Container definition | Setup notebook |
| `cloudbuild.yaml` | Build configuration | Setup notebook |
| `Dockerfile.streamlit` | Streamlit container | Frontend notebook |

## 📋 Prerequisites

### 🔑 Required Permissions

Your Google Cloud account needs these IAM roles:

- **Cloud Run Admin** - Deploy and manage services
- **Cloud Build Editor** - Build container images
- **BigQuery Admin** - Create datasets and remote functions
- **Artifact Registry Admin** - Manage container images
- **Service Account Admin** - Create service accounts

### 🛠️ Required Tools

- **Python 3.8+** with pip
- **Google Cloud SDK** (gcloud CLI)
- **Docker** (for local development)
- **Jupyter Notebook** or JupyterLab

### ☁️ Google Cloud Setup

```bash
# Install Google Cloud SDK
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Authenticate and set project
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud auth application-default login

# Enable required APIs
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable bigquery.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

## 🚀 Installation

### 1. Environment Setup

```bash
# Create virtual environment (recommended)
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Verify installation
python -c "import google.cloud.bigquery; print('✅ BigQuery client ready')"
python -c "import streamlit; print('✅ Streamlit ready')"
```

### 2. Google Cloud Configuration

```bash
# Set your project ID
export PROJECT_ID="your-project-id"
gcloud config set project $PROJECT_ID

# Create a dataset for the demo
bq mk --dataset $PROJECT_ID:antipattern_demo

# Verify setup
gcloud config list
bq ls
```

## 📖 Usage Guide

### 🎯 Scenario 1: Complete Setup (New Users)

**Goal**: Set up everything from scratch

1. **Start with Setup Notebook**
   ```bash
   jupyter notebook 01_setup_and_deploy.ipynb
   ```

2. **Follow the Interactive Workflow**
   - Configure your Google Cloud project
   - Deploy the Cloud Run service
   - Create BigQuery remote function
   - Test the deployment

3. **Proceed to Testing**
   - Run notebook 2 for API testing
   - Run notebook 3 for UDF testing
   - Run notebook 4 for frontend deployment

### 🎯 Scenario 2: API Integration (Developers)

**Goal**: Integrate the API into existing applications

1. **Quick API Setup**
   ```bash
   jupyter notebook 02_cloud_run_api_demo.ipynb
   ```

2. **Test API Endpoints**
   ```python
   import requests

   # Analyze a query
   response = requests.post(
       "https://your-service-url/analyze",
       json={"query": "SELECT * FROM table"}
   )
   print(response.json())
   ```

3. **Batch Processing**
   ```python
   # Analyze multiple queries
   queries = ["SELECT * FROM table1", "SELECT col FROM table2 ORDER BY col"]
   for query in queries:
       result = analyze_query(query)
       print(f"Anti-patterns found: {len(result['antipatterns'])}")
   ```

### 🎯 Scenario 3: BigQuery Integration (Data Analysts)

**Goal**: Use anti-pattern detection directly in BigQuery

1. **Setup UDF**
   ```bash
   jupyter notebook 03_bigquery_udf_demo.ipynb
   ```

2. **Use in BigQuery Console**
   ```sql
   -- Analyze a query using the UDF
   SELECT `your-project.your-dataset.analyze_query`(
     'SELECT * FROM `bigquery-public-data.samples.shakespeare` ORDER BY word'
   ) AS analysis_result;
   ```

3. **Batch Analysis**
   ```sql
   -- Analyze queries from INFORMATION_SCHEMA
   SELECT
     query,
     `your-project.your-dataset.analyze_query`(query) AS analysis
   FROM `your-project.region-us`.INFORMATION_SCHEMA.JOBS_BY_PROJECT
   WHERE creation_time >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 DAY)
     AND job_type = 'QUERY'
     AND state = 'DONE';
   ```

### 🎯 Scenario 4: Executive Demo (Stakeholders)

**Goal**: Demonstrate tool capabilities with visual interface

1. **Launch Streamlit App**
   ```bash
   jupyter notebook 04_streamlit_frontend.ipynb
   # Or directly: streamlit run streamlit_app.py
   ```

2. **Demo Flow**
   - Show sample queries from sidebar
   - Demonstrate anti-pattern detection
   - Highlight cost savings potential
   - Show AI rewriting capabilities
   - Display performance visualizations

3. **Key Talking Points**
   - Automated query optimization
   - Cost reduction potential
   - Developer productivity gains
   - Scalable cloud deployment

## 🚀 Deployment Options

### 🖥️ Local Development

```bash
# Run Streamlit locally
streamlit run streamlit_app.py --server.port 8501

# Access at http://localhost:8501
```

### ☁️ Cloud Run Deployment

```bash
# Build and deploy (automated in notebooks)
gcloud builds submit --tag gcr.io/$PROJECT_ID/bq-antipattern-frontend
gcloud run deploy bq-antipattern-frontend \
  --image gcr.io/$PROJECT_ID/bq-antipattern-frontend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

### 🔒 Secure Deployment

```bash
# Deploy with authentication
gcloud run deploy bq-antipattern-frontend \
  --image gcr.io/$PROJECT_ID/bq-antipattern-frontend \
  --platform managed \
  --region us-central1 \
  --no-allow-unauthenticated

# Add IAM policy for specific users
gcloud run services add-iam-policy-binding bq-antipattern-frontend \
  --member="user:user@example.com" \
  --role="roles/run.invoker" \
  --region us-central1
```

## 🔧 Troubleshooting

### ❌ Common Issues

#### 1. Authentication Errors

```bash
# Problem: "Could not automatically determine credentials"
# Solution: Set up application default credentials
gcloud auth application-default login

# Problem: "Permission denied"
# Solution: Check IAM roles
gcloud projects get-iam-policy $PROJECT_ID
```

#### 2. API Deployment Failures

```bash
# Problem: Cloud Build fails
# Solution: Enable APIs and check quotas
gcloud services enable cloudbuild.googleapis.com
gcloud compute project-info describe --project=$PROJECT_ID

# Problem: Container build timeout
# Solution: Increase build timeout
gcloud builds submit --timeout=1200s --tag gcr.io/$PROJECT_ID/service-name
```

#### 3. BigQuery UDF Issues

```sql
-- Problem: "Function not found"
-- Solution: Check function exists and permissions
SELECT routine_name, routine_type
FROM `your-project.your-dataset.INFORMATION_SCHEMA.ROUTINES`
WHERE routine_name = 'analyze_query';

-- Problem: "Remote function call failed"
-- Solution: Check Cloud Run service status
```

#### 4. Streamlit App Issues

```bash
# Problem: Import errors
# Solution: Install missing dependencies
pip install -r requirements.txt

# Problem: Configuration not found
# Solution: Run setup notebook first
jupyter notebook 01_setup_and_deploy.ipynb

# Problem: Port already in use
# Solution: Use different port
streamlit run streamlit_app.py --server.port 8502
```

### 🔍 Debug Mode

Enable detailed logging:

```python
import logging
logging.basicConfig(level=logging.DEBUG)

# In notebooks, add debug output
import os
os.environ['DEBUG'] = 'true'
```

### 📞 Getting Help

1. **Check the logs**:
   ```bash
   # Cloud Run logs
   gcloud logs read --service=your-service-name --limit=50

   # Cloud Build logs
   gcloud builds list --limit=10
   ```

2. **Verify configuration**:
   ```python
   # In Python/Jupyter
   from utils import ConfigManager
   config = ConfigManager().load_config()
   print(config)
   ```

3. **Test connectivity**:
   ```bash
   # Test Cloud Run service
   curl -X POST https://your-service-url/health

   # Test BigQuery connection
   bq query --use_legacy_sql=false "SELECT 1"
   ```

## ⚙️ Advanced Configuration

### 🎛️ Customization Options

#### 1. Modify Anti-Pattern Detection

Edit the detection rules in the main repository:

```java
// Add custom anti-pattern visitor
public class CustomAntiPatternVisitor extends AntiPatternVisitor {
    // Your custom logic here
}
```

#### 2. Extend Streamlit Interface

```python
# Add custom visualizations to streamlit_app.py
def custom_visualization(data):
    # Your custom Plotly/matplotlib code
    pass
```

#### 3. Configure AI Rewriting

```python
# Modify utils.py to use different AI models
class AntiPatternAnalyzer:
    def __init__(self, ai_model="gemini-pro"):
        self.ai_model = ai_model
```

### 🔧 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PROJECT_ID` | Google Cloud project ID | Required |
| `REGION` | Deployment region | `us-central1` |
| `SERVICE_NAME` | Cloud Run service name | `bq-antipattern-recognition` |
| `DATASET_ID` | BigQuery dataset | `antipattern_demo` |
| `DEBUG` | Enable debug logging | `false` |

### 📊 Monitoring Setup

```bash
# Enable monitoring
gcloud services enable monitoring.googleapis.com

# Create custom metrics
gcloud logging metrics create query_analysis_count \
  --description="Number of queries analyzed" \
  --log-filter='resource.type="cloud_run_revision" AND textPayload:"Query analyzed"'
```

### 🔄 CI/CD Integration

```yaml
# .github/workflows/deploy.yml
name: Deploy Demo Package
on:
  push:
    branches: [main]
    paths: [demo/**]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy to Cloud Run
        run: |
          gcloud builds submit --config demo/cloudbuild.yaml
```

## 🤝 Contributing

### 📝 Development Guidelines

1. **Code Style**: Follow PEP 8 for Python code
2. **Documentation**: Update README for any new features
3. **Testing**: Test all notebooks before submitting
4. **Versioning**: Use semantic versioning for releases

### 🔄 Making Changes

```bash
# Create feature branch
git checkout -b feature/new-visualization

# Make changes and test
jupyter notebook demo/test_changes.ipynb

# Commit and push
git add .
git commit -m "Add new visualization feature"
git push origin feature/new-visualization
```

### 🧪 Testing

```bash
# Test all notebooks
jupyter nbconvert --execute demo/*.ipynb

# Test Streamlit app
streamlit run demo/streamlit_app.py &
curl http://localhost:8501/health
```

## 📚 Additional Resources

### 📖 Documentation

- [BigQuery Anti-Pattern Recognition](../README.md) - Main project documentation
- [BigQuery Best Practices](https://cloud.google.com/bigquery/docs/best-practices) - Google Cloud documentation
- [Cloud Run Documentation](https://cloud.google.com/run/docs) - Deployment platform
- [Streamlit Documentation](https://docs.streamlit.io/) - Web framework

### 🎓 Learning Resources

- [BigQuery SQL Reference](https://cloud.google.com/bigquery/docs/reference/standard-sql)
- [Cloud Run Tutorials](https://cloud.google.com/run/docs/tutorials)
- [Jupyter Notebook Best Practices](https://jupyter-notebook.readthedocs.io/)
- [Python Data Science Handbook](https://jakevdp.github.io/PythonDataScienceHandbook/)

### 🔗 Related Projects

- [BigQuery Utils](https://github.com/GoogleCloudPlatform/bigquery-utils)
- [Cloud Run Samples](https://github.com/GoogleCloudPlatform/cloud-run-samples)
- [Streamlit Gallery](https://streamlit.io/gallery)

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.

## 🙏 Acknowledgments

- Google Cloud BigQuery team for the anti-pattern recognition engine
- Streamlit team for the excellent web framework
- Jupyter team for the interactive notebook platform
- Contributors and users of this demo package

---

**🌟 Happy querying with BigQuery Anti-Pattern Recognition! 🚀**

For questions, issues, or contributions, please visit our [GitHub repository](https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition).
