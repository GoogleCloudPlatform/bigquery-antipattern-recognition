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

import java.util.ArrayList;
import java.util.List;

public class CrossJoin {

  private CrossJoinChildNode left;
  private CrossJoinChildNode right;
  private List<String> tableNames = new ArrayList<>();

  CrossJoin(CrossJoinChildNode left, CrossJoinChildNode right) {
    this.left = left;
    this.right = right;
  }

  public CrossJoinChildNode getLeft() {
    return left;
  }

  public CrossJoinChildNode getRight() {
    return right;
  }

  public List<String> getTableNames() {
    return tableNames;
  }

  public void addTableName(String tableName) {
    tableNames.add(tableName);
  }
}
