package com.google.zetasql.toolkit.antipattern.rewriter.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PromptDetailsList {

  @JsonProperty
  private List<PromptDetails> prompts;

  public List<PromptDetails> getPrompts() {
    return prompts;
  }
}
