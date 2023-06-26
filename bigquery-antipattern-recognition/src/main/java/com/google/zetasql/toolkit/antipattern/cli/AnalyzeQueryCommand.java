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

import com.google.common.base.Preconditions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.Recommendation;
import com.google.zetasql.toolkit.antipattern.cmd.InputCsvQueryIterator;
import com.google.zetasql.toolkit.antipattern.cmd.InputFolderQueryIterable;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.BasePatternDetector;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyCTEsEvalMultipleTimes;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyInSubqueryWithoutAgg;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyNtileWindowFunction;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyOrderByWithoutLimit;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyRegexpContains;
import com.google.zetasql.toolkit.antipattern.parser.IdentifySimpleSelectStar;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
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
            + "It can be a local file or a GCS object (i.e. gs://BUCKET/path/to/file.sql). "
            + "Supports wildcards at the end (e.g. /path/to/directory/*).",
        required = true)
    Optional<Path> filePath;

    @Option(
        names = { "--csv" },
        description = "A CSV file containing the queries to analyze for antipatterns. "
            + "It can be a local file or a GCS object (i.e. gs://BUCKET/path/to/file.csv). "
            + "Supports wildcards at the end (e.g. /path/to/directory/*).",
        required = true)
    Optional<Path> csvPath;
  }

  private Iterator<InputQuery> fetchQueries() throws IOException {
    if(queryInput.query.isPresent()) {
      String query = queryInput.query.get();
      InputQuery inputQuery = new InputQuery(query, "CLI");
      return List.of(inputQuery).iterator();
    }

    if(queryInput.filePath.isPresent()) {
      // TODO: Validate the input file
      // TODO: Add support for wildcards at the end of the path
      Path filePath = queryInput.filePath.get().toAbsolutePath();
      return new InputFolderQueryIterable(List.of(filePath.toString()));
    }

    if(queryInput.csvPath.isPresent()) {
      // TODO: Validate CSV path
      // TODO: Support CSV files from GCS
      // TODO: Add support for wildcards at the end of the path
      Path csvPath = queryInput.csvPath.get().toAbsolutePath();
      return new InputCsvQueryIterator(csvPath.toString());
    }

    throw new IllegalArgumentException("Should not happen");
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

  private void outputRecommendationsToFile(
      List<InputQuery> inputQueries,
      List<List<Recommendation>> recommendations) throws IOException {
    // TODO: Validate the output path
    // TODO: Use an actual CSV writing tool. This does not do any kind of escaping or encoding.

    Preconditions.checkArgument(outputPath.isPresent());

    File file = outputPath.get().toFile();
    boolean fileExisted = file.exists();
    FileWriter csvWriter = new FileWriter(file, fileExisted);

    try(csvWriter) {
      if(!fileExisted) {
        csvWriter.write(
            String.join(",", new String[] {"id", "query", "name", "description\n"}));
      }

      for(int i = 0; i < inputQueries.size(); i++) {
        InputQuery inputQuery = inputQueries.get(i);
        List<Recommendation> recommendationsForThisQuery = recommendations.get(i);

        for (Recommendation recommendation : recommendationsForThisQuery) {
          StringBuilder rowBuilder = new StringBuilder();
          rowBuilder.append(inputQuery.getQueryId());
          rowBuilder.append(",");
          rowBuilder.append(inputQuery.getQuery());
          rowBuilder.append(",");
          rowBuilder.append(recommendation.getType().name());
          rowBuilder.append(",");
          rowBuilder.append(recommendation.getDescription());
          rowBuilder.append("\n");
          csvWriter.write(rowBuilder.toString());
        }

      }
    }

  }

  private void outputRecommendations(
      List<InputQuery> inputQueries,
      List<List<Recommendation>> recommendations) throws IOException {

    if(outputPath.isPresent()) {
      outputRecommendationsToFile(inputQueries, recommendations);
    } else {
      outputRecommendationsToConsole(inputQueries, recommendations);
    }

  }

  @Override
  public Integer call() throws IOException {
    // TODO: Add better output for end users
    // TODO: Produce sensible error messages and exit codes when an error/exception happens
    //  E.g. this method can throw IOException, but we shouldn't just show the user
    //  the stack trace if that happens.

    Iterator<InputQuery> inputQueriesIterator = fetchQueries();

    // TODO: Avoid loading all queries into memory
    ArrayList<InputQuery> inputQueries = new ArrayList<>();
    inputQueriesIterator.forEachRemaining(inputQueries::add);

    List<List<Recommendation>> recommendations = inputQueries.stream()
        .map(InputQuery::getQuery)
        .map(this::getRecommendationsForQuery)
        .collect(Collectors.toList());

    outputRecommendations(inputQueries, recommendations);

    return 0;
  }

}
