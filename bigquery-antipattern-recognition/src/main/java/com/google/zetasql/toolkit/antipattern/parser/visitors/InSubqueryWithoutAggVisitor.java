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

import com.google.zetasql.parser.ASTNodes.ASTInExpression;
import com.google.zetasql.parser.ASTNodes.ASTSelect;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;

public class InSubqueryWithoutAggVisitor extends ParseTreeVisitor {

  private final String SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE =
      "Subquery in the WHERE clause without aggregation.";

  private ArrayList<String> result = new ArrayList<String>();

  @Override
  public void visit(ASTInExpression node) {
    if (!(node.getQuery() == null)) {
      if (node.getQuery().getQueryExpr() instanceof ASTSelect) {
        ASTSelect select = (ASTSelect) node.getQuery().getQueryExpr();
        if ((!select.getDistinct()) && select.getGroupBy() == null) {
          result.add(SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE);
        }
      }
    }
    super.visit(node);
  }

  public ArrayList<String> getResult() {
    return result;
  }
}
