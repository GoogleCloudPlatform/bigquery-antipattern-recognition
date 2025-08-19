"""
Shared utilities for BigQuery Anti-Pattern Recognition Demo
"""

import json
import logging
import os
import subprocess
import time
from typing import Dict, List, Optional, Tuple

from google.auth import default
from google.cloud import bigquery
import matplotlib.pyplot as plt
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import requests
import seaborn as sns

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ConfigManager:
    """Manages configuration across notebooks"""

    def __init__(self, config_path: str = "config.json"):
        self.config_path = config_path
        self.config = {}
        self.load_config()

    def load_config(self):
        """Load configuration from file"""
        if os.path.exists(self.config_path):
            with open(self.config_path, 'r') as f:
                self.config = json.load(f)
        else:
            self.config = {}

    def save_config(self):
        """Save configuration to file"""
        with open(self.config_path, 'w') as f:
            json.dump(self.config, f, indent=2)

    def get(self, key: str, default=None):
        """Get configuration value"""
        return self.config.get(key, default)

    def set(self, key: str, value):
        """Set configuration value"""
        self.config[key] = value
        self.save_config()

    def update(self, updates: dict):
        """Update multiple configuration values"""
        self.config.update(updates)
        self.save_config()


class AntiPatternAnalyzer:
    """Main analyzer class for anti-pattern detection"""

    def __init__(self, config_manager: ConfigManager):
        self.config = config_manager
        self.bq_client = None
        self._init_bigquery_client()

    def _init_bigquery_client(self):
        """Initialize BigQuery client"""
        try:
            self.bq_client = bigquery.Client(
                project=self.config.get('project_id'))
        except Exception as e:
            logger.warning(f"Could not initialize BigQuery client: {e}")

    def call_api(self, query: str, rewrite_sql: bool = False) -> Dict:
        """Call the Cloud Run API for anti-pattern analysis"""
        service_url = self.config.get('service_url')
        if not service_url:
            raise ValueError(
                "Service URL not configured. Run setup notebook first.")

        # Get authentication token
        credentials, _ = default()
        credentials.refresh(requests.Request())
        token = credentials.token

        headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }

        payload = {
            'query': query,
            'rewrite_sql': rewrite_sql
        }

        try:
            response = requests.post(
                f'{service_url}/analyze',
                headers=headers,
                json=payload,
                timeout=30
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"API call failed: {e}")
            return {'error': str(e)}

    def call_udf(self, query: str) -> pd.DataFrame:
        """Call the BigQuery UDF for anti-pattern analysis"""
        if not self.bq_client:
            raise ValueError("BigQuery client not initialized")

        dataset = self.config.get('bq_dataset', 'antipattern_demo')

        sql = f"""
        SELECT
            '{query}' as original_query,
            {dataset}.get_antipatterns('{query}') as antipatterns
        """

        try:
            return self.bq_client.query(sql).to_dataframe()
        except Exception as e:
            logger.error(f"UDF call failed: {e}")
            return pd.DataFrame({'error': [str(e)]})

    def estimate_query_cost(self, query: str) -> Dict:
        """Estimate query cost using dry run"""
        if not self.bq_client:
            return {'error': 'BigQuery client not initialized'}

        job_config = bigquery.QueryJobConfig(
            dry_run=True, use_query_cache=False)

        try:
            job = self.bq_client.query(query, job_config=job_config)

            # Calculate estimated cost (approximate)
            bytes_processed = job.total_bytes_processed
            cost_per_tb = 5.0  # USD per TB
            estimated_cost = (bytes_processed / (1024**4)) * cost_per_tb

            return {
                'bytes_processed': bytes_processed,
                'estimated_cost_usd': estimated_cost,
                'gb_processed': bytes_processed / (1024**3)
            }
        except Exception as e:
            return {'error': str(e)}


