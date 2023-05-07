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

package com.google.zetasql.toolkit.antipattern.analyzer.visitors.crossjoin;

import com.google.zetasql.resolvedast.ResolvedNodes.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CrossJoinChildNode {
  private List<String> tableNameList = new ArrayList<>();

  CrossJoinChildNode(ResolvedScan resolvedScan) {
    setup(resolvedScan);
  }

  private void setup(ResolvedScan resolvedScan) {
    if (resolvedScan instanceof ResolvedTableScan) {
      tableNameList.add(((ResolvedTableScan) resolvedScan).getTable().getFullName());
    } else if (resolvedScan instanceof ResolvedJoinScan) {
      ResolvedJoinScan resolvedJoinScan = (ResolvedJoinScan) resolvedScan;
      tableNameList =
          resolvedJoinScan.getColumnList().stream()
              .map(resolvedColumn -> resolvedColumn.getTableName())
              .collect(Collectors.toList());
    } else if (resolvedScan instanceof ResolvedProjectScan) {
      ResolvedProjectScan resolvedProjectScan = (ResolvedProjectScan) resolvedScan;
      tableNameList.add(resolvedProjectScan.getColumnList().get(0).getTableName());
    }
  }

  public List<String> getTableNameList() {
    return tableNameList;
  }
}
