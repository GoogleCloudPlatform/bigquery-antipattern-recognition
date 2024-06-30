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

package com.google.zetasql.toolkit.antipattern.output;

import com.google.zetasql.toolkit.antipattern.cmd.AntiPatternCommandParser;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.io.IOException;

public class OutputWriterFactory {

  public static OutputWriter getOutputWriter(AntiPatternCommandParser cmdParser)
      throws IOException {
    if (cmdParser.hasOutputFileOptionName()) {
      if (GCSHelper.isGCSPath(cmdParser.getOutputFileOptionName())) {
        return new GcsCsvOutputWriter(cmdParser.getOutputFileOptionName());
      } else {
        return new LocalCsvOutputWriter(cmdParser.getOutputFileOptionName());
      }
    } else if (cmdParser.hasOutputTable()) {
      BQOutputWriter outputWriter = new BQOutputWriter(cmdParser.getOutputTable(),
          cmdParser.getProcessingProject(), cmdParser.getServiceAccountKeyfilePath());
      outputWriter.setProcessingProjectName(cmdParser.getProcessingProject());
      return outputWriter;
    } else {
      return new LogOutputWriter();
    }
  }
}
