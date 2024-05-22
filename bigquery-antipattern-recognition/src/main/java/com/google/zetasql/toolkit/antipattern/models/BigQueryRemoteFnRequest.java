package com.google.zetasql.toolkit.antipattern.models;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class BigQueryRemoteFnRequest {

    private String requestId;
    private String caller;
    private String sessionUser;
    @Nullable private Map<String, String> userDefinedContext;
    private List<Object> calls;

    // Constructor
    public BigQueryRemoteFnRequest(String requestId, String caller, String sessionUser,
                                  @Nullable Map<String, String> userDefinedContext, List<Object> calls) {
        this.requestId = requestId;
        this.caller = caller;
        this.sessionUser = sessionUser;
        this.userDefinedContext = userDefinedContext;
        this.calls = calls;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getSessionUser() {
        return sessionUser;
    }

    public void setSessionUser(String sessionUser) {
        this.sessionUser = sessionUser;
    }

    @Nullable
    public Map<String, String> getUserDefinedContext() {
        return userDefinedContext;
    }

    public void setUserDefinedContext(@Nullable Map<String, String> userDefinedContext) {
        this.userDefinedContext = userDefinedContext;
    }

    public List<Object> getCalls() {
        return calls;
    }

    public void setCalls(List<Object> calls) {
        this.calls = calls;
    }
}
