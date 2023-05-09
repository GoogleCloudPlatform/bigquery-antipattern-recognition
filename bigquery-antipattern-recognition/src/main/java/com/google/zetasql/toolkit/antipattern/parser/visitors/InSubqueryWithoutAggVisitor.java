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
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;

public class InSubqueryWithoutAggVisitor extends ParseTreeVisitor {

  private final String SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE =
      "Subquery in filter without aggregation at line %d.";

  private String query;
  private ArrayList<String> result = new ArrayList<String>();

  public InSubqueryWithoutAggVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTInExpression node) {
    if (!(node.getQuery() == null)) {
      if (node.getQuery().getQueryExpr() instanceof ASTSelect) {
        ASTSelect select = (ASTSelect) node.getQuery().getQueryExpr();
        if ((!select.getDistinct()) && select.getGroupBy() == null) {
          int lineNum = ZetaSQLStringParsingHelper.countLine(query, select.getParseLocationRange().start());
          result.add(String.format(SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE, lineNum));
        }
      }
    }
    super.visit(node);
  }

  public ArrayList<String> getResult() {
    return result;
  }
}
