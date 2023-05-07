/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zetasql.toolkit.antipattern.parser.visitors.crossjoin;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ASTNodes.ASTWhereClause;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;
import java.util.Stack;

public class IdentifyCrossJoinVisitor extends ParseTreeVisitor {

  private static final String JOIN_TYPE_CROSS = "CROSS";
  private static final String CROSS_JOIN_MESSAGE =
      "CROSS JOIN instead of INNER JOIN between %s and %s.";

  private Stack<ASTWhereClause> filterStack = new Stack<ASTWhereClause>();

  private ArrayList<String> result = new ArrayList<String>();

  public ArrayList<String> getResult() {
    return result;
  }

  @Override
  public void visit(ASTNodes.ASTJoin joinNode) {
    if (joinNode.getJoinType().toString().equals(JOIN_TYPE_CROSS)
        || (joinNode.getJoinType().toString().equals("DEFAULT_JOIN_TYPE")
            && joinNode.getOnClause() == null)) {
      CrossJoinSide lhs = new CrossJoinSide(joinNode.getLhs());
      CrossJoinSide rhs = new CrossJoinSide(joinNode.getRhs());
      CrossJoin crossJoin = new CrossJoin(lhs, rhs);
      CrossJoinFilterChecker crossJoinFilterChecker = new CrossJoinFilterChecker();
      crossJoinFilterChecker.setCrossJoin(crossJoin);
      if (!filterStack.empty()) {
        filterStack.peek().accept(crossJoinFilterChecker);
        if (crossJoinFilterChecker.result()) {
          result.add(
              String.format(
                  CROSS_JOIN_MESSAGE,
                  crossJoin.getNamesTablesUsedOnFilter().toArray(new String[0])));
        }
      }
    }
    super.visit(joinNode);
  }

  @Override
  public void visit(ASTNodes.ASTSelect node) {
    ASTWhereClause whereNode = node.getWhereClause();
    if (!(whereNode == null)) {
      filterStack.add(whereNode);
      super.visit(node);
      filterStack.pop();
    } else {
      super.visit(node);
    }
  }
}
