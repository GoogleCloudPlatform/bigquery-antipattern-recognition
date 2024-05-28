/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zetasql.toolkit.antipattern.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class BigQueryRemoteFnRequest {

    private String requestId;
    private String caller;
    private String sessionUser;
    private Map<String, String> userDefinedContext;
    private List<JsonNode> calls;

    public BigQueryRemoteFnRequest(
        @JsonProperty("requestId") String requestId,
        @JsonProperty("caller") String caller,
        @JsonProperty("sessionUser") String sessionUser,
        @JsonProperty("userDefinedContext") Map<String, String> userDefinedContext,
        @JsonProperty("calls") List<JsonNode> calls
    ) {
        this.requestId = requestId;
        this.caller = caller;
        this.sessionUser = sessionUser;
        this.userDefinedContext = userDefinedContext;
        this.calls = calls;
    }

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

    public List<JsonNode> getCalls() {
        return calls;
    }

    public void setCalls(List<JsonNode> calls) {
        this.calls = calls;
    }
}
