package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import java.io.IOException;
import java.util.List;

public class AbstractOutputGenerator {

  public void writeRecForQuery(InputQuery inputQuery, List<AbstractVisitor> visitorsThatFoundPatterns) throws IOException {

  }

}
