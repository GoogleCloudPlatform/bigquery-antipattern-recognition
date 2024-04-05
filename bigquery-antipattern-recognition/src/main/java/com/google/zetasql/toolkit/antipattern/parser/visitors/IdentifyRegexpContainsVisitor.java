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

import com.google.common.collect.ImmutableList;
import com.google.zetasql.parser.ASTNodes;

import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IdentifyRegexpContainsVisitor extends ParseTreeVisitor implements AntiPatternVisitor {

  public static final String NAME = "StringComparison";
  private static final String REGEXP_CONTAINS_ANTI_PATTERN_MESSAGE =
      "REGEXP_CONTAINS at line %d. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
  private static final String REGEXP_CONTAINS_FUN_ID_STR = "regexp_contains";
  private static final String REGEX_STRING = "['\"]\\.\\*.*\\.\\*['\"]";
  private ArrayList<String> result = new ArrayList<String>();
  private String query;

  public String getResult() {
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }

  public IdentifyRegexpContainsVisitor(String query) {
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTFunctionCall node) {
    ImmutableList<ASTNodes.ASTIdentifier> identifiers = node.getFunction().getNames();
    for (ASTNodes.ASTIdentifier identifier : identifiers) {
      // search for regexp_contains
      if (identifier.getIdString().toLowerCase().equals(REGEXP_CONTAINS_FUN_ID_STR)) {
        ImmutableList<ASTNodes.ASTExpression> arguments = node.getArguments();
        // search for argument with string
        for (ASTNodes.ASTExpression argument : arguments) {
          if (argument instanceof ASTNodes.ASTStringLiteral) {
            String stringLiteralArg = ((ASTNodes.ASTStringLiteral) argument).getImage();
            Pattern pattern = Pattern.compile(REGEX_STRING);
            Matcher matcher = pattern.matcher(stringLiteralArg);
            if (matcher.find()) {
              int lineNum =
                  ZetaSQLStringParsingHelper.countLine(
                      query, identifier.getParseLocationRange().start());
              result.add(String.format(REGEXP_CONTAINS_ANTI_PATTERN_MESSAGE, lineNum));
            }
          }
        }
      }
    }
  }

  @Override
  public String getName() {
    return NAME;
  }
}
