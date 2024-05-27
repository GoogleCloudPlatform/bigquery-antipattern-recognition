package com.google.zetasql.toolkit.antipattern.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.List;
import java.util.stream.Collectors;

@JsonRootName("replies") 
public class BigQueryRemoteFnResponse {
    @JsonInclude
    List<BigQueryRemoteFnResult> antipatterns;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;

    public BigQueryRemoteFnResponse(List<BigQueryRemoteFnResult> antipatterns, String errorMessage) {
        this.antipatterns = antipatterns;
        this.errorMessage = errorMessage;
    }

    public BigQueryRemoteFnResponse() {
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

    public static List<BigQueryRemoteFnResult> formatAntiPatterns(List<AntiPatternVisitor> visitors) {
        return visitors.stream()
                .map(visitor -> new BigQueryRemoteFnResult(visitor.getName(), visitor.getResult()))
                .collect(Collectors.toList());
    }
}
