package com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder;

import autovalue.shaded.com.google.common.collect.Ordering;
import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ASTNodes.ASTExpression;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import com.google.zetasql.parser.ASTBinaryExpressionEnums.Op;

public class CheckAndInWhereVisitor extends ParseTreeVisitor {

  private String query;
  private Stack<ASTNodes.ASTWhereClause> whereNodeStack = new Stack<>();
  private Boolean hasSuboptimalOrder = false;

  private static final Map<Op, Integer> OPCODE_COSTS = new HashMap<>();
  static {
    OPCODE_COSTS.put(Op.EQ, 1);
    OPCODE_COSTS.put(Op.GT, 2);
    OPCODE_COSTS.put(Op.LT, 2);
    OPCODE_COSTS.put(Op.GE, 3);
    OPCODE_COSTS.put(Op.LE, 3);
    OPCODE_COSTS.put(Op.NE, 5);
    OPCODE_COSTS.put(Op.NE2, 5);
    OPCODE_COSTS.put(Op.LIKE,6);
  }

  private static final Comparator<Op> COMPARATOR = (e0, e1) -> Integer.compare(
      OPCODE_COSTS.get(e0), OPCODE_COSTS.get(e1));

  public CheckAndInWhereVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTAndExpr andExprNode) {
    Iterator<ASTExpression> andNodeExprIterator = andExprNode.getConjuncts().iterator();

    List<Op> whereOpTypeList = new ArrayList<>();

    while(andNodeExprIterator.hasNext()) {
      ASTExpression astExpression = andNodeExprIterator.next();
      if(astExpression instanceof ASTNodes.ASTBinaryExpression) {
        Op op = ((ASTNodes.ASTBinaryExpression) astExpression).getOp();
        if(OPCODE_COSTS.get(op)!=null){
          whereOpTypeList.add(op);
        }
      }
    }
    if(!Ordering.from(COMPARATOR).isOrdered(whereOpTypeList)) {
      hasSuboptimalOrder = true;
    }

  }

  @Override
  public void visit(ASTNodes.ASTQuery node) {
    // do nothing, avoid traversing sub queries
  }

  public Boolean hasSuboptimalOrder() {
    return hasSuboptimalOrder;
  }
}
