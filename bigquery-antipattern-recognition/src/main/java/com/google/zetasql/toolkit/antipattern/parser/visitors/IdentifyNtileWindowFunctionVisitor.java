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
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;

public class IdentifyNtileWindowFunctionVisitor extends ParseTreeVisitor {

  private final static String NTILE_FUNCTION_STRING = "NTILE";
  private final String SUGGESTION_MESSAGE =
      "Use of NTILE window function detected at line %d. Prefer APPROX_QUANTILE if approximate bucketing is sufficient.";
  private ArrayList<String> result = new ArrayList<String>();
  private String query;

  public IdentifyNtileWindowFunctionVisitor(String query) {
    this.query = query;
  }

  public ArrayList<String> getResult() {
    return result;
  }

  // Check for the use of NTILE function among all the function calls
  @Override
  public void visit(ASTNodes.ASTFunctionCall node) {
    ImmutableList<ASTNodes.ASTIdentifier> identifiers = node.getFunction().getNames();

    for (ASTNodes.ASTIdentifier identifier : identifiers) {
      if (identifier.getIdString().equals(NTILE_FUNCTION_STRING)) {
        int lineNum = ZetaSQLStringParsingHelper.countLine(query,
            identifier.getParseLocationRange().start());
        result.add(String.format(SUGGESTION_MESSAGE, lineNum));
      }
    }
  }

}
