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

package com.google.zetasql.toolkit.antipattern.cmd;

import com.google.api.client.util.DateTime;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputGenerator {
  
  public static void writeOutput(BQAntiPatternCMDParser cmdParser, List<OutputResult> outputData)
      throws IOException {
    if (cmdParser.hasOutputFileOptionName()) {
      writeOutputToCSV(outputData, cmdParser);
    } else if (cmdParser.isReadingFromInfoSchema() && cmdParser.hasOutputTable()) {
        writeOutputToBQTable(outputData, cmdParser);
    } else {
      for (OutputResult result : outputData) {
        for (RecommendOutput RecOut : result.getListRecommend()) {
          System.out.println("--------------------------------------");
          System.out.println(String.join(";", new String[] {result.getJobId(),result.getQuery(),String.valueOf(result.getSlotHours()),RecOut.getName(),RecOut.getDescription()}));
        }
      }
  }
  }

  private static void writeOutputToCSV(List<OutputResult> outputData, BQAntiPatternCMDParser cmdParser)
      throws IOException {

    FileWriter csvWriter;
    File file = new File(cmdParser.getOutputFileOptionName());
    if (file.exists()) {
      csvWriter = new FileWriter(file, true);
    } else {
      csvWriter = new FileWriter(file);
      csvWriter.write(String.join(";", new String[] {"id", "query", "slotHours", "recommend_name", "recommend_description\n"}));
    }

    for (OutputResult result : outputData) {
        try {
              for (RecommendOutput RecOut : result.getListRecommend()) {
                  csvWriter.write(String.join(";", new String[] {result.getJobId(),result.getQuery(),String.valueOf(result.getSlotHours()),RecOut.getName(),RecOut.getDescription()}));
              }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
    }
    csvWriter.close();
  }

  private static void writeOutputToBQTable(
      List<OutputResult> outputData, BQAntiPatternCMDParser cmdParser) {
    DateTime date = new DateTime(new Date());
    for (OutputResult result : outputData) {
      for (RecommendOutput RecOut : result.getListRecommend()) {
        Map<String, Object> rowContent = new HashMap<>();
        rowContent.put("job_id", result.getJobId());
        rowContent.put("query", result.getQuery());
        rowContent.put("slot_hours", result.getSlotHours());
        rowContent.put("recommendation_name", RecOut.getName());
        rowContent.put("recommendation_description", RecOut.getDescription());
        rowContent.put("process_timestamp", date.toString());
        BigQueryHelper.writeResults(cmdParser.getProcessingProject(), cmdParser.getOutputTable(), rowContent);
      }
    }
  }
}