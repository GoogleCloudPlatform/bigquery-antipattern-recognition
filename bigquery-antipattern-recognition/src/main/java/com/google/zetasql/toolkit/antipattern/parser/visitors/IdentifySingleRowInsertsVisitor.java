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

import com.google.common.collect.ImmutableList;
import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;

public class IdentifySingleRowInsertsVisitor extends ParseTreeVisitor {

  private static final String SINGLE_ROW_INSERTS = "SINGLE ROW INSERTS";
  private static final String OTHER_INSERT_PATTERN = "OTHER INSERT PATTERN";
  private ArrayList<String> result = new ArrayList<String>();

  public ArrayList<String> getResult() {
    return result;
  }

  @Override
  public void visit(ASTNodes.ASTInsertValuesRowList node) {
    ImmutableList<ASTNodes.ASTInsertValuesRow> nodes = node.getRows();
    if (nodes.size() == 1) {
      result.add(String.format(SINGLE_ROW_INSERTS));
    }
  }
}
