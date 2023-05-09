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
import org.junit.Before;
import org.junit.Test;

public class IdentifyInSubqueryWithoutAggTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void BasicInExpressionTest() {
    String expected = "Subquery in filter without aggregation at line 6.";
    String query =
        "SELECT \n"
            + "   t1.col1 \n"
            + "FROM \n"
            + "   `project.dataset.table1` t1 \n"
            + "WHERE \n"
            + "    t1.col2 IN (SELECT col2 FROM `project.dataset.table2`) ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyInSubqueryWithoutAgg().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void BasicNotInExpressionTest() {
    String expected = "Subquery in filter without aggregation at line 6.";
    String query =
        "SELECT \n"
            + "   t1.col1 \n"
            + "FROM \n"
            + "   `project.dataset.table1` t1 \n"
            + "WHERE \n"
            + "    t1.col2 NOT IN (SELECT col2 FROM `project.dataset.table2`) ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyInSubqueryWithoutAgg().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void InExpressionWithOtherFiltersTest() {
    String expected = "Subquery in filter without aggregation at line 7.";
    String query =
        "SELECT \n"
            + "   t1.col1 \n"
            + "FROM \n"
            + "   `project.dataset.table1` t1 \n"
            + "WHERE \n"
            + "   t1.col1 > 0 \n"
            + "   AND t1.col2 IN (SELECT col2 FROM `project.dataset.table2`) \n"
            + "   AND t1.col3 = 1";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyInSubqueryWithoutAgg().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void noAntiPatternTest() {
    String expected = "";
    String query =
        "SELECT \n"
            + "   t1.col1 \n"
            + "FROM \n"
            + "   `project.dataset.table1` t1 \n"
            + "WHERE \n"
            + "   t1.col1 > 0 \n"
            + "   AND t1.col2 IN (SELECT DISTINCT col2 FROM `project.dataset.table2`) \n"
            + "   AND t1.col3 IN (SELECT col3 FROM `project.dataset.table3` GROUP BY col3) \n"
            + "   AND t1.col4 IN (1, 2, 3, 4)";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyInSubqueryWithoutAgg().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void inExpressionWithStructTest() {
    String expected = "Subquery in filter without aggregation at line 6.";
    String query =
        "SELECT \n"
            + "   t1.col1 \n"
            + "FROM \n"
            + "   `project.dataset.table1` t1 \n"
            + "WHERE \n"
            + "   (t1.col1,t1.col2) IN (SELECT (col1,col2) FROM `project.dataset.table3`) ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyInSubqueryWithoutAgg().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }
}
