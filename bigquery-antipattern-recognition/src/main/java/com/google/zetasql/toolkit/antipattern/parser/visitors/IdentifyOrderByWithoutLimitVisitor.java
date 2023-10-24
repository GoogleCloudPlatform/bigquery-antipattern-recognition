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
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class IdentifyOrderByWithoutLimitVisitor extends AbstractVisitor {

  public static final String NAME = "OrderByWithoutLimit";

  private final String ORDER_BY_SUGGESTION_MESSAGE = "ORDER BY clause without LIMIT at line %d.";
  private ArrayList<String> result = new ArrayList<String>();
  private ArrayList<String> resultToReturn = new ArrayList<String>();
  private Boolean LimitExist = Boolean.FALSE;
  private String query;

  public IdentifyOrderByWithoutLimitVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTQuery node) {
    if (!(node.getOrderBy() == null) && (node.getLimitOffset() == null)) {
      int lineNum = ZetaSQLStringParsingHelper.countLine(query, node.getOrderBy().getParseLocationRange().start());
      result.add(String.format(ORDER_BY_SUGGESTION_MESSAGE, lineNum));
    }
    super.visit(node);
  }

  public String getResult() {
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }
}
