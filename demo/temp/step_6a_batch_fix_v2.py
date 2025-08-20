# Step 6A: Deploy Cloud Run Job (Batch Processing) - FIXED VERSION V2
# Copy this code into your Jupyter notebook cell

if config.get('deploy_batch'):
    print("🚀 Deploying Cloud Run Job for batch processing...")

    # Create output table for batch results
    output_table = f"{config.get('project_id')}.{config.get('bq_dataset')}.antipattern_batch_results"

    # Check if job already exists
    check_job_command = f"""
    gcloud run jobs describe {config.get('batch_service_name')} \
        --region={config.get('region')} \
        --project={config.get('project_id')} \
        --format="value(metadata.name)"
    """

    job_exists, _ = DeploymentHelper.run_command(check_job_command)

    if job_exists:
        print(
            f"⚠️  Cloud Run Job '{config.get('batch_service_name')}' already exists.")
        print("Deleting existing job and creating new one...")

        # Delete existing job
        delete_command = f"""
        gcloud run jobs delete {config.get('batch_service_name')} \
            --region={config.get('region')} \
            --project={config.get('project_id')} \
            --quiet
        """

        delete_success, delete_output = DeploymentHelper.run_command(
            delete_command)
        if delete_success:
            print("✅ Existing job deleted successfully!")
        else:
            print(f"⚠️  Could not delete existing job: {delete_output}")

    # FIXED: Use proper INFORMATION_SCHEMA table name without backticks
    # The backticks are causing issues when passed through command line arguments
    project_id = config.get('project_id')

    # Use the format that works with the BigQuery client
    info_schema_table = f"{project_id}.region-us.INFORMATION_SCHEMA.JOBS"

    batch_deploy_command = f"""
    gcloud run jobs create {config.get('batch_service_name')} \
        --image={config.get('batch_container_image')} \
        --max-retries=3 \
        --task-timeout=15m \
        --memory=2Gi \
        --cpu=2 \
        --args="--read_from_info_schema" \
        --args="--read_from_info_schema_days" --args="1" \
        --args="--info_schema_table_name" --args="{info_schema_table}" \
        --args="--processing_project_id" --args="{project_id}" \
        --args="--output_table" --args="{output_table}" \
        --region={config.get('region')} \
        --project={project_id}
    """

    success, output = DeploymentHelper.run_command(batch_deploy_command)

    if success:
        print("✅ Cloud Run Job deployed successfully!")
        config.set('batch_job_name', config.get('batch_service_name'))
        config.set('batch_output_table', output_table)
        print(f"Job Name: {config.get('batch_service_name')}")
        print(f"Output Table: {output_table}")
        print(f"INFORMATION_SCHEMA Table: {info_schema_table}")
    else:
        print(f"❌ Batch job deployment failed: {output}")
        print("\nTroubleshooting tips:")
        print("1. Check that the container image was built successfully")
        print("2. Verify Cloud Run API is enabled")
        print("3. Ensure sufficient IAM permissions")
        print("4. Check the Cloud Run logs in Google Cloud Console")

else:
    print("⏭️  Skipping Cloud Run Job deployment (not selected)")
