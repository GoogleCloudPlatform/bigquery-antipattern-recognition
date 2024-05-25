package com.google.zetasql.toolkit.antipattern.models;

public class BigQueryRemoteFnResult {
    private String name;
    private String result;

    // Constructor
    public BigQueryRemoteFnResult(String name, String result) {
        this.name = name;
        this.result = result;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getResult() {
        return result;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
