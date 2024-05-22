package com.google.zetasql.toolkit.antipattern.models;

public class BigQueryRemoteFnReply {
    private String antiPatternMessage; // Use lower camelCase for field names
    private String antiPatternSuggestion;

    // Constructor
    public BigQueryRemoteFnReply(String antiPatternMessage, String antiPatternSuggestion) {
        this.antiPatternMessage = antiPatternMessage;
        this.antiPatternSuggestion = antiPatternSuggestion;
    }

    // Getters
    public String getAntiPatternMessage() {
        return antiPatternMessage;
    }

    public String getAntiPatternSuggestion() {
        return antiPatternSuggestion;
    }

    // Setters
    public void setAntiPatternMessage(String antiPatternMessage) {
        this.antiPatternMessage = antiPatternMessage;
    }

    public void setAntiPatternSuggestion(String antiPatternSuggestion) {
        this.antiPatternSuggestion = antiPatternSuggestion;
    }
}
