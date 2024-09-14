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

import org.junit.Before;
import org.junit.Test;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTScript;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifySelfJoinVisitor;

 public class IdentifySelfJoinTest {
   LanguageOptions languageOptions;

   @Before
   public void setUp() {
     languageOptions = new LanguageOptions();
     languageOptions.enableMaximumLanguageFeatures();
     languageOptions.setSupportsAllStatementKinds();
   }

   // Test with a query that creates a table and drops it without TEMP
   @Test
   public void selfJoinTest() {
     String expected = "Self Join detected: Table bigquery-public-data.chicago_taxi_trips.taxi_trips is joined on itself at line 4. Consider window (analytic) function.";
     String query = "SELECT DATE(t1.trip_start_timestamp) dt,\n"
     + "COUNT(DISTINCT t1.unique_key) ct,\n"
     + "COUNT(DISTINCT t2.unique_key) ct_prev_day\n"
     + "FROM `bigquery-public-data.chicago_taxi_trips.taxi_trips` t1\n"
     + "LEFT JOIN `bigquery-public-data.chicago_taxi_trips.taxi_trips` t2\n"
     + "ON DATE(t2.trip_start_timestamp) = DATE(t1.trip_start_timestamp) - INTERVAL 1 DAY\n"
     + "WHERE FORMAT_DATE('%Y-%m', t1.trip_start_timestamp) = '2023-02'\n"
     + "AND FORMAT_DATE('%Y-%m', t2.trip_start_timestamp) = '2023-02'\n"
     + "GROUP BY dt\n"
     + "ORDER BY dt desc;";

     ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
     IdentifySelfJoinVisitor visitor = new IdentifySelfJoinVisitor(query);
     parsedQuery.accept(visitor);
     String recommendations = visitor.getResult();
     assertEquals(expected, recommendations);
   }

  // Test with a query that creates a table and drops it without TEMP
  @Test
  public void selfJoinTempTest() {
    String expected = "Self Join detected: Table dly_trip_ct is joined on itself at line 13. Consider window (analytic) function.";
    String query = "BEGIN\n"
    + "  CREATE TEMP TABLE dly_trip_ct AS\n"
    + "  SELECT DATE(t1.trip_start_timestamp) dt,\n"
    + "         taxi_id,\n"
    + "         COUNT(DISTINCT t1.unique_key) ct\n"
    + "  FROM `bigquery-public-data.chicago_taxi_trips.taxi_trips` t1\n"
    + "  GROUP BY dt, taxi_id;\n"
    + "\n"
    + "  SELECT t1.dt,\n"
    + "         t1.taxi_id,\n"
    + "         t1.ct,\n"
    + "         t2.ct ct_prev_day\n"
    + "  FROM dly_trip_ct t1\n"
    + "  LEFT JOIN dly_trip_ct t2\n"
    + "    ON DATE(t2.dt) = DATE(t1.dt) - INTERVAL 1 DAY\n"
    + "    AND t1.taxi_id = t2.taxi_id\n"
    + "  ORDER BY dt desc, taxi_id desc;\n"
    + "END";

    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySelfJoinVisitor visitor = new IdentifySelfJoinVisitor(query);
    parsedQuery.accept(visitor);
    String recommendations = visitor.getResult();
    assertEquals(expected, recommendations);
  }



 }
