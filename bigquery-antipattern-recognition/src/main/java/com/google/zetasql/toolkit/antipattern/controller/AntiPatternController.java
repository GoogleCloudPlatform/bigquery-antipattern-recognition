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

import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import org.springframework.web.bind.annotation.PostMapping;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

@RestController
public class AntiPatternController {

    @PostMapping("/") 
    public ObjectNode analyzeQueries(@RequestBody BigQueryRemoteFnRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode replies = mapper.createArrayNode();
        for (JsonNode call : request.getCalls()) {
            AntiPatternHelper antiPatternHelper = new AntiPatternHelper(null, false);
            try {
                InputQuery inputQuery = new InputQuery(call.get(0).asText(), "query provided by cli:");
                List<AntiPatternVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();
                antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery,
                        visitorsThatFoundAntiPatterns);
                if (visitorsThatFoundAntiPatterns.size() > 0) {
                    for (AntiPatternVisitor visitor : visitorsThatFoundAntiPatterns) {
                        replies.add(visitor.getName());
                    }
                } else {
                    replies.add("No antipatterns found");
                }

            } catch (Exception e) {
                // response.addReply("error", e.getMessage());
            }
        }

        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.set("replies", replies);

        return jsonObject;
    }
}
