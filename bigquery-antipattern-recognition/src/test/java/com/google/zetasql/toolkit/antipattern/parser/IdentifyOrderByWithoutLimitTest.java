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

package com.google.zetasql.toolkit.antipattern.parser;

import static org.junit.Assert.assertEquals;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyOrderByWithoutLimitVisitor;
import org.junit.Before;
import org.junit.Test;

public class IdentifyOrderByWithoutLimitTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void OrderByWithoutLimitTest() {
    String expected = "ORDER BY clause without LIMIT at line 5.";
    String query = "SELECT \n"
        + "t1.col1 \n"
        + "FROM \n"
        + "`project.dataset.table1` t1 \n"
        + "ORDER BY t1.col1;";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    IdentifyOrderByWithoutLimitVisitor visitor = new IdentifyOrderByWithoutLimitVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void OrderByWithLimitTest() {
    String expected = "";
    String query = "SELECT t1.col1 FROM `project.dataset.table1` t1 ORDER BY t1.col1 LIMIT 5;";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    IdentifyOrderByWithoutLimitVisitor visitor = new IdentifyOrderByWithoutLimitVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void OrderByWithoutLimitOuterLayerTest() {
    String expected = "ORDER BY clause without LIMIT at line 13.";
    String query =
        "SELECT \n"
            + "  t1.col1, t1.col2, t2.col1\n"
            + "FROM \n"
            + "  table1 t1\n"
            + "LEFT JOIN\n"
            + "  (\n"
            + "    SELECT\n"
            + "      col1, col2\n"
            + "    FROM\n"
            + "      table2\n"
            + "    LIMIT 1000\n"
            + "  ) t2 ON t1.col1=t2.col1\n"
            + "ORDER BY\n"
            + "  t1.col1";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    IdentifyOrderByWithoutLimitVisitor visitor = new IdentifyOrderByWithoutLimitVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }
}