class ResultsFormatter:
    """Formats and displays analysis results"""

    @staticmethod
    def parse_antipatterns(response: Dict) -> List[Dict]:
        """Parse anti-patterns from API response"""
        if 'error' in response:
            return [{'name': 'Error', 'description': response['error']}]

        antipatterns = []
        if 'antipatterns' in response:
            for ap in response['antipatterns']:
                antipatterns.append({
                    'name': ap.get('name', 'Unknown'),
                    'description': ap.get('result', ap.get('description', 'No description')),
                    'severity': ap.get('severity', 'Medium')
                })

        return antipatterns

    @staticmethod
    def format_results_table(antipatterns: List[Dict]) -> pd.DataFrame:
        """Format anti-patterns as a pandas DataFrame"""
        if not antipatterns:
            return pd.DataFrame({'Message': ['No anti-patterns detected']})

        df = pd.DataFrame(antipatterns)
        return df

    @staticmethod
    def create_antipattern_chart(antipatterns: List[Dict]) -> go.Figure:
        """Create a chart showing anti-pattern distribution"""
        if not antipatterns:
            fig = go.Figure()
            fig.add_annotation(text="No anti-patterns detected",
                               xref="paper", yref="paper",
                               x=0.5, y=0.5, showarrow=False)
            return fig

        df = pd.DataFrame(antipatterns)

        # Count anti-patterns by name
        counts = df['name'].value_counts()

        fig = px.pie(
            values=counts.values,
            names=counts.index,
            title="Anti-Pattern Distribution"
        )

        return fig

    @staticmethod
    def create_cost_comparison(original_cost: Dict, optimized_cost: Dict) -> go.Figure:
        """Create cost comparison chart"""
        categories = ['Original Query', 'Optimized Query']

        original_gb = original_cost.get('gb_processed', 0)
        optimized_gb = optimized_cost.get('gb_processed', 0)

        fig = go.Figure(data=[
            go.Bar(
                x=categories,
                y=[original_gb, optimized_gb],
                marker_color=['red', 'green'],
                text=[f'{original_gb:.2f} GB', f'{optimized_gb:.2f} GB'],
                textposition='auto'
            )
        ])

        fig.update_layout(
            title="Data Processed Comparison",
            yaxis_title="GB Processed",
            showlegend=False
        )

        return fig


class DeploymentHelper:
    """Helper functions for deployment tasks"""

    @staticmethod
    def run_command(command: str, cwd: str = None) -> Tuple[bool, str]:
        """Run a shell command and return success status and output"""
        try:
            result = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True,
                cwd=cwd,
                timeout=300  # 5 minute timeout
            )

            success = result.returncode == 0
            output = result.stdout if success else result.stderr

            return success, output
        except subprocess.TimeoutExpired:
            return False, "Command timed out"
        except Exception as e:
            return False, str(e)

    @staticmethod
    def check_gcloud_auth() -> bool:
        """Check if gcloud is authenticated"""
        success, output = DeploymentHelper.run_command(
            "gcloud auth list --filter=status:ACTIVE --format='value(account)'")
        return success and output.strip() != ""

    @staticmethod
    def check_apis_enabled(project_id: str, apis: List[str]) -> Dict[str, bool]:
        """Check if required APIs are enabled"""
        results = {}

        for api in apis:
            command = f"gcloud services list --enabled --filter='name:{api}' --format='value(name)' --project={project_id}"
            success, output = DeploymentHelper.run_command(command)
            results[api] = success and api in output

        return results

    @staticmethod
    def wait_for_deployment(service_name: str, region: str, project_id: str, max_wait: int = 300) -> bool:
        """Wait for Cloud Run service to be ready"""
        start_time = time.time()

        while time.time() - start_time < max_wait:
            command = f"gcloud run services describe {service_name} --region={region} --project={project_id} --format='value(status.conditions[0].status)'"
            success, output = DeploymentHelper.run_command(command)

            if success and 'True' in output:
                return True

            time.sleep(10)

        return False


