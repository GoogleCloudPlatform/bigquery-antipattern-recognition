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

public class IdentifyNtileWindowFunctionTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  // Test with a query that uses one NTILE function
  @Test
  public void withOneNtileFunctionTest() {
    String expected = "Use of NTILE window function detected at line 4. Prefer APPROX_QUANTILE if approximate bucketing is sufficient.";

    String query = "SELECT taxi_id,\n"
        + " fare,\n"
        + " payment_type,\n"
        + " NTILE(4) OVER (PARTITION BY payment_type ORDER BY fare ASC) AS fare_rank\n"
        + " FROM `taxi_trips` trips\n"
        + " where EXTRACT(YEAR FROM trips.trip_start_timestamp AT TIME ZONE \"UTC\") = 2013;";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyNtileWindowFunction()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  // Test with a query that uses any other analytic function instead of NTILE
  @Test
  public void withOtherAnalyticFunctionTest() {
    String expected = "";

    String query = "SELECT taxi_id,\n"
        + "fare,\n"
        + "payment_type,\n"
        + "DENSE_RANK() OVER (PARTITION BY payment_type ORDER BY fare ASC) AS fare_rank\n"
        + "FROM `taxi_trips`";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyNtileWindowFunction()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  // Test with a query that uses the NTILE function in the subquery
  @Test
  public void withNtileFunctionInSubQueryTest() {
    String expected = "Use of NTILE window function detected at line 7. Prefer APPROX_QUANTILE if approximate bucketing is sufficient.";

    String query = "SELECT taxi_id,\n"
        + " fare_rank,\n"
        + " from\n"
        + " (SELECT taxi_id,\n"
        + " fare,\n"
        + " payment_type,\n"
        + " NTILE(4) OVER (PARTITION BY payment_type ORDER BY fare ASC) AS fare_rank\n"
        + " FROM `taxi_trips`\n"
        + " ) t;";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyNtileWindowFunction()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  @Test
  public void withTwoWindowFunctionsTest() {
    String expected = "Use of NTILE window function detected at line 4. Prefer APPROX_QUANTILE if approximate bucketing is sufficient.";

    String query = "SELECT taxi_id,\n"
        + " fare,\n"
        + " payment_type,\n"
        + " NTILE(4) OVER (PARTITION BY payment_type ORDER BY fare ASC) AS fare_rank,\n"
        + " DENSE_RANK() OVER (PARTITION BY payment_type ORDER BY fare ASC) AS rank_dense\n"
        + " FROM `taxi_trips` trips\n"
        + " where EXTRACT(YEAR FROM trips.trip_start_timestamp AT TIME ZONE \"UTC\") = 2013;";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyNtileWindowFunction()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

}
