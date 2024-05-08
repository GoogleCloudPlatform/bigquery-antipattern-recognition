/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zetasql.toolkit.antipattern;

import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.output.OutputWriter;
import com.google.zetasql.toolkit.antipattern.output.OutputWriterFactory;
import com.google.zetasql.toolkit.antipattern.rewriter.gemini.GeminiRewriter;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static AntiPatternCommandParser cmdParser;
  private static int countQueriesRead = 0;
  private static int countQueriesWithAntipattern = 0;


  public static void main(String[] args) throws ParseException, IOException {
    cmdParser = new AntiPatternCommandParser(args);

    AntiPatternHelper antiPatternHelper = new AntiPatternHelper(cmdParser.getProcessingProject(), cmdParser.useAnalyzer());

    Iterator<InputQuery> inputQueriesIterator = cmdParser.getInputQueries();
    OutputWriter outputWriter = OutputWriterFactory.getOutputWriter(cmdParser);
    Boolean rewriteSQL = cmdParser.rewriteSQL();
    outputWriter.setRewriteSQL(rewriteSQL);

    InputQuery inputQuery;
    while (inputQueriesIterator.hasNext()) {
      inputQuery = inputQueriesIterator.next();
      logger.info("Parsing query: " + inputQuery.getQueryId());
      executeAntiPatternsInQuery(inputQuery, outputWriter, cmdParser, antiPatternHelper);
      countQueriesRead += 1;
    }
    logResultStats();
    outputWriter.close();
  }

  private static void executeAntiPatternsInQuery(InputQuery inputQuery,
                                                 OutputWriter outputWriter,
                                                 AntiPatternCommandParser cmdParser,
                                                 AntiPatternHelper antiPatternHelper) {

    try {
      List<AntiPatternVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();
      // parser visitors
      antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitorsThatFoundAntiPatterns);

      // analyzer visitor
      if (antiPatternHelper.getUseAnalizer()) {
        antiPatternHelper.checkForAntiPatternsInQueryWithAnalyzerVisitors(inputQuery, visitorsThatFoundAntiPatterns);
      }

      // rewrite
      if(cmdParser.rewriteSQL()) {
        GeminiRewriter.rewriteSQL(inputQuery, visitorsThatFoundAntiPatterns, antiPatternHelper,
                cmdParser.getLlmRetriesSQL(), cmdParser.getLlmStrictValidation());
      }

      // write output
      if (!visitorsThatFoundAntiPatterns.isEmpty()) {
        countQueriesWithAntipattern += 1;
        outputWriter.writeRecForQuery(inputQuery, visitorsThatFoundAntiPatterns, cmdParser);
      }

    } catch (Exception e) {
      logger.error("Error processing query with id: " + inputQuery.getQueryId());
      logger.error(e.getMessage(), e);
    }
  }

  private static void logResultStats(){
    StringBuilder statsString = new StringBuilder();
    statsString.append("\n\n* Queries read: " + countQueriesRead);
    statsString.append("\n* Queries with anti patterns: " + countQueriesWithAntipattern);
    logger.info(statsString.toString());
  }
}
