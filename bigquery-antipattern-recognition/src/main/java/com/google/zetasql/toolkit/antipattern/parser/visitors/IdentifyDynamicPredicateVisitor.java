/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class IdentifyDynamicPredicateVisitor extends ParseTreeVisitor implements
    AntiPatternVisitor {

  public final static String NAME = "DynamicPredicate";
  private String query;
  private Boolean insideWhere = false;
  private final String DYNAMIC_PREDICATE_SUGGESTION_MESSAGE = "Using subquery in filter at line %d. Converting this dynamic predicate to static might provide better performance.";
  private ArrayList<String> result = new ArrayList<String>();

  public IdentifyDynamicPredicateVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTSelect selectNode) {
    if(insideWhere) {
      int location = ZetaSQLStringParsingHelper.countLine(query, selectNode.getParseLocationRange().start());
      result.add(String.format(DYNAMIC_PREDICATE_SUGGESTION_MESSAGE, location));
    }
    super.visit(selectNode);
  }

  @Override
  public void visit(ASTNodes.ASTWhereClause whereNode) {
    this.insideWhere = true;
    super.visit(whereNode);
    this.insideWhere = false;
  }

  public String getResult() {
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }

  @Override
  public String getNAME() {
    return NAME;
  }
}
