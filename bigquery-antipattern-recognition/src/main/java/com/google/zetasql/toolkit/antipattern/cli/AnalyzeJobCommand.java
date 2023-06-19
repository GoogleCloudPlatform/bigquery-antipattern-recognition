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

import java.time.Period;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
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
      required = false
  )
  Optional<String> outputTable;

  static class JobInput {
    @Option(
        names = "--job-id",
        description = "The Job ID for the BigQuery Job that should be analyzed.",
        required = true)
    String jobId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    MultiJobInput multiJobInput;

  }

  static class MultiJobInput {
    @Option(
        names = "--jobs-project",
        description = "The GCP project to fetch jobs from. "
            + "Defaults to the project used for the BigQuery API.",
        required = false)
    String jobsProjectId;

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
    Period lookBack;

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

  @Override
  public Integer call() {
    return 0;
  }

}
