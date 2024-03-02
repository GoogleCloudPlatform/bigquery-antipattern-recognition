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
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;
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
    String expected = "SubOptimal order of predicates in WHERE, line 7. Consider applying more "
        + "restrictive filters first. For example a '=' or a '>' filter usually is usually more "
        + "restrictive than a like '%' filter. The following order might provide performance "
        + "benefits is '=', '>', '<', '<>', 'like'";
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
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
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
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void SubQueryWhereOrderTest() {
    String expected = "SubOptimal order of predicates in WHERE, line 10. Consider applying more "
        + "restrictive filters first. For example a '=' or a '>' filter usually is usually more "
        + "restrictive than a like '%' filter. The following order might provide performance "
        + "benefits is '=', '>', '<', '<>', 'like'\n"
        + "SubOptimal order of predicates in WHERE, line 14. Consider applying more restrictive "
        + "filters first. For example a '=' or a '>' filter usually is usually more restrictive "
        + "than a like '%' filter. The following order might provide performance benefits is "
        + "'=', '>', '<', '<>', 'like'";
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
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void WhereOrderSubqueryInWhereTest() {
    String expected = "SubOptimal order of predicates in WHERE, line 10. Consider applying more "
        + "restrictive filters first. For example a '=' or a '>' filter usually is usually more "
        + "restrictive than a like '%' filter. The following order might provide performance "
        + "benefits is '=', '>', '<', '<>', 'like'";
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
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void WhereOrderSubqueryInWhereWithLikeInFistLevelTest() {
    String expected = "SubOptimal order of predicates in WHERE, line 5. Consider applying more "
        + "restrictive filters first. For example a '=' or a '>' filter usually is usually more "
        + "restrictive than a like '%' filter. The following order might provide performance "
        + "benefits is '=', '>', '<', '<>', 'like'\n"
        + "SubOptimal order of predicates in WHERE, line 10. Consider applying more restrictive "
        + "filters first. For example a '=' or a '>' filter usually is usually more restrictive "
        + "than a like '%' filter. The following order might provide performance benefits is "
        + "'=', '>', '<', '<>', 'like'";
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
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
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
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void WhereOrderSubqueryInWhereCorrectOrderAllTypesTest() {
    String expected = "";
    String query =
        "SELECT col1 FROM tbl1 WHERE \n"
            + "col1=1 \n"
            + "AND col2>1 \n"
            + "AND col3<1 \n"
            + "AND col6 >= 1\n"
            + "AND col7 <= 1\n"
            + "AND col5 <> 1\n"
            + "AND col5 != 1\n"
            + "AND col4 like '%a%' \n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    IdentifyWhereOrderVisitor visitor = new IdentifyWhereOrderVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }
}
