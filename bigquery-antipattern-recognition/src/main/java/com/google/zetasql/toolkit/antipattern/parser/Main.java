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

package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.IdentifyJoinOrder;
import com.google.zetasql.toolkit.antipattern.cmd.BQAntiPatternCMDParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.cmd.OutputGenerator;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static String analyzerProject = null;
  private static AnalyzerOptions analyzerOptions;
  private static BigQueryAPIResourceProvider resourceProvider;
  private static BQAntiPatternCMDParser cmdParser;
  private static ZetaSQLToolkitAnalyzer analyzer;
  private static BigQueryService service;
  private static BigQueryCatalog catalog;

  public static void main(String[] args) throws ParseException, IOException {
    cmdParser = new BQAntiPatternCMDParser(args);

    // parser setup
    LanguageOptions parserLanguageOptions = getParserLanguageOptions();

    // analyzer setup
    if (cmdParser.useAnalyzer()) {
      analyzerOptions = new AnalyzerOptions();
      analyzer = getAnalyzer(analyzerOptions);
      service = BigQueryService.buildDefault();
      resourceProvider = BigQueryAPIResourceProvider.build(service);
      catalog = new BigQueryCatalog("");
    }

    Iterator<InputQuery> inputQueriesIterator = cmdParser.getInputQueries();
    InputQuery inputQuery;
    int countQueries = 0, countAntiPatterns = 0, countErrors = 0;
    while (inputQueriesIterator.hasNext()) {
      inputQuery = inputQueriesIterator.next();
      String query = inputQuery.getQuery();
      List<String[]> outputData = new ArrayList<>();
      try {
        String rec = getRecommendationsParser(parserLanguageOptions, query);
        if(cmdParser.useAnalyzer()) {
          rec += getRecommendationsAnalyzer(inputQuery, catalog, analyzer, service);
        }

        if (rec.length() > 0) {
          addRecToOutput(cmdParser, outputData, inputQuery, rec);
          OutputGenerator.writeOutput(cmdParser, outputData);
          countAntiPatterns += 1;
        }
      } catch (Exception e) {
        countErrors += 1;
      }
      countQueries += 1;
    }

    logger.info(
        "Processing finished."
            + "Queries read: {}. "
            + "Queries with anti-patterns: {}. "
            + "Queries that could not be parsed: {}.",
        countQueries,
        countAntiPatterns,
        countErrors);
  }

  private static ZetaSQLToolkitAnalyzer getAnalyzer(AnalyzerOptions options) {
    LanguageOptions languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    options.setLanguageOptions(languageOptions);
    options.setCreateNewColumnForEachProjectedOutput(true);
    return new ZetaSQLToolkitAnalyzer(options);
  }

  private static String getRecommendationsAnalyzer(InputQuery inputQuery, BigQueryCatalog catalog,
      ZetaSQLToolkitAnalyzer analyzer, BigQueryService service) {

    String query = inputQuery.getQuery();
    String currentProject;
    if(inputQuery.getProjectId() == null) {
      currentProject = cmdParser.getAnalyzerDefaultProject();
    } else {
      currentProject = inputQuery.getProjectId();
    }
    if((analyzerProject == null || !analyzerProject.equals(currentProject))) {
      analyzerProject = inputQuery.getProjectId();
      catalog = new BigQueryCatalog(analyzerProject, resourceProvider);
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
    }
    String rec = (new IdentifyJoinOrder()).run(query, catalog, analyzer, service);
    System.out.println(rec);
    return rec;
  }


  private static LanguageOptions getParserLanguageOptions() {
    LanguageOptions languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    return languageOptions;
  }

  private static void addRecToOutput(
      BQAntiPatternCMDParser cmdParser,
      List<String[]> outputData,
      InputQuery inputQuery,
      String rec) {
    if (cmdParser.isReadingFromInfoSchema()) {
      outputData.add(
          new String[]{
              inputQuery.getQueryId(),
              inputQuery.getQuery(),
              Float.toString(inputQuery.getSlotHours()),
              "\"" + rec + "\"",
          });
    } else {
      outputData.add(new String[]{inputQuery.getQueryId(), "\"" + rec + "\""});
    }
  }

  private static String getRecommendationsParser(LanguageOptions parserLanguageOptions,
      String query) {
    ASTStatement parsedQuery = Parser.parseStatement(query, parserLanguageOptions);
    ArrayList<String> recommendation = new ArrayList<>();
    recommendation.add(new IdentifySimpleSelectStar().run(parsedQuery));
    recommendation.add(new IdentifyInSubqueryWithoutAgg().run(parsedQuery, query));
    recommendation.add(new IdentifyCTEsEvalMultipleTimes().run(parsedQuery, query));
    recommendation.add(new IdentifyOrderByWithoutLimit().run(parsedQuery, query));
    recommendation.add(new IdentifyRegexpContains().run(parsedQuery, query));
    return recommendation.stream().filter(x -> x.length() > 0).collect(Collectors.joining("\n"));
  }
}