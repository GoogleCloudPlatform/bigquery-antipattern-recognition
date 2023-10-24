package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ParseTreeVisitor;

public abstract class AbstractVisitor extends ParseTreeVisitor {
  public final String NAME = "";

  public String getResult() {
    return "";
  }

  public abstract String getNAME();
}
