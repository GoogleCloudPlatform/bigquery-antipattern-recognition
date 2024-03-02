package com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;

public class IdentifyWhereOrderVisitor extends ParseTreeVisitor implements AntiPatternVisitor {

  public final static String NAME = "WhereOrder";
  private String query;
  private Boolean insideWhere = false;
  private final String WHERE_ORDER_SUGGESTION_MESSAGE = "SubOptimal order of predicates in WHERE, line %d. "
      + "Consider applying more restrictive filters first. For example a '=' or a '>' filter usually "
      + "is usually more restrictive than a like '%%' filter. The following order might provide "
      + "performance benefits is '=', '>', '<', '<>', 'like'";

  private ArrayList<String> result = new ArrayList<String>();
  private Stack<ASTNodes.ASTWhereClause> whereNodeStack = new Stack<>();
  public IdentifyWhereOrderVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTWhereClause whereNode) {
    if(whereNode.getExpression() instanceof ASTNodes.ASTAndExpr) {
      CheckAndInWhereVisitor checkAndInWhereVisitor = new CheckAndInWhereVisitor(query);
      whereNode.accept(checkAndInWhereVisitor);

      if(checkAndInWhereVisitor.hasSuboptimalOrder()){
        int lineNum = ZetaSQLStringParsingHelper.countLine(query, whereNode.getParseLocationRange().start());
        result.add(String.format(WHERE_ORDER_SUGGESTION_MESSAGE, lineNum));
      }
    }
    super.visit(whereNode);
  }

  public String getResult() {
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }

  @Override
  public String getNAME() {
    return NAME;
  }
}
