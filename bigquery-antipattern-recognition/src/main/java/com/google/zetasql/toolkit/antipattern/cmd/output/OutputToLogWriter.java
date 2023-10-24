package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.Main;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputToLogWriter extends AntiPatternOutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(OutputToLogWriter.class);

  public void writeRecForQuery(
      InputQuery inputQuery, List<AbstractVisitor> visitorsThatFoundPatterns) {
    StringBuilder outputStrBuilder = new StringBuilder();

    outputStrBuilder.append("\n"+"-".repeat(50));
    outputStrBuilder.append("\nRecommendations for query: "+ inputQuery.getQueryId());
    for(AbstractVisitor visitor: visitorsThatFoundPatterns) {
      outputStrBuilder.append("\n* "+ visitor.getNAME() + ": " + visitor.getResult());
    }
    outputStrBuilder.append("\n"+"-".repeat(50));
    outputStrBuilder.append("\n\n");
    logger.info(outputStrBuilder.toString());
  }

}
