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
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigQueryHelper {

  private static final String USER_AGENT_HEADER = "user-agent";
  private static final String USER_AGENT_VALUE =
      "google-pso-tool/bq-anti-pattern-recognition/0.1.0";
  private static final HeaderProvider headerProvider =
      FixedHeaderProvider.create(ImmutableMap.of(USER_AGENT_HEADER, USER_AGENT_VALUE));
  private static final Logger logger = LoggerFactory.getLogger(BigQueryHelper.class);

  private static TableResult runQuery(BigQuery client, String query) throws InterruptedException {
    QueryJobConfiguration jobConfiguration = QueryJobConfiguration
        .newBuilder(query)
        .setAllowLargeResults(true)
        .setUseLegacySql(false)
        .build();

    JobInfo jobInfo = JobInfo.newBuilder(jobConfiguration).build();

    Job queryJob = client.create(jobInfo);

    return queryJob.getQueryResults();
  }

  public static TableResult getSingleQueryFromIs(JobId jobId, BigQuery client) throws InterruptedException {
    // TODO: The time range when looking up a single job should be limited

    String query = String.format(
        "SELECT\n"
            + "  project_id,\n"
            + "  CONCAT(project_id, \":%s.\",  job_id) job_id, \n"
            + "  query, \n"
            + "  total_slot_ms / (1000 * 60 * 60 ) AS slot_hours\n"
            + "FROM `%s.region-%s.INFORMATION_SCHEMA.JOBS_BY_PROJECT`\n"
            + "WHERE \n"
            + "  job_id = '%s'\n"
            + "  AND total_slot_ms > 0\n",
        jobId.getLocation(), jobId.getProject(), jobId.getLocation(), jobId.getJob()
    );

    return runQuery(client, query);
  }

  public static TableResult getQueriesFromIs(
      String projectId, String region, Date startDate, Date endDate, BigQuery client
  ) throws InterruptedException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    String startDateFormatted = dateFormatter.format(startDate);
    String endDataFormatted = dateFormatter.format(endDate);
    String regionName = region.replaceFirst("^region-", "");

    logger.info(
        "Fetching BigQuery jobs run in project {} and in region {} between {} and {}",
        projectId, region, startDateFormatted, endDataFormatted);

    String query = String.format(
        "SELECT\n"
            + "  project_id,\n"
            + "  CONCAT(project_id, \":%s.\",  job_id) job_id, \n"
            + "  query, \n"
            + "  total_slot_ms / (1000 * 60 * 60 ) AS slot_hours\n"
            + "FROM `%s.%s.INFORMATION_SCHEMA.JOBS_BY_PROJECT`\n"
            + "WHERE \n"
            + "  DATE(start_time) BETWEEN '%s' AND '%s'\n"
            + "  AND total_slot_ms > 0\n"
            + "  AND (statement_type != \"SCRIPT\" OR statement_type IS NULL)\n"
            + "  AND (reservation_id != 'default-pipeline' or reservation_id IS NULL)\n"
            + "  AND UPPER(query) NOT LIKE '%%INFORMATION_SCHEMA%%' \n"
            + "ORDER BY \n"
            + "  project_id, start_time desc\n",
        regionName, projectId, region, startDateFormatted, endDataFormatted
    );

    return runQuery(client, query);
  }

  public static TableResult getQueriesFromIs(
      String projectId, String region, Period lookBack, BigQuery client
  ) throws InterruptedException {
    String regionName = region.replaceFirst("^region-", "");

    logger.info(
        "Fetching BigQuery jobs run in project {} and in region {} looking back "
        + "{} years, {} months and {} days.",
        projectId, region, lookBack.getYears(), lookBack.getMonths(), lookBack.getDays());

    String query = String.format(
        "SELECT\n"
            + "  project_id,\n"
            + "  CONCAT(project_id, \":%s.\",  job_id) job_id, \n"
            + "  query, \n"
            + "  total_slot_ms / (1000 * 60 * 60 ) AS slot_hours\n"
            + "FROM `%s.%s.INFORMATION_SCHEMA.JOBS_BY_PROJECT`\n"
            + "WHERE \n"
            + "  start_time >= (CURRENT_TIMESTAMP - (INTERVAL '%d-%d %d' YEAR TO DAY))\n"
            + "  AND total_slot_ms > 0\n"
            + "  AND (statement_type != \"SCRIPT\" OR statement_type IS NULL)\n"
            + "  AND (reservation_id != 'default-pipeline' or reservation_id IS NULL)\n"
            + "  AND UPPER(query) NOT LIKE '%%INFORMATION_SCHEMA%%' \n"
            + "ORDER BY \n"
            + "  project_id, start_time desc\n",
        regionName, projectId, region,
        lookBack.getYears(), lookBack.getMonths(), lookBack.getDays()
    );

    return runQuery(client, query);
  }

  public static TableResult getQueriesFromIS(String projectId, String daysBack, String ISTable)
      throws InterruptedException {
    BigQuery bigquery =
        BigQueryOptions.newBuilder()
            .setProjectId(projectId)
            .setHeaderProvider(headerProvider)
            .build()
            .getService();
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT\n"
                    + "  project_id,\n"
                    + "  CONCAT(project_id, \":US.\",  job_id) job_id, \n"
                    + "  query, \n"
                    + "  total_slot_ms / (1000 * 60 * 60 ) AS slot_hours\n"
                    + "FROM\n"
                    + ISTable
                    + "\n"
                    + "WHERE \n"
                    + "  start_time >= CURRENT_TIMESTAMP - INTERVAL "
                    + daysBack
                    + " DAY\n"
                    + "  AND total_slot_ms > 0\n"
                    + "  AND (statement_type != \"SCRIPT\" OR statement_type IS NULL)\n"
                    + "  AND (reservation_id != 'default-pipeline' or reservation_id IS NULL)\n"
                    + "  AND query not like '%INFORMATION_SCHEMA%' \n"
                    + "ORDER BY \n"
                    + "  project_id, start_time desc\n")
            .setUseLegacySql(false)
            .build();

    Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).build());
    return queryJob.getQueryResults();
  }

  public static void writeResults(
      String processingProject, String outputTable, Map<String, Object> rowContent) {
    String[] tableName = outputTable.split("\\.");
    TableId tableId = TableId.of(tableName[0], tableName[1], tableName[2]);
    BigQuery bigquery =
        BigQueryOptions.newBuilder()
            .setProjectId(processingProject)
            .setHeaderProvider(headerProvider)
            .build()
            .getService();
    bigquery.insertAll(InsertAllRequest.newBuilder(tableId).addRow(rowContent).build());
  }
}
