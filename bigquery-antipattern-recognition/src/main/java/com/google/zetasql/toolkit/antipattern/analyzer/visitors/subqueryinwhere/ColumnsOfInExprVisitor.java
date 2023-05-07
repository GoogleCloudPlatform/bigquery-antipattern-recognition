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

package com.google.zetasql.toolkit.antipattern.analyzer.visitors.subqueryinwhere;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedColumnRef;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ColumnsOfInExprVisitor extends ResolvedNodes.Visitor {
  private List<String> columnList = new ArrayList<>();

  public void visit(ResolvedColumnRef resolvedColumnRef) {
    columnList.add(resolvedColumnRef.getColumn().toString().split("#")[0]);
  }

  public String getResult() {
    return columnList.stream().map(Object::toString).collect(Collectors.joining(", "));
  }
}
