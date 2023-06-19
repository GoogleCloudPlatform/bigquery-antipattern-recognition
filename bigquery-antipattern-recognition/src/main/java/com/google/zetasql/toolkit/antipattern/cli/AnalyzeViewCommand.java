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

import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "view",
    aliases = { "views" },
    description = "Analyzes and detects antipatterns in BigQuery views",
    mixinStandardHelpOptions = true,
    sortOptions = false)
public class AnalyzeViewCommand implements Callable<Integer> {

  @Option(
      names = "--project",
      description = "The GCP project used when accessing the BigQuery API. "
          + "Defaults to the environment's default project.",
      required = false)
  Optional<String> projectId;

  @ArgGroup(exclusive = true, multiplicity = "1")
  ViewInput viewInput;

  @Option(
      names = "--output-table",
      description = "A BigQuery output table to write analysis results to. "
          + "If not set, output will be written to stdout. "
          + "The table will be created if it does not exist already.",
      required = false
  )
  Optional<String> outputTable;

  static class ViewInput {

    @Option(
        names = "--view-id",
        description = "The View ID for the BigQuery view that should be analyzed.",
        required = true)
    String viewId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    MultiViewInput multiViewInput;

  }

  static class MultiViewInput {
    @Option(
        names = "--views-project",
        description = "The GCP project to fetch views from. "
            + "Defaults to the project used for the BigQuery API.",
        required = false)
    String viewsProjectId;

    @Option(
        names = "--region",
        description = "The GCP region to consider views from (e.g. region-us).",
        required = true)
    String viewsRegion;
  }

  @Override
  public Integer call() {
    return 0;
  }
}
