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

  public static void writeOutput(BQAntiPatternCMDParser cmdParser, List<Object[]> outputData)
      throws IOException {
    if (cmdParser.hasOutputFileOptionName()) {
      writeOutputToCSV(outputData, cmdParser);
    } else if (cmdParser.isReadingFromInfoSchema() && cmdParser.hasOutputTable()) {
      writeOutputToBQTable(outputData, cmdParser);
    } else {
      for (Object[] row : outputData) {
        System.out.println("----------------------------------------");
        System.out.println(String.join("\n", (String[]) row));
      }
    }
  }

  private static void writeOutputToCSV(List<Object[]> outputData, BQAntiPatternCMDParser cmdParser)
      throws IOException {

    FileWriter csvWriter;
    File file = new File(cmdParser.getOutputFileOptionName());
    if (file.exists()) {
      csvWriter = new FileWriter(file, true);
    } else {
      csvWriter = new FileWriter(file);
      csvWriter.write(String.join(",", new String[] {"id", "query\n"}));
    }

    for (Object[] row : outputData) {
      csvWriter.write(String.join(",", (String[]) row));
    }
    csvWriter.close();
  }

  private static void writeOutputToBQTable(
      List<Object[]> outputData, BQAntiPatternCMDParser cmdParser) {
    DateTime date = new DateTime(new Date());
    for (Object[] row : outputData) {
      Map<String, Object> rowContent = new HashMap<>();
      rowContent.put("job_id", row[0]);
      rowContent.put("query", row[1]);
      rowContent.put("slot_hours", row[2]);
      rowContent.put("recommendation", row[3]);
      rowContent.put("process_timestamp", date);
      BigQueryHelper.writeResults(
          cmdParser.getProcessingProject(), cmdParser.getOutputTable(), rowContent);
    }
  }
}