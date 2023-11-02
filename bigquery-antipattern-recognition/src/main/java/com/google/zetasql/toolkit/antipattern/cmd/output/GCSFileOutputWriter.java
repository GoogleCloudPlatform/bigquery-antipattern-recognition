package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.Main;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AntipatternParserVisitor;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCSFileOutputWriter extends AntiPatternOutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(GCSFileOutputWriter.class);
  private String gcsFilePath;
  public String csvFileExtension = ".csv";
  private StringBuilder outputStrBuilder = new StringBuilder();
  private final static int NUM_CHARACTERS_TO_WRITE = 200000000;
  private int file_num = -1;

  public GCSFileOutputWriter(String outputDir)  {
    gcsFilePath = outputDir;
  }

  public void writeRecForQuery(
      InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns) {
      if(outputStrBuilder.length()==0) {
        outputStrBuilder.append(OutputCSVWriterHelper.CSV_HEADER);
      }
      outputStrBuilder.append(OutputCSVWriterHelper.getOutputStringForRecord(inputQuery, visitorsThatFoundPatterns));

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
