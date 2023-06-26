package com.google.zetasql.toolkit.antipattern;

import java.util.Map;

public class Recommendation {

  private final RecommendationType type;
  private final String description;

  public Recommendation(RecommendationType type, String description) {
    this.type = type;
    this.description = description;
  }

  public RecommendationType getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public Map<String, String> toMap() {
    return Map.of(
        "name", this.type.name(),
        "description", this.description
    );
  }

}
