/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zetasql.toolkit.antipattern.cmd;

import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AntiPatternCommandParser {

  private static final Logger logger = LoggerFactory.getLogger(AntiPatternCommandParser.class);

  public static final String QUERY_OPTION_NAME = "query";
  public static final String FILE_PATH_OPTION_NAME = "input_file_path";
  public static final String FOLDER_PATH_OPTION_NAME = "input_folder_path";
  public static final String INPUT_CSV_FILE_OPTION_NAME = "input_csv_file_path";
  public static final String INPUT_BQ_TABLE_OPTION_NAME = "input_bq_table";
  public static final String OUTPUT_FILE_OPTION_NAME = "output_file_path";
  public static final String READ_FROM_INFO_SCHEMA_FLAG_NAME = "read_from_info_schema";
  public static final String READ_FROM_INFO_SCHEMA_DAYS_OPTION_NAME = "read_from_info_schema_days";
  public static final String READ_FROM_INFO_SCHEMA_START_TIME_OPTION_NAME = "read_from_info_schema_start_time";
  public static final String READ_FROM_INFO_SCHEMA_END_TIME_OPTION_NAME = "read_from_info_schema_end_time";
  public static final String READ_FROM_INFO_SCHEMA_TIMEOUT_IN_SECS_OPTION_NAME = "read_from_info_schema_timeout_in_secs";
  public static final String INFO_SCHEMA_REGION = "info_schema_region";
  public static final String INFO_SCHEMA_MIN_SLOTMS="info_schema_min_slotms";
  public static final String READ_FROM_INFO_SCHEMA_TABLE_OPTION_NAME = "info_schema_table_name";
  public static final String PROCESSING_PROJECT_ID_OPTION_NAME = "processing_project_id";
  public static final String OUTPUT_TABLE_OPTION_NAME = "output_table";
  public static final String USE_ANALYZER_FLAG_NAME = "advanced_analysis";
  public static final String ANALYZER_DEFAULT_PROJECT_ID_OPTION_NAME = "analyzer_default_project" ;
  public static final String IS_TOP_N_PERC_JOBS_OPTION_NAME = "info_schema_top_n_percentage_of_jobs";
  public static final String REWRITE_SQL_FLAG_NAME = "rewrite_sql";
  private Options options;
  private CommandLine cmd;

  public AntiPatternCommandParser(String[] args) throws ParseException {
    options = getOptions();
    CommandLineParser parser = new BasicParser();
    logger.info("Running anti pattern tool for args:" + String.join(" ", args));
    cmd = parser.parse(options, args);
    logger.info("Running with the following config:" + cmd.toString());

  }

  public String getOutputTable() {
    return cmd.getOptionValue(OUTPUT_TABLE_OPTION_NAME);
  }

  public String getProcessingProject() {
    return cmd.getOptionValue(PROCESSING_PROJECT_ID_OPTION_NAME);
  }

  public String getOutputFileOptionName() {
    return cmd.getOptionValue(OUTPUT_FILE_OPTION_NAME);
  }

  public boolean hasOutputFileOptionName() {
    return cmd.hasOption(OUTPUT_FILE_OPTION_NAME);
  }

  public boolean useAnalyzer() {
    return cmd.hasOption(USE_ANALYZER_FLAG_NAME);
  }

  public boolean isReadingFromInfoSchema() {
    return cmd.hasOption(READ_FROM_INFO_SCHEMA_FLAG_NAME);
  }

  public boolean rewriteSQL() {
    return cmd.hasOption(REWRITE_SQL_FLAG_NAME);
  }

  public boolean hasOutputTable() {
    return cmd.hasOption(OUTPUT_TABLE_OPTION_NAME);
  }

  public String getAnalyzerDefaultProject() {
    return cmd.getOptionValue(ANALYZER_DEFAULT_PROJECT_ID_OPTION_NAME);
  }

  public Options getOptions() {
    Options options = new Options();
    Option query =
        Option.builder(QUERY_OPTION_NAME)
            .argName(QUERY_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("set query")
            .build();
    options.addOption(query);

    Option filePath =
        Option.builder(FILE_PATH_OPTION_NAME)
            .argName(FILE_PATH_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("set file path")
            .build();
    options.addOption(filePath);

    Option folderPath =
        Option.builder(FOLDER_PATH_OPTION_NAME)
            .argName(FOLDER_PATH_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("set file path")
            .build();
    options.addOption(folderPath);

    Option useInfoSchemaFlag =
        Option.builder(READ_FROM_INFO_SCHEMA_FLAG_NAME)
            .argName(READ_FROM_INFO_SCHEMA_FLAG_NAME)
            .required(false)
            .desc("flag specifying if the queries should be read from INFORMATION_SCHEMA")
            .build();
    options.addOption(useInfoSchemaFlag);

    Option rewriteSQLFlag =
            Option.builder(REWRITE_SQL_FLAG_NAME)
                    .argName(REWRITE_SQL_FLAG_NAME)
                    .required(false)
                    .desc("flag specifying if the queries should be rwwritten using an LLM")
                    .build();
    options.addOption(rewriteSQLFlag);

    Option procesingProjectOption =
        Option.builder(PROCESSING_PROJECT_ID_OPTION_NAME)
            .argName(PROCESSING_PROJECT_ID_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("project where the solution will execute")
            .build();
    options.addOption(procesingProjectOption);

    Option outputTableOption =
        Option.builder(OUTPUT_TABLE_OPTION_NAME)
            .argName(OUTPUT_TABLE_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("project with the table to which output will be written")
            .build();
    options.addOption(outputTableOption);

    Option outputFileOption =
        Option.builder(OUTPUT_FILE_OPTION_NAME)
            .argName(OUTPUT_FILE_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("path to csv file for result output")
            .build();
    options.addOption(outputFileOption);

    Option inputCsvFileOption =
        Option.builder(INPUT_CSV_FILE_OPTION_NAME)
            .argName(INPUT_CSV_FILE_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("path to csv file with input queries")
            .build();
    options.addOption(inputCsvFileOption);

    Option inputBqOption =
        Option.builder(INPUT_BQ_TABLE_OPTION_NAME)
          .argName(INPUT_BQ_TABLE_OPTION_NAME)
          .hasArg()
          .required(false)
          .desc("name of bigquery table to pull queries from")
          .build();
      options.addOption(inputBqOption);

    Option infoSchemaDays =
        Option.builder(READ_FROM_INFO_SCHEMA_DAYS_OPTION_NAME)
            .argName(READ_FROM_INFO_SCHEMA_DAYS_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("Specifies how many days back should INFORMATION SCHEMA be queried for")
            .build();
    options.addOption(infoSchemaDays);

    Option infoSchemaStartTime =
        Option.builder(READ_FROM_INFO_SCHEMA_START_TIME_OPTION_NAME)
                .argName(READ_FROM_INFO_SCHEMA_START_TIME_OPTION_NAME)
                .hasArg()
                .required(false)
                .desc("Specifies start timestamp INFORMATION SCHEMA be queried for")
                .build();
    options.addOption(infoSchemaStartTime);

    Option infoSchemaEndTime =
        Option.builder(READ_FROM_INFO_SCHEMA_END_TIME_OPTION_NAME)
                .argName(READ_FROM_INFO_SCHEMA_END_TIME_OPTION_NAME)
                .hasArg()
                .required(false)
                .desc("Specifies end timestamp INFORMATION SCHEMA be queried for")
                .build();
    options.addOption(infoSchemaEndTime);

    Option infoSchemaTimeoutSecs =
        Option.builder(READ_FROM_INFO_SCHEMA_TIMEOUT_IN_SECS_OPTION_NAME)
                .argName(READ_FROM_INFO_SCHEMA_TIMEOUT_IN_SECS_OPTION_NAME)
                .hasArg()
                .required(false)
                .desc("Specifies timeout (in secs) to query INFORMATION SCHEMA")
                .build();
    options.addOption(infoSchemaTimeoutSecs);

    Option infoSchemaSlotmsMin =
            Option.builder(INFO_SCHEMA_MIN_SLOTMS)
                    .argName(INFO_SCHEMA_MIN_SLOTMS)
                    .hasArg()
                    .required(false)
                    .desc("Specifies the minimum number of slotms for a query in INFORMATION_SCHEMA to be" +
                            "selected for processing. Defaults to 0 (all queries are processed)")
                    .build();
    options.addOption(infoSchemaSlotmsMin);

    Option infoSchemaTable =
        Option.builder(READ_FROM_INFO_SCHEMA_TABLE_OPTION_NAME)
            .argName(READ_FROM_INFO_SCHEMA_TABLE_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("Specifies the table INFORMATION SCHEMA be queried for")
            .build();
    options.addOption(infoSchemaTable);

    Option useAnalyzerFlag =
        Option.builder(USE_ANALYZER_FLAG_NAME)
            .argName(USE_ANALYZER_FLAG_NAME)
            .required(false)
            .desc("flag specifying if the analyzer should be used")
            .build();
    options.addOption(useAnalyzerFlag);

    Option anaLyzerDefaultProjectId =
        Option.builder(ANALYZER_DEFAULT_PROJECT_ID_OPTION_NAME)
            .argName(ANALYZER_DEFAULT_PROJECT_ID_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("project id used by analyzer by default")
            .build();
    options.addOption(anaLyzerDefaultProjectId);

    Option ISTopNPercentageJobs =
        Option.builder(IS_TOP_N_PERC_JOBS_OPTION_NAME)
            .argName(IS_TOP_N_PERC_JOBS_OPTION_NAME)
            .hasArg()
            .required(false)
            .desc("Top % of jobs by slot_ms to read from INFORMATION_SCHEMA")
            .build();
    options.addOption(ISTopNPercentageJobs);

    Option region =
        Option.builder(INFO_SCHEMA_REGION)
            .argName(INFO_SCHEMA_REGION)
            .hasArg()
            .required(false)
            .desc("region")
            .build();
    options.addOption(region);

    return options;
  }

  public Iterator<InputQuery> getInputQueries() {
    try {
      if (cmd.hasOption(READ_FROM_INFO_SCHEMA_FLAG_NAME)) {
        return readFromIS();
      } else if (cmd.hasOption(QUERY_OPTION_NAME)) {
        return buildIteratorFromQueryStr(cmd.getOptionValue(QUERY_OPTION_NAME));
      } else if (cmd.hasOption(FILE_PATH_OPTION_NAME)) {
        return buildIteratorFromFilePath(cmd.getOptionValue(FILE_PATH_OPTION_NAME));
      } else if (cmd.hasOption(FOLDER_PATH_OPTION_NAME)) {
        return buildIteratorFromFolderPath(cmd.getOptionValue(FOLDER_PATH_OPTION_NAME));
      } else if (cmd.hasOption(INPUT_CSV_FILE_OPTION_NAME)) {
        return buildIteratorFromCSV(cmd.getOptionValue(INPUT_CSV_FILE_OPTION_NAME));
      } else if (cmd.hasOption(INPUT_BQ_TABLE_OPTION_NAME)) {
        return buildIteratorFromBQTable(cmd.getOptionValue(INPUT_BQ_TABLE_OPTION_NAME));
      }
    } catch (IOException | InterruptedException e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }
    return null;
  }

  private Iterator<InputQuery> readFromIS() throws InterruptedException {
    logger.info("Using INFORMATION_SCHEMA as input source");
    String processingProjectId = cmd.getOptionValue(PROCESSING_PROJECT_ID_OPTION_NAME);
    String infoSchemaDays = cmd.getOptionValue(READ_FROM_INFO_SCHEMA_DAYS_OPTION_NAME);
    String infoSchemaTableName = cmd.getOptionValue(READ_FROM_INFO_SCHEMA_TABLE_OPTION_NAME);
    String infoSchemaSlotmsMin = cmd.getOptionValue(INFO_SCHEMA_MIN_SLOTMS);
    String timeoutInSecs = cmd.getOptionValue(READ_FROM_INFO_SCHEMA_TIMEOUT_IN_SECS_OPTION_NAME);
    String infoSchemaStartTime = cmd.getOptionValue(READ_FROM_INFO_SCHEMA_START_TIME_OPTION_NAME);
    String infoSchemaEndTime = cmd.getOptionValue(READ_FROM_INFO_SCHEMA_END_TIME_OPTION_NAME);
    String customTopNPercent = cmd.getOptionValue(IS_TOP_N_PERC_JOBS_OPTION_NAME);
    String region = cmd.getOptionValue(INFO_SCHEMA_REGION);

    return new InformationSchemaQueryIterable(processingProjectId, infoSchemaDays,
        infoSchemaStartTime, infoSchemaEndTime, infoSchemaTableName, infoSchemaSlotmsMin,
        timeoutInSecs, customTopNPercent, region);
  }

  public static Iterator<InputQuery> buildIteratorFromQueryStr(String queryStr) {
    logger.info("Using inline query as input source");
    InputQuery inputQuery = new InputQuery(queryStr, "query provided by cli:");
    return (new ArrayList<>(Arrays.asList(inputQuery))).iterator();
  }

  public static Iterator<InputQuery> buildIteratorFromFilePath(String filePath) {
    logger.info("Using sql file as input source");
    // Using the folder query iterator with a single file
    return new InputFolderQueryIterable(new ArrayList<>(Arrays.asList(filePath)));
  }

  private static Iterator<InputQuery> buildIteratorFromCSV(String inputCSVPath) throws IOException {
    logger.info("Using csv file as input source");
    return new InputCsvQueryIterator(inputCSVPath);
  }


  private static Iterator<InputQuery> buildIteratorFromBQTable(String inputTable) throws InterruptedException {
    logger.info("Using bq table as input source");
    return new InputBigQueryTableIterator(inputTable);
  }

  private static Iterator<InputQuery> buildIteratorFromFolderPath(String folderPath) {
    logger.info("Using folder as input source");
    if (GCSHelper.isGCSPath(folderPath)) {
      logger.info("Reading input folder from GCS");
      GCSHelper gcsHelper = new GCSHelper();
      return new InputFolderQueryIterable(
          gcsHelper.getListOfFilesInGCSPath(folderPath));
    } else {
      logger.info("Reading input folder from local");
      List<String> fileList =
          Stream.of(new File(folderPath).listFiles())
              .filter(file -> file.isFile())
              .map(File::getAbsolutePath)
              .collect(Collectors.toList());
      return new InputFolderQueryIterable(fileList);
    }
  }
}
