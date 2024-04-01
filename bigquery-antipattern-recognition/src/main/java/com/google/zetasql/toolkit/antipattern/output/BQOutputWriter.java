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

import com.google.api.client.util.DateTime;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQOutputWriter extends OutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(BQOutputWriter.class);
  public static final String JOB_IDENTIFIER_COL_NAME = "job_id";
  public static final String QUERY_COL_NAME = "query";
  public static final String SLOT_HOURS_COL_NAME = "slot_hours";
  public static final String USER_EMAIL_COL_NAME = "user_email";
  public static final String RECOMMENDATION_COL_NAME = "recommendation";
  public static final String PROCESS_TIMESTAMP_COL_NAME = "process_timestamp";
  public static final String REC_NAME_COL_NAME = "name";
  public static final String DESCRIPTION_COL_NAME = "description";
  public static final String OPTIMIZED_SQL_COL_NAME = "optimized_sql";
  private String tableName;
  private String processingProjectName;
  private DateTime date;

  public BQOutputWriter(String outputDir) {
    tableName = outputDir;
    date = new DateTime(new Date());
  }

  public void setProcessingProjectName(String processingProjectName) {
    this.processingProjectName = processingProjectName;
  }

  public void writeRecForQuery(
      InputQuery inputQuery,
      List<AntiPatternVisitor> visitorsThatFoundPatterns,
      AntiPatternCommandParser cmdParser) {

    List<Map<String, String>> rec_list = new ArrayList<>();
    for (AntiPatternVisitor visitor : visitorsThatFoundPatterns) {
      Map<String, String> rec = new HashMap<>();
      rec.put(REC_NAME_COL_NAME, "ree");
      rec.put(DESCRIPTION_COL_NAME, visitor.getResult());
      rec_list.add(rec);
    }

    Map<String, Object> rowContent = new HashMap<>();
    rowContent.put(JOB_IDENTIFIER_COL_NAME, inputQuery.getQueryId());
    rowContent.put(QUERY_COL_NAME, inputQuery.getQuery());
    rowContent.put(
        SLOT_HOURS_COL_NAME, inputQuery.getSlotHours() >= 0 ? inputQuery.getSlotHours() : null);
    rowContent.put(USER_EMAIL_COL_NAME, inputQuery.getUserEmail());
    rowContent.put(RECOMMENDATION_COL_NAME, rec_list);
    rowContent.put(OPTIMIZED_SQL_COL_NAME, inputQuery.getOptimizedQuery());
    rowContent.put(PROCESS_TIMESTAMP_COL_NAME, date);
    BigQueryHelper.writeResults(processingProjectName, tableName, rowContent);
  }
}
