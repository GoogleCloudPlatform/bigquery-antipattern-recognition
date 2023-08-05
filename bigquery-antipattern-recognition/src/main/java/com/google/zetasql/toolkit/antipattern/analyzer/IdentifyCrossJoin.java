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

package com.google.zetasql.toolkit.antipattern.analyzer;

import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.crossjoin.CrossJoin;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.crossjoin.CrossJoinVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class IdentifyCrossJoin implements BasePatternDetector {

  private final String CROSS_JOIN_SUGGESTION_MESSAGE =
      "CROSS JOIN between tables: %s and %s. Try to change for a INNER JOIN if possible.";
  ArrayList<String> result = new ArrayList<String>();

  public String run(String query, BigQueryCatalog catalog, ZetaSQLToolkitAnalyzer analyzer) {

    CrossJoinVisitor visitor = new CrossJoinVisitor();

    Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);
    statementIterator.forEachRemaining(statement -> statement.accept(visitor));

    List<CrossJoin> crossJoinList = visitor.getResult();
    crossJoinList.forEach(crossJoin -> parseCrossJoin(crossJoin));

    String retValue = StringUtils.join(result, "\n");
    result.clear();
    return retValue;
  }

  public void parseCrossJoin(CrossJoin crossJoin) {
    result.add(
        String.format(
            CROSS_JOIN_SUGGESTION_MESSAGE, crossJoin.getTableNames().toArray(new String[0])));
  }
}
