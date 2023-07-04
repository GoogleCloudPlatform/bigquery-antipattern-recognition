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

package com.google.zetasql.toolkit.antipattern.cli;

import com.google.api.client.util.DateTime;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.Recommendation;
import com.google.zetasql.toolkit.antipattern.cli.converters.JobIdConverter;
import com.google.zetasql.toolkit.antipattern.cli.converters.TableIdConverter;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.BasePatternDetector;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyCTEsEvalMultipleTimes;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyInSubqueryWithoutAgg;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyNtileWindowFunction;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyOrderByWithoutLimit;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyRegexpContains;
import com.google.zetasql.toolkit.antipattern.parser.IdentifySimpleSelectStar;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "job",
    aliases = { "jobs" },
    description = "Analyzes and detects antipatterns in BigQuery jobs",
    mixinStandardHelpOptions = true,
    sortOptions = false)
public class AnalyzeJobCommand implements Callable<Integer> {

  @Option(
      names = "--project",
      description = "The GCP project used when accessing the BigQuery API. "
          + "Defaults to the environment's default project.",
      required = false)
  Optional<String> projectId;

  @ArgGroup(exclusive = true, multiplicity = "1")
  JobInput jobInput;

  @Option(
      names = "--output-table",
      description = "A BigQuery output table to write analysis results to. "
          + "If not set, output will be written to stdout. "
          + "The table will be created if it does not exist already.",
      required = false,
      converter = TableIdConverter.class
  )
  Optional<TableId> outputTable;

  static class JobInput {
    @Option(
        names = "--job-id",
        description = "The Job ID for the BigQuery Job that should be analyzed.",
        required = true,
        converter = JobIdConverter.class)
    Optional<JobId> jobId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    MultiJobInput multiJobInput;

  }

  static class MultiJobInput {
    @Option(
        names = "--jobs-project",
        description = "The GCP project to fetch jobs from. "
            + "Defaults to the project used for the BigQuery API.",
        required = false)
    Optional<String> jobsProjectId;

    @Option(
        names = "--region",
        description = "The GCP region to consider jobs from (e.g. region-us).",
        required = true)
    String jobsRegion;

    @ArgGroup(exclusive = true, multiplicity = "1")
    MultiJobPeriod jobsPeriod;
  }

  static class MultiJobPeriod {
    @Option(
        names = "--look-back",
        description = "The amount of time to look back when fetching jobs in ISO-8601 "
            + "duration format. E.g. P2D (2 days), P1Y2M3D (1 year, 2 months, 3 days), "
            + "P1W (1 week).",
        required = true)
    Optional<Period> lookBack;

    @ArgGroup(exclusive = false, multiplicity = "1")
    MultiJobDateRange dateRange;
  }

  static class MultiJobDateRange {
    @Option(
        names = { "--start", "--start-date" },
        description = "Start date from which to fetch jobs.",
        required = true
    )
    Date startDate;

    @Option(
        names = { "--end", "--end-date" },
        description = "End date from which to fetch jobs.",
        required = true
    )
    Date endDate;
  }

  private String getProjectIdForJobs(BigQuery client) {
    if(jobInput.multiJobInput != null && jobInput.multiJobInput.jobsProjectId.isPresent()) {
      return jobInput.multiJobInput.jobsProjectId.get();
    }

    if(projectId.isPresent()) {
      return projectId.get();
    }

    return client.getOptions().getProjectId();
  }

  private Iterator<InputQuery> fetchJobs(BigQuery client) throws InterruptedException {
    // TODO: We can probably build a better abstraction that what the BigQuery helper
    //  currently exposes to fetch jobs from INFORMATION_SCHEMA.
    //  At the least, the abstraction we build should return an iterator of InputQuery directly,
    //  not a TableResult.
    String projectIdForJobs = getProjectIdForJobs(client);
    TableResult tableResult;

    if(jobInput.jobId.isPresent()) {
      JobId jobId = jobInput.jobId.get();
      tableResult = BigQueryHelper.getSingleQueryFromIs(jobId, client);
    } else if (jobInput.multiJobInput.jobsPeriod.lookBack.isPresent()) {
      Period lookBack = jobInput.multiJobInput.jobsPeriod.lookBack.get();
      String region = jobInput.multiJobInput.jobsRegion;
      tableResult = BigQueryHelper.getQueriesFromIs(projectIdForJobs, region, lookBack, client);
    } else {
      String region = jobInput.multiJobInput.jobsRegion;
      Date startDate = jobInput.multiJobInput.jobsPeriod.dateRange.startDate;
      Date endDate = jobInput.multiJobInput.jobsPeriod.dateRange.endDate;
      tableResult = BigQueryHelper.getQueriesFromIs(
          projectIdForJobs, region, startDate, endDate, client);
    }

    Iterator<FieldValueList> rows = tableResult.iterateAll().iterator();

    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return rows.hasNext();
      }

