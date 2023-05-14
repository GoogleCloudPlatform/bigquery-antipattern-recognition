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

package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ASTNodes.ASTTablePathExpression;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;

public class IdentifySimpleSelectStarVisitor extends ParseTreeVisitor {

  private final String SUGGESTION_MESSAGE =
      "SELECT * on table: %s. Check that all columns are needed.";

  private final String SELECT_STAR_NODE_KIND_STRING = "Star";
  private boolean foundFrom = false;
  private boolean foundJoin = false;
  private boolean isSimpleSelect = true;
  private ArrayList<String> result = new ArrayList<String>();

  @Override
  public void visit(ASTNodes.ASTSelect selectNode) {
    selectNode
        .getSelectList()
        .getColumns()
        .forEach(
            selectColumnNode -> {
              if (selectColumnNode
                  .getExpression()
                  .nodeKindString()
                  .equals(SELECT_STAR_NODE_KIND_STRING)) {
                if (selectNode.getFromClause().getTableExpression()
                    instanceof ASTTablePathExpression) {
                  String idString =
                      ((ASTTablePathExpression) selectNode.getFromClause().getTableExpression())
                          .getPathExpr()
                          .getNames()
                          .get(0)
                          .getIdString();
                  result.add(String.format(SUGGESTION_MESSAGE, idString));
                }
              }
            });
    super.visit(selectNode);
  }

  @Override
  public void visit(ASTNodes.ASTFromClause node) {
    if ((!foundFrom) && isSimpleSelect) {
      foundFrom = true;
      super.visit(node);
    } else {
      isSimpleSelect = false;
    }
  }

  @Override
  public void visit(ASTNodes.ASTGroupBy node) {
    isSimpleSelect = false;
  }

  @Override
  public void visit(ASTNodes.ASTJoin node) {
    if ((!foundJoin) && isSimpleSelect) {
      foundJoin = true;
      super.visit(node);
    } else {
      isSimpleSelect = false;
    }
  }

  public ArrayList<String> getResult() {
    if (isSimpleSelect) {
      return result;
    } else {
      return new ArrayList<String>();
    }
  }
}
