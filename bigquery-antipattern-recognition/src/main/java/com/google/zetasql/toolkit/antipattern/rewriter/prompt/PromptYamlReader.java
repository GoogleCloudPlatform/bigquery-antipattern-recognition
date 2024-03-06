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
