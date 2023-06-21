package com.google.zetasql.toolkit.antipattern.cmd;

public class RecommendOutput {
    private String name;
    private String description;
    public RecommendOutput(String name, String description) {
        this.name = name;
        this.description = description;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
}