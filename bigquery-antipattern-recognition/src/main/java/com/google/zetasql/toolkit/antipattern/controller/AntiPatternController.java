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

package com.google.zetasql.toolkit.antipattern.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class AntiPatternController {

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/")
    public ObjectNode analyzeQueries(@RequestBody BigQueryRemoteFnRequest request) {
        ArrayNode replies = objectMapper.createArrayNode();

        for (JsonNode call : request.getCalls()) {
            replies.add(analyzeSingleQuery(call));
        }

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.set("replies", replies);

        return jsonObject;
    }

    private ObjectNode analyzeSingleQuery(JsonNode call) {
        ObjectNode queryResponse = objectMapper.createObjectNode();

        try {
            InputQuery inputQuery = new InputQuery(call.get(0).asText(), "query provided by UDF:");
            List<AntiPatternVisitor> visitors = findAntiPatterns(inputQuery);

            if (!visitors.isEmpty()) {
                queryResponse.set("antipatterns", formatAntiPatterns(visitors));
            } else {
                queryResponse.put("None", "No antipatterns found");
            }
        } catch (Exception e) {
            queryResponse.put("errorMessage", e.getMessage());
        }

        return queryResponse;
    }

    private List<AntiPatternVisitor> findAntiPatterns(InputQuery inputQuery) {
        List<AntiPatternVisitor> visitors = new ArrayList<>();
        AntiPatternHelper antiPatternHelper = new AntiPatternHelper(null, false);
        antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitors);
        return visitors;
    }

    private ArrayNode formatAntiPatterns(List<AntiPatternVisitor> visitors) {
        ArrayNode antipatterns = objectMapper.createArrayNode();
        for (AntiPatternVisitor visitor : visitors) {
            ObjectNode antipattern = objectMapper.createObjectNode();
            antipattern.put("name", visitor.getName());
            antipattern.put("result", visitor.getResult());
            antipatterns.add(antipattern);
        }
        return antipatterns;
    }
}
