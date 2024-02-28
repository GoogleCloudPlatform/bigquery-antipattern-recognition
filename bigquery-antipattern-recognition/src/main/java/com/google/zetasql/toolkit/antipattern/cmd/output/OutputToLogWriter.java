package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AntipatternParserVisitor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputToLogWriter extends AntiPatternOutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(OutputToLogWriter.class);

  public void writeRecForQuery(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns,
                               AntiPatternCommandParser cmdParser) {
    StringBuilder outputStrBuilder = new StringBuilder();

    outputStrBuilder.append("\n"+"-".repeat(50));
    outputStrBuilder.append("\nRecommendations for query: "+ inputQuery.getQueryId());
    for(AntiPatternVisitor visitor: visitorsThatFoundPatterns) {
      outputStrBuilder.append("\n* "+ visitor.getNAME() + ": " + visitor.getResult());
    }
    if(cmdParser.rewriteSQL() && inputQuery.getOptimizedQuery() != null) {
      outputStrBuilder.append("\nOptimized query:\n");
      outputStrBuilder.append(inputQuery.getOptimizedQuery());
    }
    outputStrBuilder.append("\n"+"-".repeat(50));
    outputStrBuilder.append("\n\n");
    logger.info(outputStrBuilder.toString());
  }

}
