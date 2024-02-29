package com.google.zetasql.toolkit.antipattern.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import java.io.IOException;
import java.util.List;

public abstract class OutputWriter {

  private boolean rewriteSQL = false;

  public abstract void writeRecForQuery(InputQuery inputQuery,
      List<AntiPatternVisitor> visitorsThatFoundPatterns, AntiPatternCommandParser cmdParser)
      throws IOException;

  public void close() throws IOException {};

  public void setRewriteSQL(boolean rewriteSQL) {
    this.rewriteSQL = rewriteSQL;
  }
}
