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
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.subqueryinwhere.SubqueryInWhereVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;

public class IdentifySubqueryInWhere implements BasePatternDetector {

  private final String SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE =
      "You are using an IN filter with a subquery without a DISTINCT on the following columns: ";
  ArrayList<String> result = new ArrayList<String>();

  public String run(String query, BigQueryCatalog catalog, ZetaSQLToolkitAnalyzer analyzer) {

    SubqueryInWhereVisitor visitor = new SubqueryInWhereVisitor();

    Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);

    statementIterator.forEachRemaining(statement -> statement.accept(visitor));

    visitor.getResult().forEach(subqueryInWhereColumns -> parseResults(subqueryInWhereColumns));

    String retValue = StringUtils.join(result, "\n");
    result.clear();
    return retValue;
  }

  public void parseResults(String subqueryInWhereColumns) {
    result.add(SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE + subqueryInWhereColumns);
  }
}
