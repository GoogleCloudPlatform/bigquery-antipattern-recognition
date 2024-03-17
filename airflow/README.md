### Build the project 
```bash
 # in cloud shell terminal
git clone https://github.com/GoogleCloudPlatform/bigquery-antipattern-recognition.git
cd bigquery-antipattern-recognition
mvn clean package jib:dockerBuild -DskipTests
```

### Create composer env (optional if you already have a composer env)
```bash
export PROJECT_ID="" 
export REGION="us-central1" 
export COMPOSER_ENV_NAME="example-environment"

gcloud composer environments create $PROJECT_ID \
  --location $REGION \
  --image-version composer-2.6.4-airflow-2.6.3

```

### Uploads java binary to Composer env
```bash
export PATH_TO_COMPOSER_ENV_PLUGINS=$(gcloud composer environments describe $COMPOSER_ENV_NAME \
  --location $REGION \
  --format="get(config.dagGcsPrefix)" | sed 's/\/dags/\/plugins/')

gsutil cp bigquery-antipattern-recognition/target/bigquery-antipattern-recognition-0.1.1-SNAPSHOT-jar-with-dependencies.jar ${PATH_TO_COMPOSER_ENV_PLUGINS}/bigquery-antipattern-recognition-0.1.1-SNAPSHOT-jar-with-dependencies.jar
```

#### Upload quickstart dag to composer env
```bash
gcloud composer environments storage dags import \
  --environment $COMPOSER_ENV_NAME  \
  --location $REGION \
  --source antipattern_recognition_dag.py
```

#### Run dag
For information on manually triggering the DAG and visualizing logs refer to the [View the DAG in the Airflow UI](https://cloud.google.com/composer/docs/composer-2/run-apache-airflow-dag#view_the_dag_in_the_airflow_ui) documentation. 

#### Configuring antipattern input output in DAG
The [antipattern_recognition_dag.py](./antipattern_recognition_dag.py) runs a bash command that runs the java jar with the anti pattern tool.

This happens in the following part of the code.

```python
with models.DAG(
    "antipattern_recognition_dag",
    catchup=False,
    default_args=default_args,
    schedule_interval=datetime.timedelta(days=1),
) as dag:
    # Print the dag_run id from the Airflow logs
    print_dag_run_conf = bash.BashOperator(
        task_id="run_antipattern_recognition_tool", bash_command='java -jar /home/airflow/gcs/plugins/bigquery-antipattern-recognition-0.1.1-SNAPSHOT-jar-with-dependencies.jar --query "select * from table1"')
```

You can edit the last line of this section with the bash command string and add arguments to specify input/output. See [Flags and arguments](../README.md#flags-and-arguments) documentation.
