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

package com.google.zetasql.toolkit.antipattern.util;

public class ZetaSQLHelperConstants {
  public static String INFO_SCHEMA_SCHEMATA_QUERY =
      "SELECT \n" + "  schema_name\n" + "FROM\n" + "  `%s`.INFORMATION_SCHEMA.SCHEMATA\n";

  public static String INFO_SCHEMA_COLUMNS_QUERY =
      "SELECT \n"
          + "  table_name, column_name, data_type\n"
          + "FROM \n"
          + "  %s.%s.INFORMATION_SCHEMA.COLUMNS\n"
          + "ORDER BY\n"
          + "  table_schema, table_name";

  public static String TABLE_NAME = "project.dataset.table1";
  public static String TABLE_1_NAME = "project.dataset.table1";
  public static String TABLE_2_NAME = "project.dataset.table2";
  public static String TABLE_3_NAME = "project.dataset.table3";
  public static String COL_1 = "col1";
  public static String COL_2 = "col2";
  public static String CATALOG_NAME = "catalog";
  public static String MY_PROJET = "my-project";
}
