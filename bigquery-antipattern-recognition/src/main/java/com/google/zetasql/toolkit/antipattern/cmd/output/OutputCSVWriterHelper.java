package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AbstractVisitor;
import java.util.List;
import java.util.stream.Collectors;

public class OutputCSVWriterHelper {

  public final static String CSV_HEADER = "id,recommendation\n";
  private final static String REC_FORMAT = "%s: %s";
  private final static String OUTPUT_RECORD_FORMAT = "%s,\"%s\"\n";

  public static String getOutputStringForRecord(
      InputQuery inputQuery, List<AbstractVisitor> visitorsThatFoundPatterns) {
    String rec = visitorsThatFoundPatterns.stream().map(
        visitor->String.format(REC_FORMAT, visitor.getNAME(), visitor.getResult()))
        .collect(Collectors.joining("\n"));

    return String.format(
        OUTPUT_RECORD_FORMAT,
        inputQuery.getQueryId(),
        rec
    );
  }
}
