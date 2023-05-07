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
import com.google.zetasql.parser.ASTNodes.ASTTableSubquery;
import java.util.ArrayList;
import java.util.List;

public class CrossJoinSide {

  private List<String> tableNameList = new ArrayList<>();

  CrossJoinSide(ASTNode astNode) {
    setup(astNode);
  }

  private void setup(ASTNode astNode) {
    if (astNode instanceof ASTTablePathExpression) {
      tableNameList.add(
          CrossJoinUtil.getNameFromTablePathExpression((ASTTablePathExpression) astNode));
    }
    if (astNode instanceof ASTJoin) {
      ASTJoin joinNode = ((ASTJoin) astNode);
      TablesInJoinVisitor tablesInJoinVisitor = new TablesInJoinVisitor();
      joinNode.accept(tablesInJoinVisitor);
      tableNameList = tablesInJoinVisitor.getTableNameList();
    }
    if (astNode instanceof ASTTableSubquery) {
      tableNameList.add(((ASTTableSubquery) astNode).getAlias().getIdentifier().getIdString());
    }
  }

  public List<String> getTableNameList() {
    return tableNameList;
  }
}
