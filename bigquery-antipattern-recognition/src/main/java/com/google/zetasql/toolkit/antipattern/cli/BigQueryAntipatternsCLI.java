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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "bigquery-antipatterns",
    version = "BigQuery Antipattern Recognition 0.1",
    description = "A tool for detecting SQL antipatterns in BigQuery queries and jobs",
    subcommands = { AnalyzeCommand.class },
    mixinStandardHelpOptions = true,
    sortOptions = false)
public class BigQueryAntipatternsCLI {

  @Spec
  CommandSpec spec;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new BigQueryAntipatternsCLI()).execute(args);
    System.exit(exitCode);
  }
}
