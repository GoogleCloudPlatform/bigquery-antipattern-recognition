package com.google.zetasql.toolkit.antipattern.rewriter.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.zetasql.toolkit.antipattern.rewriter.prompt.GenericPrompt;
import java.util.List;

public class PromptHandler {

  @JsonProperty
  private List<GenericPrompt> prompts;

  public List<GenericPrompt> getPrompts() {
    return prompts;
  }
}
