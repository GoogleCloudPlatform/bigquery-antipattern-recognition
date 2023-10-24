package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class IdentifyDynamicPredicateVisitor extends AbstractVisitor {

  public final static String NAME = "DynamicPredicate";
  private String query;
  private Boolean insideWhere = false;
  private final String DYNAMIC_PREDICATE_SUGGESTION_MESSAGE = "Using subquery in filter at line %d. Converting this dynamic predicate to static might provide better performance.";
  private ArrayList<String> result = new ArrayList<String>();

  public IdentifyDynamicPredicateVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTSelect selectNode) {
    if(insideWhere) {
      int location = ZetaSQLStringParsingHelper.countLine(query, selectNode.getParseLocationRange().start());
      result.add(String.format(DYNAMIC_PREDICATE_SUGGESTION_MESSAGE, location));
    }
    super.visit(selectNode);
  }

  @Override
  public void visit(ASTNodes.ASTWhereClause whereNode) {
    this.insideWhere = true;
    super.visit(whereNode);
    this.insideWhere = false;
  }

  public String getResult() {
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }
}
