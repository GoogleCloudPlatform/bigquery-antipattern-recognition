/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zetasql.toolkit.antipattern.util;

import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigQueryHelper {

  private static final String USER_AGENT_HEADER = "user-agent";
  private static final String USER_AGENT_VALUE = "google-pso-tool/antipattern-tool/0.1.0";
  private static final HeaderProvider headerProvider =
      FixedHeaderProvider.create(ImmutableMap.of(USER_AGENT_HEADER, USER_AGENT_VALUE));
  private static final Logger logger = LoggerFactory.getLogger(BigQueryHelper.class);

  private BigQuery bigquery;

  public BigQueryHelper(String processingProject, String serviceAccountKeyfilePath)
      throws IOException {
    BigQueryOptions.Builder bigQueryOptions =
        BigQueryOptions.newBuilder()
            .setHeaderProvider(headerProvider);

    if (processingProject != null) {
      bigQueryOptions.setProjectId(processingProject);
    }

    if (serviceAccountKeyfilePath != null) {
      bigQueryOptions.setCredentials(
          ServiceAccountCredentials.fromStream(
              new FileInputStream(serviceAccountKeyfilePath))
      );
    }

    bigquery = bigQueryOptions.build().getService();
  }

  public TableResult getQueriesFromIS(
      String daysBack,
      String startTime,
      String endTime,
      String ISTable,
      Integer slotsMsMin,
      Long timeoutInSecs,
      Float topNPercent,
      String region)
      throws InterruptedException {
    String timeCriteria;
    if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
      logger.info(
          "Running job on project {}, reading from: {}, scanning last {} days."
              + "and selecting queries with minimum {} slotms. "
              + " Considering only top {}% slot consuming jobs",
          bigquery.getOptions().getProjectId(), ISTable, daysBack, slotsMsMin, topNPercent * 100);
      timeCriteria = "  creation_time >= CURRENT_TIMESTAMP - INTERVAL " + daysBack + " DAY\n";
    } else {
      logger.info(
          "Running job on project {}, reading from: {}, scanning between {} and {}."
              + "and selecting queries with minimum {} slotms",
          bigquery.getOptions().getProjectId(),
          ISTable,
          startTime,
          endTime,
          slotsMsMin);

      startTime = startTime.trim();
      endTime = endTime.trim();
      if (!(startTime.startsWith("'") || startTime.startsWith("\""))) {
        startTime = "'" + startTime + "'";
      }
      if (!(endTime.startsWith("'") || endTime.startsWith("\""))) {
        endTime = "'" + endTime + "'";
      }
      timeCriteria = "  creation_time BETWEEN " + startTime + " AND " + endTime + "\n";
    }
    return getQueriesFromIS(timeoutInSecs, timeCriteria, ISTable, slotsMsMin, topNPercent, region);
  }

  private TableResult getQueriesFromIS(
      Long timeoutInSecs,
      String timeCriteria,
      String ISTable,
      Integer slotsMsMin,
      Float topNPercent,
      String region)
      throws InterruptedException {

    String query =
        "SELECT\n"
            + "  project_id,\n"
            + "  CONCAT(project_id, \":"
            + region.toUpperCase()
            + ".\",  job_id) job_id, \n"
            + "  query, \n"
            + "  total_slot_ms / (1000 * 60 * 60 ) AS slot_hours, \n"
            + "  user_email, \n"
            + "  PERCENT_RANK() OVER(ORDER BY total_slot_ms desc) perc_rnk \n"
            + "FROM\n"
            + ISTable
            + "\n"
            + "WHERE \n"
            + timeCriteria
            + "  AND total_slot_ms > "
            + slotsMsMin
            + "\n"
            + "  AND reservation_id != 'default-pipeline' \n"
            + "  AND query not like '%INFORMATION_SCHEMA%' \n"
            + "QUALIFY perc_rnk < "
            + topNPercent
            + "\n"
            + "ORDER BY \n"
            + "  project_id, start_time desc\n";

    logger.info("Reading from INFORMATION_SCHEMA: \n" + query);
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query)
            .setUseLegacySql(false)
            .setJobTimeoutMs(TimeUnit.SECONDS.toMillis(timeoutInSecs))
            .build();

    logger.debug("Running query:\n" + queryConfig.getQuery());
    Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).build());
    return queryJob.getQueryResults();
  }

  public TableResult getQueriesFromBQTable(String inputTable) throws InterruptedException {
    String query = "SELECT\n" + "  id,\n" + "  query" + " FROM \n`" + inputTable + "`;";

    logger.info("Reading from BigQuery table: \n" + query);
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build();

    logger.debug("Running query:\n" + queryConfig.getQuery());
    Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).build());
    return queryJob.getQueryResults();
  }

  public void writeResults(
      String processingProject, String outputTable, Map<String, Object> rowContent)
      throws IOException {
    String[] tableName = outputTable.split("\\.");
    TableId tableId = TableId.of(tableName[0], tableName[1], tableName[2]);
    InsertAllResponse response =
        bigquery.insertAll(InsertAllRequest.newBuilder(tableId).addRow(rowContent).build());
    if (response.hasErrors()) {
      logger.error(
          "Insert into "
              + tableId.toString()
              + " failed, with these errors: "
              + StringUtils.join(response.getInsertErrors()));
    }
  }

  public void checkBQConnectiviy() {
    try {
      bigquery.listDatasets("bigquery-public-data", BigQuery.DatasetListOption.pageSize(1));
    } catch (Throwable e) {

    }
  }
}
