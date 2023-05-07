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

public class IdentifySimpleSelectStarTest {
  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void simpleSelectStarTest() {
    String expected =
        "SELECT * on table: project.dataset.table1. Check that all columns are needed.";
    String query = "SELECT * FROM `project.dataset.table1`";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifySimpleSelectStar()).run(parsedQuery);
    assertEquals(expected, recommendations);
  }

  @Test
  public void simpleSelectStarWithLimitTest() {
    String expected =
        "SELECT * on table: project.dataset.table1. Check that all columns are needed.";
    String query = "SELECT * FROM `project.dataset.table1` LIMIT 1000";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifySimpleSelectStar()).run(parsedQuery);
    assertEquals(expected, recommendations);
  }

  @Test
  public void selectStarWithGroupByTest() {
    String expected = "";
    String query = "SELECT * FROM `project.dataset.table1` GROUP BY col1";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifySimpleSelectStar()).run(parsedQuery);
    assertEquals(expected, recommendations);
  }

  @Test
  public void selectStarWithJoinTest() {
    String expected = "";
    String query = "SELECT * FROM table1, table2";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifySimpleSelectStar()).run(parsedQuery);
    assertEquals(expected, recommendations);
  }
}
