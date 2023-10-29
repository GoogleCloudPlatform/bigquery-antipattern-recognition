package com.google.zetasql.toolkit.antipattern;

import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder.JoinOrderVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.cmd.output.AntiPatternOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.BQOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.GCSFileOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.LocalFileOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.OutputToLogWriter;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyCTEsEvalMultipleTimesVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyDynamicPredicateVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyInSubqueryWithoutAggVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyOrderByWithoutLimitVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyRegexpContainsVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifySimpleSelectStarVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.rownum.IdentifyLatestRecordVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static AntiPatternCommandParser cmdParser;
  private static String analyzerProject = null;
  private static AnalyzerOptions analyzerOptions;
  private static BigQueryAPIResourceProvider resourceProvider;
  private static ZetaSQLToolkitAnalyzer analyzer;
  private static BigQueryService service;
  private static BigQueryCatalog catalog;
  private static LanguageOptions languageOptions = new LanguageOptions();
  private static HashMap<String, Integer> visitorMetricsMap;
  private static int countQueriesRead = 0;
  private static int countQueriesWithAntipattern = 0;

  static {
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    languageOptions.enableReservableKeyword("QUALIFY");
  }

  public static void main(String[] args) throws ParseException, IOException {
    cmdParser = new AntiPatternCommandParser(args);
    if (cmdParser.useAnalyzer()) {
      setAnalyzerOptions();
    }

    Iterator<InputQuery> inputQueriesIterator = cmdParser.getInputQueries();
    AntiPatternOutputWriter outputWriter = getOutputWriter(cmdParser);

    InputQuery inputQuery;

    while (inputQueriesIterator.hasNext()) {
      inputQuery = inputQueriesIterator.next();
      logger.info("Parsing query: " + inputQuery.getQueryId());
      checkForAntiPatternsInQuery(inputQuery, outputWriter);
      countQueriesRead += 1;
    }
    logResultStats();
    outputWriter.close();
  }

  private static void checkForAntiPatternsInQuery(InputQuery inputQuery, AntiPatternOutputWriter outputWriter)
      throws IOException {

    try {
      List<AntiPatternVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();
      // parser visitors
      checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitorsThatFoundAntiPatterns);

      // analyzer visitor
      if (cmdParser.useAnalyzer()) {
        checkForAntiPatternsInQueryWithAnalyzerVisitors(inputQuery, visitorsThatFoundAntiPatterns);
      }

      // write output
      if (visitorsThatFoundAntiPatterns.size() > 0) {
        countQueriesWithAntipattern += 1;
        outputWriter.writeRecForQuery(inputQuery, visitorsThatFoundAntiPatterns);
      }

    } catch (Exception e) {
      logger.error("Error processing query with id: " + inputQuery.getQueryId());
      logger.error(e.getMessage(), e);
    }
  }

  private static void checkForAntiPatternsInQueryWithParserVisitors(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns ) {

    List<AntiPatternVisitor> parserVisitorList = getParserVisitorList(inputQuery.getQuery());
    if(visitorMetricsMap == null) {
      setVisitorMetricsMap(parserVisitorList);
    }

    for (AntiPatternVisitor visitor : parserVisitorList) {
      try{
        logger.info("Parsing query with id: " + inputQuery.getQueryId() +
            " for anti-pattern: " + visitor.getNAME());
        ASTStatement parsedQuery = Parser.parseStatement( inputQuery.getQuery(), languageOptions);
        parsedQuery.accept((ParseTreeVisitor) visitor);
        String result = visitor.getResult();
        if(result.length() > 0) {
          visitorsThatFoundAntiPatterns.add(visitor);
          visitorMetricsMap.merge(visitor.getNAME(), 1, Integer::sum);
        }
      } catch (Exception e) {
        logger.error("Error parsing query with id: " + inputQuery.getQueryId() +
            " for anti-pattern:" + visitor.getNAME());
        logger.error(e.getMessage(), e);
      }
    }
  }

  private static void checkForAntiPatternsInQueryWithAnalyzerVisitors(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns ) {
    String query = inputQuery.getQuery();
    String currentProject;

    if (inputQuery.getProjectId() == null) {
      currentProject = cmdParser.getAnalyzerDefaultProject();
    } else {
      currentProject = inputQuery.getProjectId();
    }

    if ((analyzerProject == null || !analyzerProject.equals(currentProject))) {
      analyzerProject = inputQuery.getProjectId();
      catalog = new BigQueryCatalog(analyzerProject, resourceProvider);
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
    }
    JoinOrderVisitor visitor = new JoinOrderVisitor(service);
    if(visitorMetricsMap.get(visitor.getNAME()) == null) {
      visitorMetricsMap.put(visitor.getNAME(), 0);
      visitorMetricsMap.merge(visitor.getNAME(), 1, Integer::sum);
    }
    try {
      logger.info("Analyzing query with id: " + inputQuery.getQueryId() +
          " For anti-pattern:" + visitor.getNAME());
      Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));

      String result = visitor.getResult();
      if (result.length() > 0) {
        visitorsThatFoundAntiPatterns.add(visitor);
      }
    } catch (Exception e) {
      logger.error("Error analyzing query with id: " + inputQuery.getQueryId() +
          " For anti-pattern:" + visitor.getNAME());
      logger.error(e.getMessage(), e);
    }
  }

  private static List<AntiPatternVisitor> getParserVisitorList(String query) {
    return new ArrayList<>(Arrays.asList(
        new IdentifySimpleSelectStarVisitor(),
        new IdentifyInSubqueryWithoutAggVisitor(query),
        new IdentifyCTEsEvalMultipleTimesVisitor(query),
        new IdentifyOrderByWithoutLimitVisitor(query),
        new IdentifyRegexpContainsVisitor(query),
        new IdentifyLatestRecordVisitor(query),
        new IdentifyDynamicPredicateVisitor(query),
        new IdentifyWhereOrderVisitor(query)
    ));
  }

  private static void setVisitorMetricsMap(List<AntiPatternVisitor> parserVisitorList ) {
    visitorMetricsMap = new HashMap<>();
    parserVisitorList.stream().forEach(visitor -> visitorMetricsMap.put(visitor.getNAME(), 0));
  }

  private static AntiPatternOutputWriter getOutputWriter(AntiPatternCommandParser cmdParser) {
    if (cmdParser.hasOutputFileOptionName()){
      if(GCSHelper.isGCSPath(cmdParser.getOutputFileOptionName())){
        return new GCSFileOutputWriter(cmdParser.getOutputFileOptionName());
      } else {
        return new LocalFileOutputWriter(cmdParser.getOutputFileOptionName());
      }
    }
    else if(cmdParser.hasOutputTable()) {
      BQOutputWriter outputWriter = new BQOutputWriter(cmdParser.getOutputTable());
      outputWriter.setProcessingProjectName(cmdParser.getProcessingProject());
      return outputWriter;
    }
    else {
      return new OutputToLogWriter();
    }
  }

  private static void setAnalyzerOptions() {
    analyzerOptions = new AnalyzerOptions();
    analyzer = getAnalyzer(analyzerOptions);
    service = BigQueryService.buildDefault();
    resourceProvider = BigQueryAPIResourceProvider.build(service);
    catalog = new BigQueryCatalog("");
  }

  private static ZetaSQLToolkitAnalyzer getAnalyzer(AnalyzerOptions options) {
    LanguageOptions languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    options.setLanguageOptions(languageOptions);
    options.setCreateNewColumnForEachProjectedOutput(true);
    return new ZetaSQLToolkitAnalyzer(options);
  }

  private static void logResultStats(){
    StringBuilder statsString = new StringBuilder();
    statsString.append("\n\n* Queries read: " + countQueriesRead);
    statsString.append("\n* Queries with anti patterns: " + countQueriesWithAntipattern);

    // for (HashMap.Entry<String, Integer> entry : visitorMetricsMap.entrySet()) {
    //   statsString.append(String.format("\n* %s: %d", entry.getKey(), entry.getValue()));
    // }
    logger.info(statsString.toString());
  }
}
