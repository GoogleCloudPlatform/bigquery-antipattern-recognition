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
import com.google.zetasql.toolkit.antipattern.parser.IdentifyLatestRecord;
import com.google.zetasql.toolkit.antipattern.parser.IdentifySimpleSelectStar;

public class PrintParserDebugString {

  public static void main(String[] args) {
    LanguageOptions languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();

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
    //System.out.println(parsedQuery);
    System.out.println(new IdentifyLatestRecord().run(parsedQuery, query));
    // System.out.println(new IdentifyInSubqueryWithoutAgg().run(parsedQuery));
    // System.out.println(new IdentifyCrossJoin().run(parsedQuery));

  }
}
