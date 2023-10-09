package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyDynamicPredicateVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.rownum.IdentifyLatestRecordVisitor;
import java.util.stream.Collectors;

public class IdentifyDynamicPredicate implements BasePatternDetector {

  @Override
  public String run(ASTStatement parsedQuery, String query) {
    IdentifyDynamicPredicateVisitor visitor = new IdentifyDynamicPredicateVisitor(query);
    parsedQuery.accept(visitor);
    return visitor.getResult().stream().distinct().collect(Collectors.joining("\n"));

  }

}
