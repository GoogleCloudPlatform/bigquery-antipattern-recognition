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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogOutputWriter extends OutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(LogOutputWriter.class);

  public void writeRecForQuery(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns,
                               AntiPatternCommandParser cmdParser) {
    StringBuilder outputStrBuilder = new StringBuilder();

    outputStrBuilder.append("\n"+"-".repeat(50));
    outputStrBuilder.append("\nRecommendations for query: "+ inputQuery.getQueryId());
    for(AntiPatternVisitor visitor: visitorsThatFoundPatterns) {
      outputStrBuilder.append("\n* "+ visitor.getNAME() + ": " + visitor.getResult());
    }
    if(cmdParser.rewriteSQL() && inputQuery.getOptimizedQuery() != null) {
      outputStrBuilder.append("\n* Optimized query:\n");
      outputStrBuilder.append(inputQuery.getOptimizedQuery());
    }
    outputStrBuilder.append("\n"+"-".repeat(50));
    outputStrBuilder.append("\n\n");
    logger.info(outputStrBuilder.toString());
  }

}