class SampleQueries:
    """Sample queries for demonstration"""

    QUERIES = {
        'select_star': {
            'name': 'SELECT * Anti-Pattern',
            'query': "SELECT * FROM `bigquery-public-data.samples.shakespeare` LIMIT 100",
            'description': 'Selecting all columns when only specific columns are needed',
            'expected_antipatterns': ['SimpleSelectStar']
        },
        'order_without_limit': {
            'name': 'ORDER BY without LIMIT',
            'query': """
                SELECT word, COUNT(*) as word_count
                FROM `bigquery-public-data.samples.shakespeare`
                GROUP BY word
                ORDER BY word_count DESC
            """,
            'description': 'Using ORDER BY without LIMIT can be expensive',
            'expected_antipatterns': ['OrderByWithoutLimit']
        },
        'regexp_misuse': {
            'name': 'REGEXP_CONTAINS Misuse',
            'query': """
                SELECT word
                FROM `bigquery-public-data.samples.shakespeare`
                WHERE REGEXP_CONTAINS(word, '.*love.*')
                LIMIT 100
            """,
            'description': 'Using REGEXP_CONTAINS when LIKE would suffice',
            'expected_antipatterns': ['RegexpContains']
        },
        'where_order': {
            'name': 'WHERE Clause Ordering',
            'query': """
                SELECT *
                FROM `bigquery-public-data.github_repos.files`
                WHERE ref LIKE '%master%'
                  AND repo_name = 'tensorflow/tensorflow'
                LIMIT 100
            """,
            'description': 'Less selective filter (LIKE) appears before more selective filter',
            'expected_antipatterns': ['WhereOrder']
        },
        'multiple_ctes': {
            'name': 'Multiple CTE References',
            'query': """
                WITH base_data AS (
                    SELECT word, corpus, word_count
                    FROM `bigquery-public-data.samples.shakespeare`
                    WHERE word_count > 5
                ),
                word_stats AS (
                    SELECT corpus, COUNT(*) as word_count
                    FROM base_data
                    GROUP BY corpus
                ),
                corpus_stats AS (
                    SELECT corpus, AVG(word_count) as avg_words
                    FROM base_data
                    GROUP BY corpus
                )
                SELECT w.corpus, w.word_count, c.avg_words
                FROM word_stats w
                JOIN corpus_stats c ON w.corpus = c.corpus
                LIMIT 100
            """,
            'description': 'CTE referenced multiple times, causing re-evaluation',
            'expected_antipatterns': ['IdentifyCTEsEvalMultipleTimes']
        },
        'in_subquery_without_agg': {
            'name': 'IN Subquery without Aggregation',
            'query': """
                SELECT word
                FROM `bigquery-public-data.samples.shakespeare`
                WHERE corpus IN (
                    SELECT corpus
                    FROM `bigquery-public-data.samples.shakespeare`
                    WHERE word_count > 100
                )
                LIMIT 100
            """,
            'description': 'Using IN with subquery that should have DISTINCT',
            'expected_antipatterns': ['IdentifyInSubqueryWithoutAgg']
        }
    }

    @classmethod
    def get_query(cls, key: str) -> Dict:
        """Get a sample query by key"""
        return cls.QUERIES.get(key, {})

    @classmethod
    def get_all_queries(cls) -> Dict:
        """Get all sample queries"""
        return cls.QUERIES

    @classmethod
    def get_query_list(cls) -> List[str]:
        """Get list of available query keys"""
        return list(cls.QUERIES.keys())


def display_progress(current: int, total: int, description: str = ""):
    """Display progress bar (for use in notebooks)"""
    percentage = (current / total) * 100
    bar_length = 50
    filled_length = int(bar_length * current // total)
    bar = '█' * filled_length + '-' * (bar_length - filled_length)

    print(f'\r{description} |{bar}| {percentage:.1f}% ({current}/{total})',
          end='', flush=True)

    if current == total:
        print()  # New line when complete


def format_sql(sql: str) -> str:
    """Format SQL for better display"""
    try:
        import sqlparse
        return sqlparse.format(sql, reindent=True, keyword_case='upper')
    except ImportError:
        return sql


def create_diff_view(original: str, optimized: str) -> str:
    """Create a simple diff view of two SQL queries"""
    original_lines = original.strip().split('\n')
    optimized_lines = optimized.strip().split('\n')

    diff_html = "<div style='display: flex;'>"
    diff_html += "<div style='width: 50%; padding: 10px; background-color: #ffe6e6;'>"
    diff_html += "<h4>Original Query</h4>"
    diff_html += f"<pre>{original}</pre>"
    diff_html += "</div>"
    diff_html += "<div style='width: 50%; padding: 10px; background-color: #e6ffe6;'>"
    diff_html += "<h4>Optimized Query</h4>"
    diff_html += f"<pre>{optimized}</pre>"
    diff_html += "</div>"
    diff_html += "</div>"

    return diff_html
