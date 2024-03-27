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

package com.google.zetasql.toolkit.antipattern.rewriter.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromptYamlReader {

  private final static String EXAMPLE_HEADER = "Example %d:\n";
  private final static String YAML_FILE_NAME = "antiPatternExamples.yaml";

  private final static Map<String, String> antiPatternNameToPrompt = new HashMap<>();

  private PromptDetailsList promptDetailsList;

  public PromptYamlReader() throws IOException {
    populatePromptHandler();
    for(PromptDetails prompt: promptDetailsList.getPrompts()) {
      String promptStr = String.format(RewriterConstants.PROMPT_TEMPLATE,
          RewriterConstants.PROMPT_HEADER,
          prompt.getDescription(),
          getExampleString(prompt.getExamples()),
          RewriterConstants.PROMPT_FOOTER);
      antiPatternNameToPrompt.put(prompt.getName(), promptStr);
    }
  }

  private void populatePromptHandler() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource(YAML_FILE_NAME).getFile());
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    promptDetailsList = mapper.readValue(file, PromptDetailsList.class);
  }

  private String getExampleString(List<String> examples) {
    int count = 0;
    StringBuilder sb = new StringBuilder();
    for(String example: examples) {
      count+=1;
      sb.append(String.format(EXAMPLE_HEADER, count));
      sb.append(example + "\n");
    }
    return sb.toString();
  }

  public Map<String, String> getAntiPatternNameToPrompt() {
    return antiPatternNameToPrompt;
  }
}
