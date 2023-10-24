package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import java.io.IOException;
import java.util.List;

public abstract class AntiPatternOutputWriter {

  public abstract void writeRecForQuery(InputQuery inputQuery, List<AbstractVisitor> visitorsThatFoundPatterns) throws IOException;

  public void close() throws IOException {

  };
}
