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

package com.google.zetasql.toolkit.antipattern.util;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;

public class PrintParserDebugString {

  public static void main(String[] args) {
    LanguageOptions languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();

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

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    System.out.println(parsedQuery);

  }
}
