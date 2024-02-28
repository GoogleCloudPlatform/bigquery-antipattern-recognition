package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;

import java.util.List;
import java.util.stream.Collectors;

public class OutputCSVWriterHelper {

  private final static String CSV_HEADER = "id,recommendation";
  private final static String CSV_HEADER_OPTIMIZED_SQL_COL_NAME = "optimized_sql";
  private final static String REC_FORMAT = "%s: %s";
  private final static String BASE_OUTPUT_RECORD_FORMAT = "%s,\"%s\"";

  public static String getOutputStringForRecord(InputQuery inputQuery,
                                                List<AntiPatternVisitor> visitorsThatFoundPatterns,
                                                AntiPatternCommandParser cmdParser) {
    String rec = visitorsThatFoundPatterns.stream().map(
        visitor->String.format(REC_FORMAT, visitor.getNAME(), visitor.getResult()))
        .collect(Collectors.joining("\n"));

    return buildRecord(inputQuery, visitorsThatFoundPatterns, cmdParser, rec);
  }

  public static String getHeader(AntiPatternCommandParser cmdParser) {
    if(cmdParser.rewriteSQL()) {
      return CSV_HEADER + "," + CSV_HEADER_OPTIMIZED_SQL_COL_NAME + "\n";
    } else {
      return CSV_HEADER + "\n";
    }

  }

  public static String getOutputRecFormat(AntiPatternCommandParser cmdParser) {
    if(cmdParser.rewriteSQL()) {
      return BASE_OUTPUT_RECORD_FORMAT + ",\"%s\"\n";
    } else {
      return BASE_OUTPUT_RECORD_FORMAT + "\n";
    }
  }

  public static String buildRecord(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns,
                                   AntiPatternCommandParser cmdParser, String rec) {
    String output_record_format = getOutputRecFormat(cmdParser);
    if(cmdParser.rewriteSQL()) {
      return String.format(
              output_record_format,
              inputQuery.getQueryId(),
              rec,
              inputQuery.getOptimizedQuery());
    } else {
      return String.format(
              output_record_format,
              inputQuery.getQueryId(),
              rec);
    }
  }
}

