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
import com.google.zetasql.parser.ASTNodes.ASTPathExpression;
import com.google.zetasql.parser.ParseTreeVisitor;

public class CrossJoinFilterChecker extends ParseTreeVisitor {

  boolean foundFilterForCrossJoin;
  CrossJoin crossJoin;

  public void setCrossJoin(CrossJoin crossJoin) {
    this.crossJoin = crossJoin;
  }

  public void visit(ASTNodes.ASTBinaryExpression node) {
    if (node.getOp().toString().equals("EQ")) {
      if (node.getLhs() instanceof ASTPathExpression
          && node.getRhs() instanceof ASTPathExpression) {
        checkFilter(node);
      }
    }
    super.visit(node);
  }

  public void checkFilter(ASTNodes.ASTBinaryExpression node) {
    boolean foundFilterLeftSideOfJoin = false;
    boolean foundFilterRightSideOfJoin = false;

    String lhsFilterTableName = ((ASTPathExpression) node.getLhs()).getNames().get(0).getIdString();
    String rhsFilterTableName = ((ASTPathExpression) node.getRhs()).getNames().get(0).getIdString();

    String tempLeftTable = null;
    String tempRightTable = null;

    if (crossJoin.getLeft().getTableNameList().contains(lhsFilterTableName)) {
      foundFilterLeftSideOfJoin = true;
      tempLeftTable = lhsFilterTableName;
    } else if (crossJoin.getLeft().getTableNameList().contains(rhsFilterTableName)) {
      foundFilterLeftSideOfJoin = true;
      tempLeftTable = rhsFilterTableName;
    }

    if (crossJoin.getRight().getTableNameList().contains(lhsFilterTableName)) {
      foundFilterRightSideOfJoin = true;
      tempRightTable = lhsFilterTableName;
    } else if (crossJoin.getRight().getTableNameList().contains(rhsFilterTableName)) {
      foundFilterRightSideOfJoin = true;
      tempRightTable = rhsFilterTableName;
    }

    if (foundFilterLeftSideOfJoin && foundFilterRightSideOfJoin) {
      foundFilterForCrossJoin = true;
      crossJoin.addTableName(tempLeftTable);
      crossJoin.addTableName(tempRightTable);
    }
  }

  public boolean result() {
    return foundFilterForCrossJoin;
  }
}
