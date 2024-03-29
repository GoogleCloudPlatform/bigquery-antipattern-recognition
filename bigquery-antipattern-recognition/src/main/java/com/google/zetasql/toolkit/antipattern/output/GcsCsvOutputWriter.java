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
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsCsvOutputWriter extends OutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(GcsCsvOutputWriter.class);
  private String gcsFilePath;
  public String csvFileExtension = ".csv";
  private StringBuilder outputStrBuilder = new StringBuilder();
  private final static int NUM_CHARACTERS_TO_WRITE = 200000000;
  private int file_num = -1;

  public GcsCsvOutputWriter(String outputDir)  {
    gcsFilePath = outputDir;
  }

  public void writeRecForQuery(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns,
                               AntiPatternCommandParser cmdParser) {
      if(outputStrBuilder.length()==0) {
        outputStrBuilder.append(OutputCSVWriterHelper.getHeader(cmdParser));
      }
      outputStrBuilder.append(OutputCSVWriterHelper.getOutputStringForRecord(inputQuery, visitorsThatFoundPatterns, cmdParser));

      // every ~400 MB
      if(outputStrBuilder.length()>=NUM_CHARACTERS_TO_WRITE) {
        writeOutput();
      }
  }

  public void close() {
    if(outputStrBuilder.length()>0){
      writeOutput();
    }

  }

  private void writeOutput() {
    logger.info("Writing recommendations to GCS: " + gcsFilePath);
    String gcsOutputFileName = getGCSOutputFileName();
    GCSHelper gcsHelper = new GCSHelper();
    gcsHelper.writeToGCS(gcsOutputFileName, outputStrBuilder.toString());
    outputStrBuilder = new StringBuilder();
  }

  private String getGCSOutputFileName() {
    file_num += 1;
    if(!gcsFilePath.endsWith(csvFileExtension)) {
      gcsFilePath += csvFileExtension;
    }

    if(file_num == 0 ){
      return gcsFilePath;
    } else {
      return gcsFilePath.replace(csvFileExtension, "_" + file_num + csvFileExtension);
    }
  }

}
