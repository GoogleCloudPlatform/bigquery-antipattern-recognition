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

import static org.junit.Assert.*;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import org.junit.Before;
import org.junit.Test;

public class IdentifyCrossJoinTest {
  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void simpleCrossJoin() {
    String expected = "CROSS JOIN instead of INNER JOIN between t1 and t2.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "CROSS JOIN "
            + "    `project.dataset.table2` t2 "
            + "WHERE "
            + "   t1.col1 = t2.col1 ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendations);
  }

  @Test
  public void crossJoinTwoCrossJoinsTest() {
    String expected =
        "CROSS JOIN instead of INNER JOIN between t1 and t3.\n"
            + "CROSS JOIN instead of INNER JOIN between t1 and t2.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "CROSS JOIN "
            + "    `project.dataset.table2` t2 "
            + "CROSS JOIN "
            + "    `project.dataset.table3` t3 "
            + "WHERE "
            + "   t1.col1 = t2.col1 "
            + "   AND t1.col1 = t3.col1 ";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }

  @Test
  public void crossJoinTwoTablesNoFilterTest() {
    String expected = "";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "CROSS JOIN "
            + "    `project.dataset.table2` t2 ";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }

  @Test
  public void crossJoinTableSubqueryTest() {
    String expected = "CROSS JOIN instead of INNER JOIN between t1 and t2.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   (SELECT * FROM `project.dataset.table1`) t1 "
            + "CROSS JOIN "
            + "    `project.dataset.table2` t2 "
            + "WHERE "
            + "   t1.col1 = t2.col1 ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }

  @Test
  public void crossJoinTwoSubqueries() {
    String expected = "CROSS JOIN instead of INNER JOIN between t1 and t2.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   (SELECT * FROM `project.dataset.table1`) t1 "
            + "CROSS JOIN "
            + "    (SELECT * FROM `project.dataset.table2`) t2 "
            + "WHERE "
            + "   t1.col1 = t2.col1 ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }

  @Test
  public void crossJoinSeveralJoins() {
    String expected = "CROSS JOIN instead of INNER JOIN between t1 and t3.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "INNER JOIN "
            + "    `project.dataset.table2` t2 ON t1.col2 = t2.col2 "
            + "CROSS JOIN "
            + "    `project.dataset.table3` t3  "
            + "WHERE "
            + "   t1.col1 = t3.col1 ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }

  @Test
  public void crossJoinSeveralJoinsAndSubquery() {
    String expected = "CROSS JOIN instead of INNER JOIN between t1 and t3.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "INNER JOIN "
            + "    `project.dataset.table2` t2 ON t1.col2 = t2.col2 "
            + "CROSS JOIN "
            + "    (SELECT * FROM  `project.dataset.table3`) t3  "
            + "WHERE "
            + "   t1.col1 = t3.col1 ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }

  @Test
  public void crossJoinNestedCrossJoin() {
    String expected = "CROSS JOIN instead of INNER JOIN between t1 and t2.";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "INNER JOIN "
            + "    `project.dataset.table2` t2 ON t1.col2 = t2.col2 "
            + "INNER JOIN "
            + "    `project.dataset.table3` t3 ON t1.col1 = t3.col1 "
            + "INNER JOIN "
            + " ( "
            + "   SELECT "
            + "      t1.col1 "
            + "    FROM "
            + "       `project.dataset.table1` t1 "
            + "    INNER JOIN "
            + "       `project.dataset.table3` t3 ON t1.col1 = t3.col1 "
            + "    CROSS JOIN "
            + "       `project.dataset.table2` t2 "
            + "    WHERE "
            + "        t1.col1 = t2.col1 "
            + " ) t11 ON t1.col1 = t11.col1 "
            + "WHERE "
            + "   t1.col1 = t3.col1 ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCrossJoin()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }
}
