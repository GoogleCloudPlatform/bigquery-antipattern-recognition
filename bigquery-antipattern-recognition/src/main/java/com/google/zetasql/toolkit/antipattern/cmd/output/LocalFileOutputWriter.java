package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class LocalFileOutputWriter extends AntiPatternOutputWriter {

  private String filePath;
  private FileWriter csvWriter;

  public LocalFileOutputWriter(String outputDir)  {
    filePath = outputDir;
  }

  public void writeRecForQuery(InputQuery inputQuery, List<AbstractVisitor> visitorsThatFoundPatterns)
      throws IOException {
    setFileWriter();
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
