package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ParseTreeVisitor;

public class AbstractVisitor extends ParseTreeVisitor {
  public final String NAME = "";

  public String getResult() {
    return "";
  }
}
