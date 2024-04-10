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
import com.google.zetasql.parser.ASTNodes.ASTSelect;
import com.google.zetasql.parser.ASTNodes.ASTTablePathExpression;
import com.google.zetasql.parser.ASTNodes.ASTWithClause;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class IdentifyCTEsEvalMultipleTimesVisitor extends ParseTreeVisitor
    implements AntiPatternVisitor {

  public static final String NAME = "CTEsEvalMultipleTimes";
  private final String MULTIPLE_CTE_SUGGESTION_MESSAGE =
      "CTE with multiple references: alias %s defined at line %d is referenced %d times.";

  // An array list to store the suggestions.
  private final ArrayList<String> result = new ArrayList<>();

  // A map to keep track of the number of times each CTE is evaluated.
  private final Map<String, Integer> cteCountMap = new HashMap<>();

  // A map to keep track of the number of times each CTE is evaluated.
  private final Map<String, Integer> cteStartPositionMap = new HashMap<>();

  private String query;

  public IdentifyCTEsEvalMultipleTimesVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTWithClause withClause) {

    // Loop through all the CTE entries in the WITH clause.
    withClause
        .getWith()
        .forEach(
            alias -> {
              // Add the CTE name to the count map with initial count 0.
              cteCountMap.put(alias.getAlias().getIdString().toLowerCase(), 0);
              cteStartPositionMap.put(
                  alias.getAlias().getIdString().toLowerCase(),
                  alias.getParseLocationRange().start());
              // Visit from and fetch tablename
              if (alias.getQuery().getQueryExpr() instanceof ASTSelect) {
                ASTNodes.ASTTableExpression tableExpression =
                    ((ASTSelect) alias.getQuery().getQueryExpr())
                        .getFromClause()
                        .getTableExpression();
                visit(tableExpression);
              }
            });
  }

  // fetch table names from, FROM clause
  public void visit(ASTNodes.ASTTableExpression tableExpression) {
    if (tableExpression instanceof ASTNodes.ASTTablePathExpression) {
      visit((ASTTablePathExpression) tableExpression);
    } else if (tableExpression instanceof ASTNodes.ASTJoin) {
      visit(((ASTNodes.ASTJoin) tableExpression).getLhs());
      visit(((ASTNodes.ASTJoin) tableExpression).getRhs());
    } else if (tableExpression instanceof ASTNodes.ASTTableSubquery) {
      ASTNodes.ASTQueryExpression queryExpression =
          ((ASTNodes.ASTTableSubquery) tableExpression).getSubquery().getQueryExpr();
      if (queryExpression instanceof ASTNodes.ASTSelect) {
        ASTNodes.ASTTableExpression tableExpression1 =
            ((ASTSelect) queryExpression).getFromClause().getTableExpression();
        visit(tableExpression1);
      }
    }
  }

  // Fetch table names and count occurrence of it
  public void visit(ASTTablePathExpression tablePathExpression) {
    // Loop through all the identifiers in the table path expression.
    tablePathExpression
        .getPathExpr()
        .getNames()
        .forEach(
            identifier -> {
              // Get the identifier as a string in lower case.
              String table = identifier.getIdString().toLowerCase();
              // If the count map contains the identifier, increment its count.
              if (cteCountMap.containsKey(table)) {
                cteCountMap.put(table, cteCountMap.get(table) + 1);
              }
            });
  }

  // Getter method to retrieve the list of suggestion messages.
  public String getResult() {
    // Loop through all the entries in the count map.
    for (Map.Entry<String, Integer> entry : cteCountMap.entrySet()) {
      // Get the CTE name and its count.
      String cteName = entry.getKey();
      int count = entry.getValue();
      // If the CTE count is greater than 1, add the suggestion message to the list.
      if (count > 1) {
        int lineNum = ZetaSQLStringParsingHelper.countLine(query, cteStartPositionMap.get(cteName));
        result.add(String.format(MULTIPLE_CTE_SUGGESTION_MESSAGE, cteName, lineNum, count));
      }
    }
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }

  @Override
  public String getName() {
    return NAME;
  }
}
