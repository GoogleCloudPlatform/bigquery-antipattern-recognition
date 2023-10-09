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
import com.google.zetasql.toolkit.antipattern.parser.IdentifyDynamicPredicate;
import com.google.zetasql.toolkit.antipattern.parser.IdentifyLatestRecord;
import com.google.zetasql.toolkit.antipattern.parser.IdentifySimpleSelectStar;

public class PrintParserDebugString {

  public static void main(String[] args) {
    LanguageOptions languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();

    String query = "SELECT\n"
        + "    col1, col2 \n"
        + "FROM \n"
        + "    table1 \n"
        + "WHERE \n"
        + "    col3 in (select col3 from table2)";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    // System.out.println(parsedQuery);
    // System.out.println(new IdentifyLatestRecord().run(parsedQuery, query));
    System.out.println(new IdentifyDynamicPredicate().run(parsedQuery, query));
    // System.out.println(new IdentifyInSubqueryWithoutAgg().run(parsedQuery));
    // System.out.println(new IdentifyCrossJoin().run(parsedQuery));

  }
}
