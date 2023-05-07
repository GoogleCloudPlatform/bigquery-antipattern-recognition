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

package com.google.zetasql.toolkit.antipattern.parser.visitors.crossjoin;

import java.util.ArrayList;
import java.util.List;

public class CrossJoin {

  private CrossJoinSide left;
  private CrossJoinSide right;
  private List<String> namesTablesUsedOnFilter = new ArrayList<>();

  CrossJoin(CrossJoinSide left, CrossJoinSide right) {
    this.left = left;
    this.right = right;
  }

  public List<String> getNamesTablesUsedOnFilter() {
    return namesTablesUsedOnFilter;
  }

  public CrossJoinSide getLeft() {
    return left;
  }

  public CrossJoinSide getRight() {
    return right;
  }

  public void addTableName(String tableName) {
    namesTablesUsedOnFilter.add(tableName);
  }
}
