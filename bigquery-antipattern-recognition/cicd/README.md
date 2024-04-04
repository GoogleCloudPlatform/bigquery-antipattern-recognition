# Using BigQuery AntiPattern Tool in CI/CD

## Overview

One option to deploy the BigQuery Anti Pattern Recognition tool is to run it as part of a CI/CD process to check SQL files present in a folder inside a code base.

In order to deploy the tool to Cloud Run Jobs, you'll need to:
* Package the tool into a container and push it to Artifact Registry.
* Create a trigger in Cloud Build which is connected to your Github repo.
* Modify the sample cloudbuild.yaml present in this folder and add it to your repo. 

## Package the tool into a container and push it to Artifact Registry

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

1. Follow the steps outlined (here)[https://cloud.google.com/build/docs/automating-builds/create-manage-triggers#build_trigger] to create a trigger in your console, linking your Github repository. 

2. Event: Select the repository event to invoke your trigger.

Push to a branch: Set your trigger to start a build on commits to a particular branch.

Push new tag: Set your trigger to start a build on commits that contain a particular tag.

Pull request: Set your trigger to start a build on commits to a pull request.

3. Included files (optional): Changes affecting at least one of these files will invoke a build. You can use glob strings to specify multiple files with wildcard characters. Acceptable wildcard characters include the characters supported by Go Match, **, and alternation.

Example: enter input/** if your SQL files are present in the input/ folder.

## Modify the sample cloudbuild.yaml present in this folder and add it to your repo. 

1. Replace the following variables:

    ${YOUR_REPOSITORY} = the git repository you want to clone into.
    ${YOUR_BRANCH} = the branch of the git repsoitory you want to clone into.
    ${YOUR_INPUT_FOLDER} = the folder in the repository which contains the SQL files to be scanned.
    ${YOUR_ANTIPATTERN_IMAGE} = the image name of the antipattern tool which you built in step 1.
