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
 import com.google.zetasql.toolkit.antipattern.Main;
 import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
 import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResponse;
 import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnReply;
 import org.springframework.web.bind.annotation.PostMapping;
 import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
 import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
 import com.google.gson.JsonObject;
 import com.google.gson.JsonParser;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
 
 @RestController
 public class AntiPatternController {
 
     @PostMapping("/") // Default endpoint for BigQuery remote functions
     public void analyzeQueries(@RequestBody BigQueryRemoteFnRequest request) {
        BigQueryRemoteFnResponse response;
         InputQuery inputQuery;
         for (Object row : request.getCalls()) {
            //  AntiPatternHelper antiPatternHelper = new AntiPatternHelper(null, null);
            //  try {
            //      List<AntiPatternVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();
            //     // parser visitors
            //     antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitorsThatFoundAntiPatterns);
            //  } catch (Exception e) {
            //      response.addReply("error", e.getMessage());
            //  }
            response = new BigQueryRemoteFnResponse(new BigQueryRemoteFnReply[] {}, "null");
                System.out.println(row);
         }
         
        //  return response;
     }
 }
 


            
