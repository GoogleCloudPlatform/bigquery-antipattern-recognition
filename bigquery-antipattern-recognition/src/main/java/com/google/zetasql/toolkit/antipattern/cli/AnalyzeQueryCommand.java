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

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "query",
    aliases = { "queries" },
    description = "Analyzes and detects antipatterns in BigQuery queries",
    mixinStandardHelpOptions = true,
    sortOptions = false)
public class AnalyzeQueryCommand implements Callable<Integer> {

  @ArgGroup(exclusive = true, multiplicity = "1")
  QueryInput queryInput;

  @Option(
      names = { "-o", "--output-file" },
      description = "Optional output file path. It can be a local or a GCS path "
          + "(i.e. gs://BUCKET/path/to/output). If omitted, output will be written to stdout.",
      required = false)
  Optional<Path> outputPath;

  static class QueryInput {
    @Option(
        names = "--query",
        description = "The query to analyze for antipatterns.",
        required = true)
    Optional<String> query;

    @Option(
        names = { "-f", "--input-file" },
        description = "Path to a .sql file containing the queries to analyze for antipatterns. "
            + "It can be a local file or a GCS object (i.e. gs://BUCKET/path/to/file.sql).",
        required = true)
    Optional<Path> filePath;

    @Option(
        names = { "--input-directory" },
        description = "Path to a directory containing .sql files to analyze for antipatterns. "
            + "It can be a local directory or a GCS prefix (i.e. gs://BUCKET/directory/).",
        required = true)
    Optional<Path> directoryPath;

    @Option(
        names = { "--csv" },
        description = "A CSV file containing the queries to analyze for antipatterns. "
            + "It can be a local file or a GCS object (i.e. gs://BUCKET/path/to/file.csv).",
        required = true)
    Optional<Path> csvPath;
  }

  @Override
  public Integer call() {
    return 0;
  }

}
