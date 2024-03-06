package com.google.zetasql.toolkit.antipattern.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCsvOutputWriter extends OutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(LocalCsvOutputWriter.class);

  private String filePath;
  private FileWriter csvWriter;

  public LocalCsvOutputWriter(String outputDir)  {
    filePath = outputDir;
  }

  public void writeRecForQuery(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns,
                               AntiPatternCommandParser cmdParser) throws IOException {
    setFileWriter(cmdParser);
    logger.info(String.format("Writing recommendation for query: %s to local file: %s", inputQuery.getQueryId(), filePath));
    csvWriter.write(
        OutputCSVWriterHelper.getOutputStringForRecord(inputQuery, visitorsThatFoundPatterns, cmdParser));
    csvWriter.flush();
  }

  public void close() throws IOException {
    if(csvWriter != null){
      csvWriter.close();
    }
  }

  private void setFileWriter(AntiPatternCommandParser cmdParser) throws IOException {
    if(csvWriter == null){
      File file = new File(filePath);
      if (file.exists()) {
        csvWriter = new FileWriter(file, true);
      } else {
        csvWriter = new FileWriter(file);
        csvWriter.write(OutputCSVWriterHelper.getHeader(cmdParser));
      }
    }
  }


}
