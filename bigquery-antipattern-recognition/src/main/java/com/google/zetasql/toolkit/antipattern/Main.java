package com.google.zetasql.toolkit.antipattern;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.cmd.output.AntiPatternOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.BQOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.GCSFileOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.LocalFileOutputWriter;
import com.google.zetasql.toolkit.antipattern.cmd.output.OutputToLogWriter;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyCTEsEvalMultipleTimesVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyDynamicPredicateVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyInSubqueryWithoutAggVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyOrderByWithoutLimitVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyRegexpContainsVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifySimpleSelectStarVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.rownum.IdentifyLatestRecordVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static AntiPatternCommandParser cmdParser;
  private static LanguageOptions languageOptions = new LanguageOptions();
  static {
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    languageOptions.enableReservableKeyword("QUALIFY");
  }

  public static void main(String[] args) throws ParseException, IOException {
    cmdParser = new AntiPatternCommandParser(args);

    Iterator<InputQuery> inputQueriesIterator = cmdParser.getInputQueries();
    AntiPatternOutputWriter outputWriter = getOutputWriter(cmdParser);

    InputQuery inputQuery;
    int countQueries = 0, countAntiPatterns = 0, countErrors = 0,
        countNotParsedQueries = 0, countPartiallyParsedQueries = 0, countFullyParsedQueries = 0;
    while (inputQueriesIterator.hasNext()) {
      inputQuery = inputQueriesIterator.next();
      logger.info("Parsing query: " + inputQuery.getQueryId());
      String query = inputQuery.getQuery();

      List<AbstractVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();

      for (AbstractVisitor visitor : getVisitorList(inputQuery.getQuery())) {
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        parsedQuery.accept(visitor);
        String result = visitor.getResult();
        if(result.length() > 0){
          visitorsThatFoundAntiPatterns.add(visitor);
        }
      }
      if(visitorsThatFoundAntiPatterns.size()>0) {
        outputWriter.writeRecForQuery(inputQuery, visitorsThatFoundAntiPatterns);
      }
    }
    outputWriter.close();
  }

  private static List<AbstractVisitor> getVisitorList(String query) {
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

}
