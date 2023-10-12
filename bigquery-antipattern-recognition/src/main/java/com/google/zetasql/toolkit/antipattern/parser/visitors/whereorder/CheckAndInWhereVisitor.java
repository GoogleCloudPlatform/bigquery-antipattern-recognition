package com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ASTNodes.ASTBinaryExpression;
import com.google.zetasql.parser.ASTNodes.ASTExpression;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class CheckAndInWhereVisitor extends ParseTreeVisitor {

  private String query;
  private Boolean insideWhere = false;
  private ArrayList<String> result = new ArrayList<String>();
  private Stack<ASTNodes.ASTWhereClause> whereNodeStack = new Stack<>();
  private String BINARY_EXPR_OP_LIKE = "LIKE";
  private String BINARY_EXPR_OP_EQ = "EQ";
  private ASTExpression likeFilterBeforeEqualFilter = null;

  public CheckAndInWhereVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTAndExpr andExprNode) {
    Iterator<ASTExpression> it = andExprNode.getConjuncts().iterator();
    ASTExpression likeFilterBeforeEqualFilterTemp = null;

    while(it.hasNext()) {
      ASTExpression astExpression = it.next();

      if(astExpression instanceof ASTNodes.ASTBinaryExpression) {
        String exprOpType = ((ASTNodes.ASTBinaryExpression) astExpression).getOp().toString();

        if(exprOpType.equals(BINARY_EXPR_OP_LIKE) && likeFilterBeforeEqualFilterTemp==null) {
          likeFilterBeforeEqualFilterTemp = astExpression;
        } else if (exprOpType.equals(BINARY_EXPR_OP_EQ) && likeFilterBeforeEqualFilterTemp!=null) {
          likeFilterBeforeEqualFilter = likeFilterBeforeEqualFilterTemp;
        }
      }
    }
  }

  @Override
  public void visit(ASTNodes.ASTQuery node) {
    // do nothing, avoid traversing sub queries
  }

  public ASTExpression getLikeFilterBeforeEqualFilter() {
    return likeFilterBeforeEqualFilter;
  }
}
