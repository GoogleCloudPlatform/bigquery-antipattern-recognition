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

package com.google.zetasql.toolkit.antipattern.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;

import java.util.List;
import java.util.stream.Collectors;

public class OutputCSVWriterHelper {

  private static final String CSV_HEADER = "id,recommendation";
  private static final String CSV_HEADER_OPTIMIZED_SQL_COL_NAME = "optimized_sql";
  private static final String REC_FORMAT = "%s: %s";
  private static final String BASE_OUTPUT_RECORD_FORMAT = "%s,\"%s\"";

  public static String getOutputStringForRecord(
      InputQuery inputQuery,
      List<AntiPatternVisitor> visitorsThatFoundPatterns,
      AntiPatternCommandParser cmdParser) {
    String rec =
        visitorsThatFoundPatterns.stream()
            .map(visitor -> String.format(REC_FORMAT, visitor.getName(), visitor.getResult()))
            .collect(Collectors.joining("\n"));

    return buildRecord(inputQuery, cmdParser, rec);
  }

  public static String getHeader(AntiPatternCommandParser cmdParser) {
    if (cmdParser.rewriteSQL()) {
      return CSV_HEADER + "," + CSV_HEADER_OPTIMIZED_SQL_COL_NAME + "\n";
    } else {
      return CSV_HEADER + "\n";
    }
  }

  public static String getOutputRecFormat(AntiPatternCommandParser cmdParser) {
    if (cmdParser.rewriteSQL()) {
      return BASE_OUTPUT_RECORD_FORMAT + ",\"%s\"\n";
    } else {
      return BASE_OUTPUT_RECORD_FORMAT + "\n";
    }
  }

  public static String buildRecord(
      InputQuery inputQuery, AntiPatternCommandParser cmdParser, String rec) {
    String output_record_format = getOutputRecFormat(cmdParser);
    if (cmdParser.rewriteSQL()) {
      return String.format(
          output_record_format, inputQuery.getQueryId(), rec, inputQuery.getOptimizedQuery());
    } else {
      return String.format(output_record_format, inputQuery.getQueryId(), rec);
    }
  }
}