      @Override
      public InputQuery next() {
        FieldValueList row = rows.next();
        String job_id = row.get("job_id").getStringValue();
        String query = row.get("query").getStringValue();
        String projectId = row.get("project_id").getStringValue();
        String slot_hours = row.get("slot_hours").getStringValue();
        return new InputQuery(query, job_id, projectId, Float.parseFloat(slot_hours));
      }
    };
  }

  private List<Recommendation> getRecommendationsForQuery(String query) {
    // TODO: Avoid repetition of this piece of logic between commands.
    ASTStatement parsedQuery = Parser.parseStatement(query, BigQueryLanguageOptions.get());

    List<BasePatternDetector> patternDetectors = List.of(
        new IdentifySimpleSelectStar(),
        new IdentifyInSubqueryWithoutAgg(),
        new IdentifyCTEsEvalMultipleTimes(),
        new IdentifyOrderByWithoutLimit(),
        new IdentifyRegexpContains(),
        new IdentifyNtileWindowFunction()
    );

    return patternDetectors.stream()
        .map(detector -> detector.run(parsedQuery, query))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private void outputRecommendationsToConsole(
      List<InputQuery> inputQueries,
      List<List<Recommendation>> recommendations) {
    // TODO: Avoid repetition of this piece of logic between commands
    // TODO: Define what formatting we want when outputting to the console
    for(int i = 0; i < inputQueries.size(); i++) {
      InputQuery inputQuery = inputQueries.get(i);
      List<Recommendation> recommendationsForThisQuery = recommendations.get(i);

      System.out.printf("Query: %s\n", inputQuery.getQuery());
      recommendationsForThisQuery.forEach(singleRecommendation ->
          System.out.printf("%s: %s\n",
              singleRecommendation.getType().name(),
              singleRecommendation.getDescription()));
      System.out.println("----------------------------");
    }
  }

  private void outputRecommendationsToTable(
      List<InputQuery> inputQueries,
      List<List<Recommendation>> recommendations,
      BigQuery client) {
    // TODO: Parse the output table properly, this currently assumes it is well-formed
    // TODO: Create the output table if it does not exist
    // TODO: Inserts should be batched, the size of a single InsertAllRequest is bounded
    Preconditions.checkArgument(outputTable.isPresent());

    InsertAllRequest.Builder requestBuilder = InsertAllRequest.newBuilder(outputTable.get());

    for(int i = 0; i < inputQueries.size(); i++) {
      InputQuery inputQuery = inputQueries.get(i);
      List<Recommendation> recommendation = recommendations.get(i);
      requestBuilder.addRow(Map.of(
          "job_id", inputQuery.getQueryId(),
          "query", inputQuery.getQuery(),
          "slot_hours", Float.toString(inputQuery.getSlotHours()),
          "recommendation", recommendation,
          "process_timestamp", DateTime.parseRfc3339(Instant.now().toString())
      ));
    }

    InsertAllRequest request = requestBuilder.build();

    client.insertAll(request);
  }

  private void outputRecommendations(
      List<InputQuery> inputQueries,
      List<List<Recommendation>> recommendations,
      BigQuery client) {

    if(outputTable.isPresent()) {
      outputRecommendationsToTable(inputQueries, recommendations, client);
    } else {
      outputRecommendationsToConsole(inputQueries, recommendations);
    }

  }

  @Override
  public Integer call() throws InterruptedException {
    // TODO: Add better output for end users
    // TODO: Produce sensible error messages and exit codes when an error/exception happens
    //  E.g. this method can throw InterruptedException, but we shouldn't just show the user
    //  the stack trace if that happens.
    BigQueryOptions.Builder clientBuilder = BigQueryOptions.newBuilder();
    projectId.ifPresent(clientBuilder::setProjectId);
    BigQuery client = clientBuilder.build().getService();

    Iterator<InputQuery> inputQueriesIterator = fetchJobs(client);

    // TODO: Avoid loading all jobs into memory
    ArrayList<InputQuery> inputQueries = new ArrayList<>();
    inputQueriesIterator.forEachRemaining(inputQueries::add);

    List<List<Recommendation>> recommendations = inputQueries.stream()
        .map(InputQuery::getQuery)
        .map(this::getRecommendationsForQuery)
        .collect(Collectors.toList());

    outputRecommendations(inputQueries, recommendations, client);

    return 0;
  }

}
