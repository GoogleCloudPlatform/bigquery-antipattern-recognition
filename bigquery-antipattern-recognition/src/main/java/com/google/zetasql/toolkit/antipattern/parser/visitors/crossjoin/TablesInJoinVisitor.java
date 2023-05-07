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

import com.google.zetasql.parser.ASTNode;
import com.google.zetasql.parser.ASTNodes.ASTJoin;
import com.google.zetasql.parser.ASTNodes.ASTTablePathExpression;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;
import java.util.List;

public class TablesInJoinVisitor extends ParseTreeVisitor {

  private List<String> tableNameList = new ArrayList<>();

  public List<String> getTableNameList() {
    return tableNameList;
  }

  @Override
  public void visit(ASTJoin joinNode) {
    checkSide(joinNode.getLhs());
    checkSide(joinNode.getRhs());
  }

  private void checkSide(ASTNode node) {
    if (node instanceof ASTTablePathExpression) {
      tableNameList.add(
          CrossJoinUtil.getNameFromTablePathExpression((ASTTablePathExpression) node));

    } else if (node instanceof ASTJoin) {
      super.visit((ASTJoin) node);
    }
  }
}
