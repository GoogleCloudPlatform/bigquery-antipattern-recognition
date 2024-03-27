/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder;

import com.google.api.services.bigquery.model.TableReference;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedProjectScan;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedTableScan;
import java.util.ArrayList;
import java.util.List;

public class JoinScanVisitor extends ResolvedNodes.Visitor {

  private boolean isJoiningOnyTables = true;
  private List<TableReference> tablesInJoin = new ArrayList<>();

  public boolean isJoiningOnyTables() {
    return isJoiningOnyTables;
  }

  public List<TableReference> getTablesInJoin() {
    return tablesInJoin;
  }

  public void visit(ResolvedTableScan tableScan) {
    if(isJoiningOnyTables) {
      addTableToTablesInJoin(tableScan);
    }
  }

  public void visit(ResolvedProjectScan projectScan) {
    isJoiningOnyTables = false;
  }

  private void addTableToTablesInJoin(ResolvedTableScan tableScan) {
    String[] tableName = tableScan.getTable().getFullName().split("\\.");
    TableReference tableReference = new TableReference();
    tableReference.setProjectId(tableName[0]);
    tableReference.setDatasetId(tableName[1]);
    tableReference.setTableId(tableName[2]);
    tablesInJoin.add(tableReference);
  }

}
