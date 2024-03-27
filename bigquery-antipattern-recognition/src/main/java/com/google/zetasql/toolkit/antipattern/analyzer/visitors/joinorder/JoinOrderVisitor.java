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

package com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder;

import com.google.api.services.bigquery.model.TableReference;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedJoinScan;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedProjectScan;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class JoinOrderVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

  public final static String NAME = "JoinOrder";
  private final String RECOMMENDATION_MESSAGE = "JOIN on tables: [%s] might perform better if tables where joined in the following order: [%s]";
  private BigQueryService service;
  private String result = "";

  public JoinOrderVisitor(BigQueryService service) {
    this.service = service;
  }

  public String getResult() {
    return result;
  }

  public String getNAME() {
    return NAME;
  }

  public void visit(ResolvedJoinScan joinScan) {
    JoinScanVisitor joinScanVisitor = new JoinScanVisitor();
    joinScan.accept(joinScanVisitor);
    if (joinScanVisitor.isJoiningOnyTables()) {
      List<TableReference> tablesInJoinWithOptimalOrder = getOptimalOrder(
          joinScanVisitor.getTablesInJoin());
      checkForAntiPatter(joinScanVisitor.getTablesInJoin(), tablesInJoinWithOptimalOrder);
    } else {
      super.visit(joinScan);
    }
  }

  private void checkForAntiPatter(List<TableReference> tablesInJoin,
      List<TableReference> tablesInJoinWithOptimalOrder) {
    if (!tablesInJoin.equals(tablesInJoinWithOptimalOrder)) {
      String tablesOriginalOrder = String.join(", ",
          tablesInJoin.stream().map(x -> x.getTableId()).collect(Collectors.toList()));
      String tablesOptimalOrder = String.join(", ",
          tablesInJoinWithOptimalOrder.stream().map(x -> x.getTableId()).collect(Collectors.toList()));
      if (result.length() > 0) {
        result += '\n';
      }
      result += String.format(RECOMMENDATION_MESSAGE, tablesOriginalOrder, tablesOptimalOrder);
    }
  }


  private void sortTablesByNumRows(List<TableReference> tablesInJoin) {
    Comparator<TableReference> comparator = (table1, table2) -> {
      BigInteger value1 = this.service.fetchTable(table1.getProjectId(),
          table1.getDatasetId() + "." + table1.getTableId()).get().getNumRows();
      BigInteger value2 = this.service.fetchTable(table2.getProjectId(),
          table2.getDatasetId() + "." + table2.getTableId()).get().getNumRows();
      return value1.compareTo(value2);
    };
    Collections.sort(tablesInJoin, comparator);
    Collections.reverse(tablesInJoin);
  }

  private List<TableReference> getOptimalOrder(List<TableReference> tablesInJoin) {
    List<TableReference> tablesInJoinWithOptimalOrder = new ArrayList<>(tablesInJoin);
    sortTablesByNumRows(tablesInJoinWithOptimalOrder);
    return tablesInJoinWithOptimalOrder;
  }

}
