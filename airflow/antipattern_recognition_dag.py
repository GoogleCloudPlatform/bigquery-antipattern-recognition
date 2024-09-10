import datetime

from airflow import models
from airflow.operators import bash


YESTERDAY = datetime.datetime.now() - datetime.timedelta(days=1)

default_args = {
    "owner": "Composer Antipattern Example",
    "depends_on_past": False,
    "email": [""],
    "email_on_failure": False,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": datetime.timedelta(minutes=5),
    "start_date": YESTERDAY,
}

bash_command = '''java -jar \
    /home/airflow/gcs/plugins/bigquery-antipattern-recognition-0.1.1-SNAPSHOT-jar-with-dependencies.jar \
    --processing_project_id <processing-project-id> \
    --read_from_info_schema \
    --info_schema_region us \
    --read_from_info_schema_days 1 \
    --output_table "<project-id>.<dataset>.antipattern_output_table"  \
    --info_schema_top_n_percentage_of_jobs .01
'''
with models.DAG(
    "antipattern_recognition_dag",
    catchup=False,
    default_args=default_args,
    schedule_interval=datetime.timedelta(days=1),
) as dag:
    # Print the dag_run id from the Airflow logs
    print_dag_run_conf = bash.BashOperator(
        task_id="run_antipattern_recognition_tool", bash_command=bash_command
    )


