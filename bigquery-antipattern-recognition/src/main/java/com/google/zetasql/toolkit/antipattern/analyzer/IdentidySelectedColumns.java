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

import com.google.zetasql.NotFoundException;
import com.google.zetasql.Table;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.SelectedColumnsVisitor;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.SelectedColumnsVisitor.TableWithSelectedCol;
import com.google.zetasql.toolkit.antipattern.exceptions.TableNotFoundInCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class IdentidySelectedColumns implements BasePatternDetector {

  private ArrayList<String> result = new ArrayList<String>();
  private static final String SELECTED_COLUMNS_MESSAGE =
      "All columns on table: %s are being selected. Please be sure that all columns are needed";

  public String run(String query, BigQueryCatalog catalog, ZetaSQLToolkitAnalyzer analyzer) {
    SelectedColumnsVisitor visitor = new SelectedColumnsVisitor();

    Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);
    statementIterator.forEachRemaining(statement -> statement.accept(visitor));

    List<TableWithSelectedCol> visitorResults = visitor.getResult();

    visitorResults.stream()
        .forEach(
            tableWithSelectedCol -> {
              try {
                checkTable(tableWithSelectedCol, catalog, result);
              } catch (NotFoundException e) {
                throw new TableNotFoundInCatalog(e.getMessage());
              }
            });

    return StringUtils.join(result, "\n");
  }

  static void checkTable(
      SelectedColumnsVisitor.TableWithSelectedCol tableWithSelectedCol,
      BigQueryCatalog catalog,
      ArrayList<String> result)
      throws NotFoundException {
    String tableName = tableWithSelectedCol.getTable();
    Table tableInCatalog = catalog.getZetaSQLCatalog().findTable(Arrays.asList(tableName));

    if (tableInCatalog.getColumnCount() == tableWithSelectedCol.getSelectedColumns().size()) {
      result.add(String.format(SELECTED_COLUMNS_MESSAGE, tableName));
    }
  }
}
