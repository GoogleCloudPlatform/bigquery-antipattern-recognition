package com.google.zetasql.toolkit.antipattern.parser.visitors.rownum;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;

public class SearchRowNumFilterInWhereVisitor extends ParseTreeVisitor {

  private String rowNumColAlias;
  private boolean foundFilter = false;
  private ASTNodes.ASTBinaryExpression filterWithRowNumNode = null;

  public SearchRowNumFilterInWhereVisitor(String rowNumColAlias) {
    this.rowNumColAlias = rowNumColAlias;
  }

  public void visit(ASTNodes.ASTBinaryExpression filterNode) {
    if(foundFilter) {
      return;
    }
    ASTNodes.ASTExpression lhs = filterNode.getLhs();
    ASTNodes.ASTExpression rhs = filterNode.getRhs();
    if(lhs instanceof ASTNodes.ASTPathExpression && rhs instanceof ASTNodes.ASTIntLiteral) {
      ASTNodes.ASTPathExpression lhsPathExp = (ASTNodes.ASTPathExpression) lhs;
      ASTNodes.ASTIntLiteral rhsIntLit = (ASTNodes.ASTIntLiteral) rhs;
      if(lhsPathExp.getNames().get(0).getIdString().equals(rowNumColAlias) && rhsIntLit.getImage().equals("1")){
        foundFilter = true;
        filterWithRowNumNode = filterNode;
      }
    }
  }

  public Boolean getfoundFilter() {
    return foundFilter;
  }

  public ASTNodes.ASTBinaryExpression getFilterWithRowNumNode() {
    return filterWithRowNumNode;
  }
}
