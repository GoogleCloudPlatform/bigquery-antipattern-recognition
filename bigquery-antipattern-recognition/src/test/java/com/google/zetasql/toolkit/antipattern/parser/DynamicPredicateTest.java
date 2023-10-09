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

public class DynamicPredicateTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void SimpleDynamicPredicateTest() {
    String expected = "Using subquery in filter at line 6. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT\n"
        + "    col1, col2 \n"
        + "FROM \n"
        + "    table1 \n"
        + "WHERE \n"
        + "    col3 in (select col3 from table2)";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void DynamicPredicateWithSeveralFiltersTest() {
    String expected = "Using subquery in filter at line 7. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT\n"
        + "    col1, col2 \n"
        + "FROM \n"
        + "    table1 \n"
        + "WHERE \n"
        + "    col1 > 0\n"
        + "    AND col3 in (select col3 from table2)\n"
        + "    AND col2 = 1";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void SeveralDynamicPredicatesTest() {
    String expected = "Using subquery in filter at line 7. Converting this dynamic predicate to static might provide better performance.\n"
        + "Using subquery in filter at line 8. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT\n"
        + "    col1, col2 \n"
        + "FROM \n"
        + "    table1 \n"
        + "WHERE \n"
        + "    col1 > 0\n"
        + "    AND col3 in (select col3 from table2)\n"
        + "    AND col3 in (select col3 from table3)\n"
        + "    AND col2 = 1";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void SeveralDynamicPredicatesWithJoinTest() {
    String expected = "Using subquery in filter at line 11. Converting this dynamic predicate to static might provide better performance.\n"
        + "Using subquery in filter at line 12. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT\n"
        + "    t1.col1, t1.col2, t2.col4, t3.col5 \n"
        + "FROM \n"
        + "    table1 t1\n"
        + "JOIN\n"
        + "    table2 t2 ON t1.col1 = t2.col2\n"
        + "JOIN\n"
        + "    table3 t3 ON t1.col1 = t3.col2\n"
        + "WHERE \n"
        + "    t1.col1 > 0\n"
        + "    AND t1.col3 in (select col3 from table2)\n"
        + "    AND t1.col3 in (select col3 from table3)\n"
        + "    AND t1.col2 = 1";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void DynamicPredicateInSubqueryTest() {
    String expected = "Using subquery in filter at line 7. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT * FROM\n"
        + "("
        + " SELECT\n"
        + "    col1, col2 \n"
        + " FROM \n"
        + "    table1 \n"
        + " WHERE \n"
        + "    col3 in (select col3 from table2)\n"
        + ")\n"
        + "WHERE col1 > 1;";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void SeveralDynamicPredicateInSubqueryTest() {
    String expected = "Using subquery in filter at line 7. Converting this dynamic predicate to static might provide better performance.\n"
        + "Using subquery in filter at line 9. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT * FROM\n"
            + "("
            + " SELECT\n"
            + "    col1, col2 \n"
            + " FROM \n"
            + "    table1 \n"
            + " WHERE \n"
            + "    col3 in (select col3 from table2)\n"
            + ")\n"
            + "WHERE col1 in (select col1 from table3);";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void DynamicPredicateWithJoinTest() {
    String expected = "Using subquery in filter at line 10. Converting this dynamic predicate to static might provide better performance.";
    String query =
        "SELECT\n"
        + "*\n"
        + "FROM\n"
        + " comments c\n"
        + "JOIN\n"
        + " users u ON c.user_id = u.id\n"
        + "WHERE\n"
        + " u.id in\n"
        + "   (\n"
        + "     SELECT\n"
        + "       id \n"
        + "     FROM\n"
        + "      users\n"
        + "     WHERE\n"
        + "       location LIKE '%New York'\n"
        + "     GROUP BY\n"
        + "       id\n"
        + "     ORDER BY\n"
        + "       SUM(up_votes) desc\n"
        + "     LIMIT 10\n"
        + "   );\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyDynamicPredicate().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

}
