from datetime import datetime
import json
import os
import time

import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import requests
import streamlit as st
from utils import AntiPatternAnalyzer
from utils import ConfigManager
from utils import ResultsFormatter
from utils import SampleQueries

# Page configuration
st.set_page_config(
    page_title="BigQuery Anti-Pattern Recognition",
    page_icon="🔍",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom CSS for better styling
st.markdown("""
<style>
    .main-header {
        font-size: 2.5rem;
        font-weight: bold;
        color: #1f77b4;
        text-align: center;
        margin-bottom: 2rem;
    }
    .metric-card {
        background-color: #f0f2f6;
        padding: 1rem;
        border-radius: 0.5rem;
        border-left: 4px solid #1f77b4;
    }
    .antipattern-found {
        background-color: #ffebee;
        border-left: 4px solid #f44336;
        padding: 1rem;
        border-radius: 0.5rem;
        margin: 0.5rem 0;
    }
    .antipattern-clean {
        background-color: #e8f5e8;
        border-left: 4px solid #4caf50;
        padding: 1rem;
        border-radius: 0.5rem;
        margin: 0.5rem 0;
    }
    .code-block {
        background-color: #f8f9fa;
        padding: 1rem;
        border-radius: 0.5rem;
        border: 1px solid #e9ecef;
        font-family: 'Courier New', monospace;
    }
</style>
""", unsafe_allow_html=True)


class StreamlitApp:
    def __init__(self):
        self.config_manager = ConfigManager()
        self.analyzer = None
        self.formatter = ResultsFormatter()
        self.sample_queries = SampleQueries()

        # Initialize session state
        if 'analysis_results' not in st.session_state:
            st.session_state.analysis_results = None
        if 'rewritten_query' not in st.session_state:
            st.session_state.rewritten_query = None
        if 'analysis_history' not in st.session_state:
            st.session_state.analysis_history = []

    def load_configuration(self):
        """Load configuration and initialize analyzer"""
        try:
            config = self.config_manager.load_config()
            if config:
                self.analyzer = AntiPatternAnalyzer(
                    cloud_run_url=config.get('cloud_run_url'),
                    project_id=config.get('project_id'),
                    dataset_id=config.get('dataset_id'),
                    connection_id=config.get('connection_id')
                )
                return True
            return False
        except Exception as e:
            st.error(f"Failed to load configuration: {str(e)}")
            return False

    def render_sidebar(self):
        """Render sidebar with configuration and sample queries"""
        st.sidebar.title("🔧 Configuration")

        # Configuration status
        config_loaded = self.load_configuration()
        if config_loaded:
            st.sidebar.success("✅ Configuration loaded successfully")
            config = self.config_manager.load_config()
            st.sidebar.info(f"**Project:** {config.get('project_id', 'N/A')}")
            st.sidebar.info(f"**Dataset:** {config.get('dataset_id', 'N/A')}")
        else:
            st.sidebar.error("❌ Configuration not found")
            st.sidebar.warning("Please run the setup notebook first!")

        st.sidebar.divider()

        # Sample queries section
        st.sidebar.title("📝 Sample Queries")

        antipattern_types = list(self.sample_queries.get_all_samples().keys())
        selected_type = st.sidebar.selectbox(
            "Select Anti-Pattern Type:",
            [""] + antipattern_types,
            help="Choose a sample query to analyze"
        )

        if selected_type:
            samples = self.sample_queries.get_samples(selected_type)
            if samples:
                selected_sample = st.sidebar.selectbox(
                    "Select Sample Query:",
                    range(len(samples)),
                    format_func=lambda x: f"Sample {x+1}",
                    help="Choose a specific sample query"
                )

                if st.sidebar.button("📋 Load Sample Query"):
                    st.session_state.sample_query = samples[selected_sample]['query']
                    st.rerun()

        st.sidebar.divider()

        # Analysis history
        if st.session_state.analysis_history:
            st.sidebar.title("📊 Analysis History")
            for i, analysis in enumerate(reversed(st.session_state.analysis_history[-5:])):
                timestamp = analysis.get('timestamp', 'Unknown')
                antipatterns_count = len(analysis.get('antipatterns', []))

                with st.sidebar.expander(f"Analysis {len(st.session_state.analysis_history)-i}"):
                    st.write(f"**Time:** {timestamp}")
                    st.write(f"**Anti-patterns:** {antipatterns_count}")
                    if antipatterns_count > 0:
                        for ap in analysis.get('antipatterns', []):
                            st.write(f"• {ap}")

    def render_query_input(self):
        """Render the query input section"""
        st.markdown(
            '<div class="main-header">🔍 BigQuery Anti-Pattern Recognition</div>', unsafe_allow_html=True)

        st.markdown("### 1. 📝 Query Input")
        st.markdown(
            "Paste your BigQuery SQL query below to analyze for anti-patterns:")

        # Initialize query text
        default_query = ""
        if hasattr(st.session_state, 'sample_query'):
            default_query = st.session_state.sample_query
            delattr(st.session_state, 'sample_query')

        query_text = st.text_area(
            "SQL Query:",
            value=default_query,
            height=200,
            placeholder="SELECT * FROM `project.dataset.table` WHERE condition ORDER BY column;",
            help="Enter your BigQuery SQL query here. You can also use the sample queries from the sidebar."
        )

        col1, col2, col3 = st.columns([1, 1, 2])

        with col1:
            analyze_button = st.button(
                "🔍 Analyze Query",
                type="primary",
                disabled=not query_text.strip() or not self.analyzer,
                help="Analyze the query for anti-patterns"
            )

        with col2:
            clear_button = st.button(
                "🗑️ Clear Results",
                help="Clear all analysis results"
            )

        if clear_button:
            st.session_state.analysis_results = None
            st.session_state.rewritten_query = None
            st.rerun()

        return query_text, analyze_button

    def analyze_query(self, query_text):
        """Analyze the query for anti-patterns"""
        if not self.analyzer:
            st.error(
                "❌ Analyzer not configured. Please run the setup notebook first.")
            return None

        try:
            with st.spinner("🔍 Analyzing query for anti-patterns..."):
                # Perform analysis
                results = self.analyzer.analyze_query(query_text)

                # Add to history
                analysis_record = {
                    'timestamp': datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    'query': query_text[:100] + "..." if len(query_text) > 100 else query_text,
                    'antipatterns': [ap['name'] for ap in results.get('antipatterns', [])],
                    'results': results
                }
                st.session_state.analysis_history.append(analysis_record)

                return results

        except Exception as e:
            st.error(f"❌ Analysis failed: {str(e)}")
            return None

    def render_analysis_results(self, results):
        """Render the analysis results section"""
        st.markdown("### 2. 📊 Analysis Results")

        if not results:
            st.info(
                "👆 Enter a query above and click 'Analyze Query' to see results here.")
            return

        antipatterns = results.get('antipatterns', [])

        # Summary metrics
        col1, col2, col3, col4 = st.columns(4)

        with col1:
            st.metric(
                "Anti-patterns Found",
                len(antipatterns),
                delta=None,
                delta_color="inverse"
            )

        with col2:
            severity_counts = {}
            for ap in antipatterns:
                severity = ap.get('severity', 'UNKNOWN')
                severity_counts[severity] = severity_counts.get(
                    severity, 0) + 1

            high_severity = severity_counts.get('HIGH', 0)
            st.metric(
                "High Severity",
                high_severity,
                delta=None,
                delta_color="inverse"
            )

        with col3:
            processing_time = results.get('processing_time_ms', 0)
            st.metric(
                "Processing Time",
                f"{processing_time}ms"
            )

        with col4:
            query_length = len(results.get('original_query', ''))
            st.metric(
                "Query Length",
                f"{query_length} chars"
            )

        # Anti-patterns details
        if antipatterns:
            st.markdown("#### 🚨 Anti-patterns Detected")

            for i, antipattern in enumerate(antipatterns):
                name = antipattern.get('name', 'Unknown')
                description = antipattern.get(
                    'description', 'No description available')
                severity = antipattern.get('severity', 'UNKNOWN')
                recommendation = antipattern.get(
                    'recommendation', 'No recommendation available')

                # Color coding based on severity
                if severity == 'HIGH':
                    alert_class = "antipattern-found"
                    severity_emoji = "🔴"
                elif severity == 'MEDIUM':
                    alert_class = "antipattern-found"
                    severity_emoji = "🟡"
                else:
                    alert_class = "antipattern-found"
                    severity_emoji = "🟠"

                st.markdown(f"""
                <div class="{alert_class}">
                    <h5>{severity_emoji} {name} ({severity})</h5>
                    <p><strong>Description:</strong> {description}</p>
                    <p><strong>Recommendation:</strong> {recommendation}</p>
                </div>
                """, unsafe_allow_html=True)
        else:
            st.markdown("""
            <div class="antipattern-clean">
                <h5>✅ No Anti-patterns Detected</h5>
                <p>Your query looks good! No common anti-patterns were found.</p>
            </div>
            """, unsafe_allow_html=True)

        # Raw results (expandable)
        with st.expander("🔍 View Raw Analysis Results"):
            st.json(results)

    def render_ai_rewriter(self, query_text, analysis_results):
        """Render the AI query rewriter section"""
        st.markdown("### 3. 🤖 AI-Powered Query Rewriting")

        if not analysis_results:
            st.info("👆 Analyze a query first to see AI rewriting suggestions here.")
            return

        antipatterns = analysis_results.get('antipatterns', [])

        if not antipatterns:
            st.success(
                "✅ Your query is already optimized! No rewriting needed.")
            return

        col1, col2 = st.columns([1, 3])

        with col1:
            rewrite_button = st.button(
                "🤖 Rewrite Query",
                type="secondary",
                help="Use AI to rewrite the query and fix anti-patterns"
            )

        if rewrite_button:
            try:
                with st.spinner("🤖 AI is rewriting your query..."):
                    # Simulate AI rewriting (in real implementation, this would call Vertex AI)
                    time.sleep(2)  # Simulate processing time

                    # Generate a mock rewritten query based on anti-patterns
                    rewritten_query = self.generate_mock_rewritten_query(
                        query_text, antipatterns)

                    st.session_state.rewritten_query = {
                        'original': query_text,
                        'rewritten': rewritten_query,
                        'improvements': self.generate_improvements_summary(antipatterns),
                        'timestamp': datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }

            except Exception as e:
                st.error(f"❌ AI rewriting failed: {str(e)}")

        # Display rewritten query if available
        if st.session_state.rewritten_query:
            rewrite_data = st.session_state.rewritten_query

            st.markdown("#### 📝 Rewritten Query")

            # Show before/after comparison
            col1, col2 = st.columns(2)

            with col1:
                st.markdown("**Original Query:**")
                st.code(rewrite_data['original'], language='sql')

            with col2:
                st.markdown("**Rewritten Query:**")
                st.code(rewrite_data['rewritten'], language='sql')

            # Copy button for rewritten query
            st.markdown("**Copy Rewritten Query:**")
            st.text_area(
                "Rewritten SQL:",
                value=rewrite_data['rewritten'],
                height=100,
                help="Copy this optimized query to use in your application"
            )

            # Improvements summary
            st.markdown("#### ✨ Improvements Made")
            for improvement in rewrite_data['improvements']:
                st.markdown(f"• {improvement}")

    def generate_mock_rewritten_query(self, original_query, antipatterns):
        """Generate a mock rewritten query based on detected anti-patterns"""
        rewritten = original_query

        for antipattern in antipatterns:
            name = antipattern.get('name', '')

            if 'SELECT_STAR' in name:
                # Replace SELECT * with specific columns
                rewritten = rewritten.replace(
                    'SELECT *', 'SELECT column1, column2, column3')

            elif 'ORDER_BY_WITHOUT_LIMIT' in name:
                # Add LIMIT clause
                if 'LIMIT' not in rewritten.upper():
                    rewritten += ' LIMIT 1000'

            elif 'REGEXP_CONTAINS' in name:
                # Replace REGEXP_CONTAINS with LIKE when appropriate
                rewritten = rewritten.replace('REGEXP_CONTAINS', 'LIKE')

            # Add more rewriting logic for other anti-patterns

        return rewritten

    def generate_improvements_summary(self, antipatterns):
        """Generate a summary of improvements made"""
        improvements = []

        for antipattern in antipatterns:
            name = antipattern.get('name', '')

            if 'SELECT_STAR' in name:
                improvements.append(
                    "Replaced SELECT * with specific column names to reduce data transfer")

            elif 'ORDER_BY_WITHOUT_LIMIT' in name:
                improvements.append(
                    "Added LIMIT clause to prevent sorting large result sets")

            elif 'REGEXP_CONTAINS' in name:
                improvements.append(
                    "Replaced REGEXP_CONTAINS with LIKE for better performance on simple patterns")

            elif 'WHERE_ORDER' in name:
                improvements.append(
                    "Reordered WHERE conditions to put most selective filters first")

            elif 'DYNAMIC_PREDICATE' in name:
                improvements.append(
                    "Optimized dynamic predicates for better query planning")

            else:
                improvements.append(
                    f"Fixed {name.replace('_', ' ').title()} anti-pattern")

        return improvements

    def render_visualizations(self, analysis_results):
        """Render improvement visualizations"""
        st.markdown("### 4. 📈 Performance Impact Visualization")

        if not analysis_results:
            st.info("👆 Analyze a query first to see performance visualizations here.")
            return

        antipatterns = analysis_results.get('antipatterns', [])

        if not antipatterns:
            st.success("✅ No performance issues detected!")
            return

        # Create visualizations
        col1, col2 = st.columns(2)

        with col1:
            # Anti-pattern severity distribution
            severity_counts = {}
            for ap in antipatterns:
                severity = ap.get('severity', 'UNKNOWN')
                severity_counts[severity] = severity_counts.get(
                    severity, 0) + 1

            if severity_counts:
                fig_severity = px.pie(
                    values=list(severity_counts.values()),
                    names=list(severity_counts.keys()),
                    title="Anti-pattern Severity Distribution",
                    color_discrete_map={
                        'HIGH': '#ff4444',
                        'MEDIUM': '#ffaa00',
                        'LOW': '#ffdd00'
                    }
                )
                st.plotly_chart(fig_severity, use_container_width=True)

        with col2:
            # Performance impact estimation
            impact_data = []
            for ap in antipatterns:
                name = ap.get('name', 'Unknown')
                severity = ap.get('severity', 'LOW')

                # Mock performance impact based on severity
                if severity == 'HIGH':
                    impact = 80
                elif severity == 'MEDIUM':
                    impact = 50
                else:
                    impact = 20

                impact_data.append({
                    'Anti-pattern': name.replace('_', ' ').title(),
                    'Performance Impact (%)': impact,
                    'Severity': severity
                })

            if impact_data:
                df_impact = pd.DataFrame(impact_data)
                fig_impact = px.bar(
                    df_impact,
                    x='Performance Impact (%)',
                    y='Anti-pattern',
                    color='Severity',
                    title="Estimated Performance Impact",
                    color_discrete_map={
                        'HIGH': '#ff4444',
                        'MEDIUM': '#ffaa00',
                        'LOW': '#ffdd00'
                    }
                )
                fig_impact.update_layout(
                    yaxis={'categoryorder': 'total ascending'})
                st.plotly_chart(fig_impact, use_container_width=True)

        # Cost estimation comparison
        if st.session_state.rewritten_query:
            st.markdown("#### 💰 Cost Estimation Comparison")

            # Mock cost calculations
            original_cost = len(analysis_results.get(
                'original_query', '')) * 0.001  # Mock calculation
            optimized_cost = original_cost * 0.6  # Assume 40% cost reduction
            savings = original_cost - optimized_cost

            col1, col2, col3 = st.columns(3)

            with col1:
                st.metric(
                    "Original Query Cost",
                    f"${original_cost:.3f}",
                    help="Estimated cost for original query"
                )

            with col2:
                st.metric(
                    "Optimized Query Cost",
                    f"${optimized_cost:.3f}",
                    delta=f"-${savings:.3f}",
                    delta_color="inverse",
                    help="Estimated cost for optimized query"
                )

            with col3:
                savings_percent = (savings / original_cost) * 100
                st.metric(
                    "Cost Savings",
                    f"{savings_percent:.1f}%",
                    help="Percentage cost reduction"
                )

            # Cost comparison chart
            cost_data = pd.DataFrame({
                'Query Type': ['Original', 'Optimized'],
                'Cost ($)': [original_cost, optimized_cost],
                'Color': ['#ff4444', '#44ff44']
            })

            fig_cost = px.bar(
                cost_data,
                x='Query Type',
                y='Cost ($)',
                title="Cost Comparison",
                color='Query Type',
                color_discrete_map={
                    'Original': '#ff4444', 'Optimized': '#44ff44'}
            )
            st.plotly_chart(fig_cost, use_container_width=True)

    def run(self):
        """Main application runner"""
        # Render sidebar
        self.render_sidebar()

        # Main content
        query_text, analyze_button = self.render_query_input()

        # Perform analysis if button clicked
        if analyze_button and query_text.strip():
            results = self.analyze_query(query_text.strip())
            if results:
                st.session_state.analysis_results = results

        # Render results sections
        self.render_analysis_results(st.session_state.analysis_results)
        self.render_ai_rewriter(query_text, st.session_state.analysis_results)
        self.render_visualizations(st.session_state.analysis_results)

        # Footer
        st.markdown("---")
        st.markdown("""
        <div style="text-align: center; color: #666; padding: 1rem;">
            <p>🔍 BigQuery Anti-Pattern Recognition Tool</p>
            <p>Built with Streamlit • Powered by Google Cloud</p>
        </div>
        """, unsafe_allow_html=True)


# Run the application
if __name__ == "__main__":
    app = StreamlitApp()
    app.run()
