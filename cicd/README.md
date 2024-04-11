# Using BigQuery AntiPattern Tool in CI/CD

## Overview

One option to deploy the BigQuery Anti Pattern Recognition tool is to run it as part of a CI/CD process to check SQL files present in a Github repository. The following example assumes all SQL files are present in one folder.

In order to deploy the tool for CI/CD, you'll need to:
* Package the tool into a container and push it to Artifact Registry.
* Create a trigger in Cloud Build which is connected to your Github repository.
* Modify the sample cloudbuild.yaml present in this folder and add it to your repository at the root level. 

## Package the tool into a container and push it to Artifact Registry

1. Set environment variables

```bash
export PROJECT_ID=""  # Project ID where resources are created
export REGION="us-central1"  # Region for Artifact Registry
export REPOSITORY="bigquery-antipattern-recognition"  # Artifact Registry repository name
export CONTAINER_IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/recognizer:0.1.1-SNAPSHOT"
```
1. Create an Artifact Registry Repository, if necessary

    ``` bash
    gcloud artifacts repositories create $REPOSITORY \
        --repository-format=docker \
        --location=$REGION \
        --project=$PROJECT_ID
    ```

2. Build and push the container image to Artifact Registry

    ``` bash
    gcloud auth configure-docker $REGION-docker.pkg.dev

    mvn clean package jib:build \
        -DskipTests \
        -Djib.to.image=$CONTAINER_IMAGE
    ```

## Create a trigger in Cloud Build which is connected to your Github repo

Follow the steps outlined [here](https://cloud.google.com/build/docs/automating-builds/create-manage-triggers#build_trigger) to create a trigger in your console, linking your Github repository. 

Included files (optional): Changes affecting at least one of these files will invoke a build. You can use glob strings to specify multiple files with wildcard characters. Acceptable wildcard characters include the characters supported by Go Match, **, and alternation.

Example: enter input/** if your SQL files are present in the input/ folder.

(Optional) Enable the option to "Send build logs to Github"

## Modify the cloudbuild.yaml sample and add it to your repo.

** Important: Grant your Cloud Build service account the `Artifact Registry Reader` role before continuing.

```bash
gcloud iam roles grant roles/artifactregistry.reader \
    serviceAccount:<PROJECT_ID>@cloudbuild.iam.gserviceaccount.com 
```

### Public Github Repositories
If your Github repository is public, copy the cloudbuild_public_repo.yaml into the root of your directory, rename it to `cloudbuild.yaml` and replace the following variables:

    ${GITHUB_REPOSITORY} = the git repository you want to clone into in the form: https://github.com/{github_username}/{repository_name}
    ${GITHUB_BRANCH} = the branch of the git repsoitory you want to clone into.
    ${INPUT_FOLDER} = the folder in the repository which contains the SQL files to be scanned.
    ${ANTIPATTERN_IMAGE} = the Artifactory image name of the antipattern tool which you built in step 1.

### Private Github Repositories

If your Github repository is private, copy the cloudbuild_private_repo.yaml into the root of your directory, rename it to `cloudbuild.yaml`.

Please follow the instructions [here](https://cloud.google.com/build/docs/access-github-from-build) to store your private SSH key in Secret Manager, public SSH key to your private repository's deploy keys, as well as grant your Cloud Build Service account the 'Secret Manager Accessor' role. In addition to the steps mentioned, create an additional secret to store your known_hosts.github file for builds to trigger successfully. 

Replace the following variables in cloud_build_private repo:

    ${GITHUB_REPOSITORY} = the git repository you want to clone into in the form: git@github.com:{github_username}/{repository_name}
    ${GITHUB_BRANCH} = the branch of the git repsoitory you want to clone into.
    ${NPUT_FOLDER} = the folder in the repository which contains the SQL files to be scanned.
    ${ANTIPATTERN_IMAGE} = the Artifactory image name of the antipattern tool which you built in step 1.
    ${PROJECT_ID} = GCP project ID
    ${PRIVATE_SSH_SECRET} = Name of the secret in GCP used to store your private SSH key
    ${KNOWN_HOSTS_SECRET} = Name of the secret in GCP used to store your known_hosts.github
