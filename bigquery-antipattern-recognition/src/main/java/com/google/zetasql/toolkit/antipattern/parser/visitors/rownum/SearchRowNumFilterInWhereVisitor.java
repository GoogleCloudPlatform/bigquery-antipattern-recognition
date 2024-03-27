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

package com.google.zetasql.toolkit.antipattern.parser.visitors.rownum;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;

public class SearchRowNumFilterInWhereVisitor extends ParseTreeVisitor {

  private String rowNumColAlias;
  private boolean foundFilter = false;
  private ASTNodes.ASTBinaryExpression filterWithRowNumNode = null;

  public SearchRowNumFilterInWhereVisitor(String rowNumColAlias) {
    this.rowNumColAlias = rowNumColAlias;
  }

  public void visit(ASTNodes.ASTBinaryExpression filterNode) {
    if(foundFilter) {
      return;
    }
    ASTNodes.ASTExpression lhs = filterNode.getLhs();
    ASTNodes.ASTExpression rhs = filterNode.getRhs();
    if(lhs instanceof ASTNodes.ASTPathExpression && rhs instanceof ASTNodes.ASTIntLiteral) {
      ASTNodes.ASTPathExpression lhsPathExp = (ASTNodes.ASTPathExpression) lhs;
      ASTNodes.ASTIntLiteral rhsIntLit = (ASTNodes.ASTIntLiteral) rhs;
      if(lhsPathExp.getNames().get(0).getIdString().equals(rowNumColAlias) && rhsIntLit.getImage().equals("1")){
        foundFilter = true;
        filterWithRowNumNode = filterNode;
      }
    }
  }

  public Boolean getfoundFilter() {
    return foundFilter;
  }

  public ASTNodes.ASTBinaryExpression getFilterWithRowNumNode() {
    return filterWithRowNumNode;
  }
}
