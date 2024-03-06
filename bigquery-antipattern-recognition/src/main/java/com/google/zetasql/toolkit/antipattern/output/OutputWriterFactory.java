package com.google.zetasql.toolkit.antipattern.output;

import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;

public class OutputWriterFactory {
  public static OutputWriter getOutputWriter(AntiPatternCommandParser cmdParser) {
    if (cmdParser.hasOutputFileOptionName()){
      if(GCSHelper.isGCSPath(cmdParser.getOutputFileOptionName())){
        return new GcsCsvOutputWriter(cmdParser.getOutputFileOptionName());
      } else {
        return new LocalCsvOutputWriter(cmdParser.getOutputFileOptionName());
      }
    }
    else if(cmdParser.hasOutputTable()) {
      BQOutputWriter outputWriter = new BQOutputWriter(cmdParser.getOutputTable());
      outputWriter.setProcessingProjectName(cmdParser.getProcessingProject());
      return outputWriter;
    }
    else {
      return new LogOutputWriter();
    }
  }
}
