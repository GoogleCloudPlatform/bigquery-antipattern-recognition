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

public class IdentifyLatestRecordTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void RowNumberForLatestRecordTest() {
    String expected = "Seems like you might be using analytical function row_number in line 7 to filter the latest record in line 12.";
    String query = "SELECT \n"
        + "  taxi_id, trip_seconds, fare\n"
        + "FROM\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ")\n"
        + "WHERE \n"
        + "  rn = 1\n"
        + "ORDER BY\n"
        + "  taxi_id;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyLatestRecord().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void RowNumberNoFilterTest() {
    String expected = "";
    String query = "SELECT \n"
        + "  taxi_id, trip_seconds, fare\n"
        + "FROM\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ")\n"
        + "WHERE \n"
        + "  taxi_id = 1\n"
        + "ORDER BY\n"
        + "  taxi_id;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyLatestRecord().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void RowNumberForLatestRecordWithOneJoinTest() {
    String expected = "Seems like you might be using analytical function row_number in line 7 to filter the latest record in line 14.";
    String query = "SELECT \n"
        + "  taxi_id, trip_seconds, fare\n"
        + "FROM\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ") t1\n"
        + "JOIN\n"
        + "  table2 t2 ON t1.taxi_is = t2.col1\n"
        + "WHERE \n"
        + "  rn = 1\n"
        + "ORDER BY\n"
        + "  taxi_id;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyLatestRecord().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void RowNumberForLatestRecordWithTwoJoinsTest() {
    String expected = "Seems like you might be using analytical function row_number in line 9 to filter the latest record in line 16.";
    String query = "SELECT \n"
        + "  taxi_id, trip_seconds, fare\n"
        + "FROM\n"
        + "  table0 t0\n"
        + "JOIN\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ") t1 ON t0.col1 = t1.taxi_id\n"
        + "JOIN\n"
        + "  table2 t2 ON t1.taxi_is = t2.col1\n"
        + "WHERE \n"
        + "  rn = 1\n"
        + "ORDER BY\n"
        + "  taxi_id;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyLatestRecord().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void RowNumberForLatestRecordSeveralRowNumTest() {
    String expected = "Seems like you might be using analytical function row_number in line 7 to filter the latest record in line 23.\n"
        + "Seems like you might be using analytical function rank in line 8 to filter the latest record in line 25.\n"
        + "Seems like you might be using analytical function row_number in line 17 to filter the latest record in line 26.\n"
        + "Seems like you might be using analytical function rank in line 18 to filter the latest record in line 27.";
    String query = "SELECT \n"
        + "  taxi_id, trip_seconds, fare\n"
        + "FROM\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn,\n"
        + "    rank() over(partition by taxi_id order by fare desc) rnk\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ") t1\n"
        + "JOIN table3 t3 ON t1.taxi_id = t3.col1\n"
        + "JOIN\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn2,\n"
        + "    rank() over(partition by taxi_id order by fare desc) rnk2\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ") t2\n"
        + "WHERE \n"
        + "  rn = 1\n"
        + "  and taxi_id>0\n"
        + "  and rnk = 1\n"
        + "  and rn2 = 1\n"
        + "  and rnk2 = 1\n"
        + "ORDER BY\n"
        + "  taxi_id;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyLatestRecord().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }

  @Test
  public void RowNumberForLatestRecordSeveralWindowFunTest() {
    String expected = "Seems like you might be using analytical function row_number in line 7 to filter the latest record in line 23.\n"
        + "Seems like you might be using analytical function rank in line 18 to filter the latest record in line 25.";
    String query = "SELECT \n"
        + "  taxi_id, trip_seconds, fare\n"
        + "FROM\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn,\n"
        + "    rank() over(partition by taxi_id order by fare desc) rnk\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ") t1\n"
        + "JOIN table3 t3 ON t1.taxi_id = t3.col1\n"
        + "JOIN\n"
        + "  (\n"
        + "  SELECT \n"
        + "    taxi_id, trip_seconds, fare,\n"
        + "    row_number() over(partition by taxi_id order by fare desc) rn2,\n"
        + "    rank() over(partition by taxi_id order by fare desc) rnk2\n"
        + "  FROM \n"
        + "    `bigquery-public-data.chicago_taxi_trips.taxi_trips`\n"
        + ") t2\n"
        + "WHERE \n"
        + "  rn = 1\n"
        + "  and taxi_id>0\n"
        + "  and rnk2 = 1\n"
        + "ORDER BY\n"
        + "  taxi_id;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = new IdentifyLatestRecord().run(parsedQuery, query);
    assertEquals(expected, recommendation);
  }
}
