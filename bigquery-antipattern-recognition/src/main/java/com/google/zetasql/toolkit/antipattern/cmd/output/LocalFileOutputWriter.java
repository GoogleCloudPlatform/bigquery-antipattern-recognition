package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AntipatternParserVisitor;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileOutputWriter extends AntiPatternOutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(LocalFileOutputWriter.class);

  private String filePath;
  private FileWriter csvWriter;

  public LocalFileOutputWriter(String outputDir)  {
    filePath = outputDir;
  }

  public void writeRecForQuery(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns)
      throws IOException {
    setFileWriter();
    logger.info(String.format("Writing recommendation for query: %s to local file: %s", inputQuery.getQueryId(), filePath));
    csvWriter.write(
        OutputCSVWriterHelper.getOutputStringForRecord(inputQuery, visitorsThatFoundPatterns));
    csvWriter.flush();
  }

  public void close() throws IOException {
    csvWriter.close();
  }

  private void setFileWriter() throws IOException {
    if(csvWriter == null){
      File file = new File(filePath);
      if (file.exists()) {
        csvWriter = new FileWriter(file, true);
      } else {
        csvWriter = new FileWriter(file);
        csvWriter.write(OutputCSVWriterHelper.CSV_HEADER);
      }
    }
  }


}
