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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.List;
import java.util.stream.Collectors;

@JsonRootName("replies")
public class BigQueryRemoteFnResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
