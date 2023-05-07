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

package com.google.zetasql.toolkit.antipattern.analyzer.visitors;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.*;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.*;

public class SelectedColumnsVisitor extends ResolvedNodes.Visitor {

  private final Map<String, TableWithSelectedCol> tablesWithSelectedColsMap =
      new HashMap<String, TableWithSelectedCol>();

  public List<TableWithSelectedCol> getResult() {
    return new ArrayList<TableWithSelectedCol>(this.tablesWithSelectedColsMap.values());
  }

  public void visit(ResolvedComputedColumn computedCol) {
    addColumnToMap(computedCol);
    super.visit(computedCol);
  }

  private void addColumnToMap(ResolvedComputedColumn computedCol) {
    String tableName = ZetaSQLStringParsingHelper.getTableNameFromExpr(computedCol.toString());
    if (tableName != null) {
      String colName = computedCol.getColumn().getName();
      if (tablesWithSelectedColsMap.containsKey(tableName)) {
        tablesWithSelectedColsMap.get(tableName).addSelectedColumn(colName);
      } else {
        TableWithSelectedCol tableWithSelectedCol = new TableWithSelectedCol(tableName);
        tableWithSelectedCol.addSelectedColumn(colName);
        tablesWithSelectedColsMap.put(tableName, tableWithSelectedCol);
      }
    }
  }

  public static class TableWithSelectedCol {

    private final String table;
    private final Set<String> selectedColumns = new HashSet<>();

    public TableWithSelectedCol(String table) {
      this.table = table;
    }

    public String getTable() {
      return table;
    }

    public Set<String> getSelectedColumns() {
      return selectedColumns;
    }

    public void addSelectedColumn(String column) {
      this.selectedColumns.add(column);
    }

    public Map<String, Object> toMap() {
      return Map.of(
          "table", this.table,
          "selectedColumns", this.selectedColumns);
    }
  }
}
