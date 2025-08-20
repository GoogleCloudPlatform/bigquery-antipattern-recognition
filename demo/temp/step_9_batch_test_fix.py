# Step 9: Test Cloud Run Job (Batch Processing) - FIXED VERSION
# Copy this code into your Jupyter notebook cell

# Test Cloud Run Job (Batch Processing)
if config.get('deploy_batch') and config.get('batch_job_name'):
    print("🧪 Testing Cloud Run Job (Batch Processing)...")

    # Execute the job
    execute_command = f"""
    gcloud run jobs execute {config.get('batch_job_name')} \
        --region={config.get('region')} \
        --project={config.get('project_id')} \
        --wait
    """

    print("⏱️  Executing batch job... This may take a few minutes.")
    success, output = DeploymentHelper.run_command(execute_command)

    if success:
        print("✅ Batch job executed successfully!")
        print(
            f"Results should be available in: {config.get('batch_output_table')}")

        # Check if results were written - FIXED: Remove backticks to avoid escape sequence warning
        batch_output_table = config.get('batch_output_table')
        check_results_command = f"""
        bq query --project_id={config.get('project_id')} --use_legacy_sql=false \
            "SELECT COUNT(*) as result_count FROM {batch_output_table}"
        """

        success, result_output = DeploymentHelper.run_command(
            check_results_command)
        if success:
            print(f"📊 Results check: {result_output}")
        else:
            print(f"⚠️  Could not check results: {result_output}")
    else:
        print(f"❌ Batch job execution failed: {output}")
        print("\nTroubleshooting tips:")
        print("1. Check the Cloud Run logs in Google Cloud Console")
        print("2. Verify the INFORMATION_SCHEMA table name is correct")
        print("3. Ensure the service account has BigQuery permissions")
        print("4. Check if there are queries in the last 24 hours to analyze")

else:
    print("⏭️  Skipping batch job test (not deployed)")
