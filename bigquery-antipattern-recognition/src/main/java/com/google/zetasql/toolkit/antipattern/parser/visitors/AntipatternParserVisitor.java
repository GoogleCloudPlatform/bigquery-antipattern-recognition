package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

public abstract class AntipatternParserVisitor extends ParseTreeVisitor implements AntiPatternVisitor {
  public final String NAME = "";

  public String getResult() {
    return "";
  }

  public abstract String getNAME();
}
