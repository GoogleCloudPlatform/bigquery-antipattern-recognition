package com.google.zetasql.toolkit.antipattern.rewriter.prompt;

import java.util.List;

public class PromptDetails {
  private String name;
  private String description;
  private List<String> examples;

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getExamples() {
    return examples;
  }
}
