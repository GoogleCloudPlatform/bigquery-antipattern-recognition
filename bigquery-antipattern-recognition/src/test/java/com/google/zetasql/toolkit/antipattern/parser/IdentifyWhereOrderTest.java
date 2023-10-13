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

public class IdentifyWhereOrderTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void SimpleWhereOrderTest() {
    String expected = "LIKE filter in line 8 precedes a more selective filter.";
    String query =
        "SELECT \n"
        + "  repo_name, \n"
        + "  id,\n"
        + "  ref\n"
        + "FROM \n"
        + "  `bigquery-public-data.github_repos.files` \n"
        + "WHERE\n"
        + "  ref like '%master%'\n"
        + "  and repo_name = 'cdnjs/cdnjs'\n"
        + ";\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyWhereOrder().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void CorrectOrderTest() {
    String expected = "";
    String query =
        "SELECT \n"
        + "  repo_name, \n"
        + "  id,\n"
        + "  ref\n"
        + "FROM \n"
        + "  `bigquery-public-data.github_repos.files` \n"
        + "WHERE\n"
        + "  repo_name = 'cdnjs/cdnjs'\n"
        + "  and ref like '%master%'\n"
        + ";\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyWhereOrder().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void SubQueryWhereOrderTest() {
    String expected = "LIKE filter in line 11 precedes a more selective filter.\n"
        + "LIKE filter in line 15 precedes a more selective filter.";
    String query =
        "SELECT\n"
        + "   *\n"
        + "FROM\n"
        + "   (SELECT \n"
        + "     repo_name, \n"
        + "     id,\n"
        + "     ref\n"
        + "   FROM \n"
        + "     `bigquery-public-data.github_repos.files` \n"
        + "   WHERE\n"
        + "     ref like '%master%'\n"
        + "     AND repo_name = 'cdnjs/cdnjs'\n"
        + "   )\n"
        + "WHERE\n"
        + "   col1 like '%asdf%'\n"
        + "   AND col2 = 'udifj'\n"
        + ";\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyWhereOrder().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void WhereOrderSubqueryInWhereTest() {
    String expected = "LIKE filter in line 11 precedes a more selective filter.";
    String query =
        "SELECT\n"
        + "  col1\n"
        + "FROM \n"
        + "  table1\n"
        + "WHERE\n"
        + "  col1 = 1\n"
        + "  AND col2 in (\n"
        + "    SELECT id\n"
        + "    FROM `bigquery-public-data.github_repos.files` \n"
        + "    WHERE\n"
        + "      ref like '%master%'\n"
        + "      and repo_name = 'cdnjs/cdnjs'\n"
        + "  )\n"
        + "  AND col3 > 100\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyWhereOrder().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void WhereOrderSubqueryInWhereWithLikeInFistLevelTest() {
    String expected = "LIKE filter in line 6 precedes a more selective filter.\n"
        + "LIKE filter in line 11 precedes a more selective filter.";
    String query =
        "SELECT\n"
        + "  col1\n"
        + "FROM \n"
        + "  table1\n"
        + "WHERE\n"
        + "  col1 like '%vhbfjdn%'\n"
        + "  AND col2 in (\n"
        + "    SELECT id\n"
        + "    FROM `bigquery-public-data.github_repos.files` \n"
        + "    WHERE\n"
        + "      ref like '%master%'\n"
        + "      and repo_name = 'cdnjs/cdnjs'\n"
        + "  )\n"
        + "  AND col3 = 100\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyWhereOrder().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void WhereOrderSubqueryInWhereCorrectOrderTest() {
    String expected = "";
    String query =
        "SELECT\n"
            + "  col1\n"
            + "FROM \n"
            + "  table1\n"
            + "WHERE\n"
            + "  col1 like '%vhbfjdn%'\n"
            + "  AND col2 in (\n"
            + "    SELECT id\n"
            + "    FROM `bigquery-public-data.github_repos.files` \n"
            + "    WHERE\n"
            + "      repo_name = 'cdnjs/cdnjs'\n"
            + "  )\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyWhereOrder().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }
}
