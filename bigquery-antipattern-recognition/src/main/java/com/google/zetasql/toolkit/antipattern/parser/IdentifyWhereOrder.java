package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;
import java.util.stream.Collectors;

public class IdentifyWhereOrder implements BasePatternDetector {

  @Override
  public String run(ASTStatement parsedQuery, String query) {
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    return visitor.getResult().stream().distinct().collect(Collectors.joining("\n"));

  }

}
