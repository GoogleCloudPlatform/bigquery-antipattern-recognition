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
import com.google.zetasql.parser.ASTNodes.ASTExpression;
import com.google.zetasql.parser.ASTNodes.ASTGroupingItem;
import com.google.zetasql.parser.ASTNodes.ASTIdentifier;
import com.google.zetasql.parser.ASTNodes.ASTJoin;
import com.google.zetasql.parser.ASTNodes.ASTOnClause;
import com.google.zetasql.parser.ASTNodes.ASTOnOrUsingClauseList;
import com.google.zetasql.parser.ASTNodes.ASTPathExpression;
import com.google.zetasql.parser.ASTNodes.ASTSelect;
import com.google.zetasql.parser.ASTNodes.ASTTablePathExpression;
import com.google.zetasql.parser.ASTNodes.ASTTableSubquery;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class IdentifyAggAfterJoinVisitor extends ParseTreeVisitor {

  private final String SUGGESTION_MESSAGE =
      "GROUP BY found at line number %s after JOIN at line number %s. Consider applying aggregation before joining to reduce join data";

  private String query;
  private boolean foundGroupBy = false;
  private boolean foundJoin = false;

  private int joinLocation=0;
  private int groupByLocation=0;
  private ArrayList<String> result = new ArrayList<String>();

  private Set<String> lhsJoinKeys = new HashSet<>();
  private Set<String> rhsJoinKeys = new HashSet<>();

  private Set<String> groupByKeys = new HashSet<>();

  public IdentifyAggAfterJoinVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTSelect selectNode) {

    // Traverse from the select node to check if a join exists and set the foundJoin flag
    ASTNodes.ASTTableExpression tableExpression = selectNode.getFromClause().getTableExpression();
    if (tableExpression instanceof ASTNodes.ASTJoin) {
      foundJoin = true;
      joinLocation = ZetaSQLStringParsingHelper.countLine(query, tableExpression.getParseLocationRange().start());

      ASTOnClause onClause = ((ASTJoin) tableExpression).getOnClause();

      if (onClause.getExpression() instanceof ASTNodes.ASTAndExpr) {
        ASTNodes.ASTAndExpr andNode = (ASTNodes.ASTAndExpr) onClause.getExpression();
        for (ASTNodes.ASTExpression elementNode : andNode.getConjuncts()) {
          visit((ASTNodes.ASTBinaryExpression) elementNode);
        }
      } else {
        ASTNodes.ASTBinaryExpression node = (ASTNodes.ASTBinaryExpression) onClause.getExpression();
        visit(node);
      }
    }

    // Check if group by exists and set the flag accordingly
    if (foundJoin && selectNode.getGroupBy() != null) {

      foundGroupBy = true;
      groupByLocation = ZetaSQLStringParsingHelper.countLine(query, selectNode.getGroupBy().getParseLocationRange().start());
      for (ASTGroupingItem groupingItem : selectNode.getGroupBy().getGroupingItems()) {
        ASTNodes.ASTPathExpression groupPath = (ASTNodes.ASTPathExpression) groupingItem.getExpression();

        if (groupPath.getNames().size() > 1) {
          groupByKeys.add(groupPath.getNames().get(1).getIdString());
        } else {
          groupByKeys.add(groupPath.getNames().get(0).getIdString());
        }

      }

    }

    // Re-route to select node if subquery exists
    if(tableExpression instanceof ASTTableSubquery)
    {
      if (((ASTTableSubquery) tableExpression).getSubquery().getQueryExpr() instanceof ASTSelect) {
        ASTSelect subQuerySelectNode = (ASTSelect) ((ASTTableSubquery) tableExpression).getSubquery().getQueryExpr();
        visit(subQuerySelectNode);
      }
    }

  }

  // Traverse LHS and RHS of the on clause of each join and collect the keys in corresponding hashsets
  @Override
  public void visit(ASTNodes.ASTBinaryExpression node) {

    ASTNodes.ASTPathExpression lhsPath = (ASTNodes.ASTPathExpression) node.getLhs();
    ASTNodes.ASTPathExpression rhsPath = (ASTNodes.ASTPathExpression) node.getRhs();

    if (lhsPath.getNames().size() > 1) {
      lhsJoinKeys.add(lhsPath.getNames().get(1).getIdString());
    } else {
      lhsJoinKeys.add(lhsPath.getNames().get(0).getIdString());
    }

    if (rhsPath.getNames().size() > 1) {
      rhsJoinKeys.add(rhsPath.getNames().get(1).getIdString());
    } else {
      rhsJoinKeys.add(rhsPath.getNames().get(0).getIdString());
    }

    super.visit(node);
  }

  // If both foundJoin and foundGroupBy are true, compare lhs and rhs join keys sets with grouping keys set
  public ArrayList<String> getResult() {
    if (foundJoin && foundGroupBy) {
      Set<String> lhsGroupByIntersection = new HashSet<String>(lhsJoinKeys);
      lhsGroupByIntersection.retainAll(groupByKeys);

      Set<String> rhsGroupByIntersection = new HashSet<String>(rhsJoinKeys);
      rhsGroupByIntersection.retainAll(groupByKeys);

      if (lhsGroupByIntersection.size() > 0 || rhsGroupByIntersection.size() > 0) {
        result.add(
            String.format(
                SUGGESTION_MESSAGE,
                groupByLocation,
                joinLocation));
      }
      return result;
    } else {
      return new ArrayList<String>();
    }
  }
}
