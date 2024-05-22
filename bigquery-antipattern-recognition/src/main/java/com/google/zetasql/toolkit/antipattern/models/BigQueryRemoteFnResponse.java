package com.google.zetasql.toolkit.antipattern.models;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;


public class BigQueryRemoteFnResponse {

    private List<BigQueryRemoteFnReply> replies;
    private String errorMessage;

    // Constructor
    public BigQueryRemoteFnResponse(List<BigQueryRemoteFnReply> replies, String errorMessage) {
        this.replies = replies;
        this.errorMessage = errorMessage;
    }

    // Getters
    public List<?> getReplies() {
        return replies;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Setters
    public void setReplies(List<BigQueryRemoteFnReply> replies) {
        this.replies = replies;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public static BigQueryRemoteFnResponse withReplies(List<BigQueryRemoteFnReply> replies) {
        return new BigQueryRemoteFnResponse(checkNotNull(replies), null);
      }
    
      public static BigQueryRemoteFnResponse withErrorMessage(String errorMessage) {
        return new BigQueryRemoteFnResponse(null, errorMessage);
      }
}
