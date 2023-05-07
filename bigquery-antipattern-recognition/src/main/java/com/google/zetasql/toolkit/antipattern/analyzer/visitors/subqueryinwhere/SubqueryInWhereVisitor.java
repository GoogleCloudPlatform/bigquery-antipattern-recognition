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

package com.google.zetasql.toolkit.antipattern.analyzer.visitors.subqueryinwhere;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedAggregateScanBase;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFunctionCall;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedSubqueryExpr;
import java.util.ArrayList;
import java.util.List;

public class SubqueryInWhereVisitor extends ResolvedNodes.Visitor {

  private static final String SUBQUERY_TYPE_IN = "IN";
  private List<String> subQueriesInWhereWithoutAgg = new ArrayList<>();

  public void visit(ResolvedFunctionCall resolvedFunctionCall) {
    resolvedFunctionCall
        .getArgumentList()
        .forEach(
            resolvedExpr -> {
              if (resolvedExpr instanceof ResolvedSubqueryExpr) {
                ResolvedSubqueryExpr resolvedSubqueryExpr = (ResolvedSubqueryExpr) resolvedExpr;
                if (resolvedSubqueryExpr.getSubqueryType().toString().equals(SUBQUERY_TYPE_IN)
                    && !(resolvedSubqueryExpr.getSubquery() instanceof ResolvedAggregateScanBase)) {
                  ColumnsOfInExprVisitor columnsOfInExprVisitor = new ColumnsOfInExprVisitor();
                  resolvedSubqueryExpr.getInExpr().accept(columnsOfInExprVisitor);
                  subQueriesInWhereWithoutAgg.add(columnsOfInExprVisitor.getResult());
                }
              }
            });
    super.visit(resolvedFunctionCall);
  }

  public List<String> getResult() {
    return subQueriesInWhereWithoutAgg;
  }
}
