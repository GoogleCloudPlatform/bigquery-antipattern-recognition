package com.google.zetasql.toolkit.antipattern.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.List;
import java.util.stream.Collectors;

// Removed @JsonPropertyOrder to keep the original order of the fields 
@JsonRootName("replies") 
public class BigQueryRemoteFnResponse {
    @JsonInclude
    private List<BigQueryRemoteFnResult> antipatterns;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;

    public BigQueryRemoteFnResponse(List<BigQueryRemoteFnResult> antipatterns, String errorMessage) {
        this.antipatterns = antipatterns;
        this.errorMessage = errorMessage;
    }

    public List<BigQueryRemoteFnResult> getAntipatterns() {
        return antipatterns;
    }

    public void setAntipatterns(List<BigQueryRemoteFnResult> antipatterns) {
        this.antipatterns = antipatterns;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Method to format AntiPatterns (no changes)
    public static List<BigQueryRemoteFnResult> formatAntiPatterns(List<AntiPatternVisitor> visitors) {
        return visitors.stream()
                .map(visitor -> new BigQueryRemoteFnResult(visitor.getName(), visitor.getResult()))
                .collect(Collectors.toList());
    }
}
