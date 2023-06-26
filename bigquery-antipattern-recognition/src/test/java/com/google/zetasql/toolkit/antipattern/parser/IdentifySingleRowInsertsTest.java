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
import com.google.zetasql.toolkit.antipattern.Recommendation;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class IdentifySingleRowInsertsTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void insertSingleRow() {
    String expected = "SINGLE ROW INSERTS";
    String query = "INSERT dataset.Inventory (product, quantity) VALUES('top load washer', 10)";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);

    Optional<Recommendation> maybeRecommendation =
        (new IdentifySingleRowInserts()).run(parsedQuery, query);
    assertTrue(maybeRecommendation.isPresent());
    assertEquals(expected, maybeRecommendation.get().getDescription());
  }

  @Test
  public void insertMultiRow() {
    String query =
        "INSERT dataset.Inventory (product, quantity) VALUES('top load washer', 10), ('abc', 20)";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);

    Optional<Recommendation> maybeRecommendation =
        (new IdentifySingleRowInserts()).run(parsedQuery, query);
    assertTrue(maybeRecommendation.isEmpty());
  }

  @Test
  public void insertSingleRowSelect() {
    String expected = "SINGLE ROW INSERTS";
    String query =
        "INSERT dataset.DetailedInventory (product, quantity) VALUES('countertop microwave',"
            + " (SELECT quantity FROM dataset.DetailedInventory WHERE product = 'microwave'))";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);

    Optional<Recommendation> maybeRecommendation =
        (new IdentifySingleRowInserts()).run(parsedQuery, query);
    assertTrue(maybeRecommendation.isPresent());
    assertEquals(expected, maybeRecommendation.get().getDescription());
  }

  @Test
  public void insertSelectUnnest() {
    String query =
        "INSERT dataset.Warehouse (warehouse, state) SELECT * FROM UNNEST([('warehouse #1', 'WA'),"
            + " ('warehouse #2', 'CA'),  ('warehouse #3', 'WA')])";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);

    Optional<Recommendation> maybeRecommendation =
        (new IdentifySingleRowInserts()).run(parsedQuery, query);
    assertTrue(maybeRecommendation.isEmpty());
  }

  @Test
  public void insertCte() {
    String query =
        "INSERT dataset.Warehouse (warehouse, state) WITH w AS (SELECT ARRAY<STRUCT<warehouse"
            + " string, state string>> [('warehouse #1', 'WA'), ('warehouse #2', 'CA'),('warehouse"
            + " #3', 'WA')] col) SELECT warehouse, state FROM w, UNNEST(w.col)";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);

    Optional<Recommendation> maybeRecommendation =
        (new IdentifySingleRowInserts()).run(parsedQuery, query);
    assertTrue(maybeRecommendation.isEmpty());
  }

  @Test
  public void insertSelectFrom() {
    String query =
        "INSERT dataset.DetailedInventory (product, quantity, supply_constrained) SELECT"
            + " product,quantity, false FROM dataset.Inventory ";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);

    Optional<Recommendation> maybeRecommendation =
        (new IdentifySingleRowInserts()).run(parsedQuery, query);
    assertTrue(maybeRecommendation.isEmpty());
  }
}
