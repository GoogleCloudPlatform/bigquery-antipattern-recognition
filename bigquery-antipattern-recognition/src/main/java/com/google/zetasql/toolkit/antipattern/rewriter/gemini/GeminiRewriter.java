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

package com.google.zetasql.toolkit.antipattern.rewriter.gemini;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.rewriter.prompt.PromptYamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GeminiRewriter {

    private static final Logger logger = LoggerFactory.getLogger(GeminiRewriter.class);
    private static final String API_URI_TEMPLATE = "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1/publishers/google/models/gemini-1.0-pro:streamGenerateContent?alt=sse";

    public static void rewriteSQL(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns,
                                  String projectId, PromptYamlReader promptYamlReader) throws IOException {
        try {
            String queryStr = inputQuery.getQuery();
            for (AntiPatternVisitor visitor : visitorsThatFoundAntiPatterns) {
                String prompt = promptYamlReader.getAntiPatternNameToPrompt().get(visitor.getNAME());
                if (prompt != null) {
                    prompt = String.format(prompt, queryStr).replace("%%","%");
                    queryStr = processPrompt(prompt, projectId);
                }
            }
            if (!queryStr.equals(inputQuery.getQuery())) {
                inputQuery.setOptimizedQuery(queryStr);
            }
        } catch (Exception e) {
            logger.error("Could not rewrite SQL. " + e.getMessage());
        }
    }

    public static String processPrompt(String prompt, String projectId) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        credentials.refreshIfExpired();
        String accessToken = credentials.getAccessToken().getTokenValue();

        // Prepare request
        URL url = new URL(String.format(API_URI_TEMPLATE, projectId));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);

        String body = String.format(GeminiConstants.GEMINI_API_HTTP_POST_BODY, prompt);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // check response
        int responseCode = connection.getResponseCode();
        logger.info(connection.toString());
        logger.info("POST Response Code: " + responseCode);

        // get response body
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String res = response.toString();
                String optimizedSQL = getSqlFromRes(res);
                optimizedSQL = optimizedSQL.replace("```sql", "")
                    .replace("```", "").trim();
                return optimizedSQL;
            }
        } else {
            logger.info("POST request failed" + connection.getResponseMessage());
        }
        return null;
    }

    private static String getSqlFromRes(String res) {
        JsonParser parser = new JsonParser();
        String[] resList = res.split("data: ");
        String  optimizedSQL = "";
        String currText = "";
        for(String currRes: resList){
            if(currRes.length()==0){continue;}
            JsonObject object = (JsonObject) parser.parse(currRes);
            currText = object.getAsJsonArray("candidates")
                .get(0).getAsJsonObject().getAsJsonObject("content")
                .getAsJsonArray("parts").get(0).getAsJsonObject().get("text")
                .getAsString();
            optimizedSQL += currText;
        }

        return optimizedSQL;
    }
}
