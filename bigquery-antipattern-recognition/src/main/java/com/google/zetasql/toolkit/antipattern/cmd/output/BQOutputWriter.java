package com.google.zetasql.toolkit.antipattern.cmd.output;

import com.google.api.client.util.DateTime;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.Main;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.AntipatternParserVisitor;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQOutputWriter extends AntiPatternOutputWriter {

  private static final Logger logger = LoggerFactory.getLogger(BQOutputWriter.class);
  public static final String JOB_IDENTIFIER_COL_NAME = "job_id";
  public static final String QUERY_COL_NAME = "query";
  public static final String SLOT_HOURS_COL_NAME = "slot_hours";
  public static final String USER_EMAIL_COL_NAME = "user_email";
  public static final String RECOMMENDATION_COL_NAME = "recommendation";
  public static final String PROCESS_TIMESTAMP_COL_NAME = "process_timestamp";
  public static final String REC_NAME_COL_NAME = "name";
  public static final String DESCRIPTION_COL_NAME = "description";
  private String tableName;
  private String processingProjectName;
  private DateTime date;

  public BQOutputWriter(String outputDir) {
    tableName = outputDir;
    date = new DateTime(new Date());
  }

  public void setProcessingProjectName(String processingProjectName) {
    this.processingProjectName = processingProjectName;
  }


  public void writeRecForQuery(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundPatterns) {

    List<Map<String, String>> rec_list = new ArrayList<>();
    for(AntiPatternVisitor visitor: visitorsThatFoundPatterns) {

      Map<String, String> rec = new HashMap<>();
      rec.put(REC_NAME_COL_NAME, visitor.getNAME());
      rec.put(DESCRIPTION_COL_NAME, visitor.getResult());
      rec_list.add(rec);

      Map<String, Object> rowContent = new HashMap<>();
      rowContent.put(JOB_IDENTIFIER_COL_NAME, inputQuery.getQueryId());
      rowContent.put(QUERY_COL_NAME, inputQuery.getQuery());
      rowContent.put(
          SLOT_HOURS_COL_NAME, inputQuery.getSlotHours() >= 0 ? inputQuery.getSlotHours() : null);
      rowContent.put(USER_EMAIL_COL_NAME, inputQuery.getUserEmail());
      rowContent.put(RECOMMENDATION_COL_NAME, rec_list);
      rowContent.put(PROCESS_TIMESTAMP_COL_NAME, date);
      logger.info("Writing rec to BQ :" + rowContent);
      BigQueryHelper.writeResults(
          processingProjectName, tableName, rowContent);
    }
  }

}



