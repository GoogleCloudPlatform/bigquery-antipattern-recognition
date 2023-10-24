package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class LocalFileOutputGenerator extends AbstractOutputGenerator {

  private String filePath;
  private final static String CSV_HEADER = "id,recommendation\n";
  private final static String REC_FORMAT = "%s: %s";
  private final static String OUTPUT_RECORD_FORMAT = "%s,\"%s\"\n";
  public LocalFileOutputGenerator(String outputDir)  {
    filePath = outputDir;
  }

  public void writeRecForQuery(InputQuery inputQuery, List<AbstractVisitor> visitorsThatFoundPatterns)
      throws IOException {
    FileWriter csvWriter;
    File file = new File(filePath);
    if (file.exists()) {
      csvWriter = new FileWriter(file, true);
    } else {
      csvWriter = new FileWriter(file);
      csvWriter.write(CSV_HEADER);
    }
    csvWriter.write(String.format(
        OUTPUT_RECORD_FORMAT,
        inputQuery.getQueryId(),
        visitorsThatFoundPatterns.stream().map(visitor->String.format(REC_FORMAT, visitor.NAME, visitor.getResult())).collect(
            Collectors.joining("\n"))
        )
    );
    csvWriter.close();

  }


}
