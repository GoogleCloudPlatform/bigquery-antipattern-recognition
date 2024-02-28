package com.google.zetasql.toolkit.antipattern.rewriter.gemini;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.rewriter.prompt.RewriterConstants;
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

    public static void rewriteSQL(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns,
                                  String projectId) throws IOException {
        String queryStr = inputQuery.getQuery();
        for(AntiPatternVisitor visitor: visitorsThatFoundAntiPatterns) {
            String prompt = RewriterConstants.antiPatternNameToGeminiPrompt.get(visitor.getNAME());
            prompt = String.format(prompt, queryStr);
            queryStr = processPrompt(prompt, projectId);
        }
        inputQuery.setOptimizedQuery(queryStr);
    }

    public static String processPrompt(String prompt, String projectId) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        credentials.refreshIfExpired();
        String accessToken = credentials.getAccessToken().getTokenValue();

        String apiUrl = "https://us-central1-aiplatform.googleapis.com/v1/projects/" + projectId + "/locations/us-central1/publishers/google/models/gemini-1.0-pro:streamGenerateContent?alt=sse";

        // Prepare request
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);

        // add body to request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = prompt.getBytes(StandardCharsets.UTF_8);
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
                return response.toString();
            }
        } else {
            logger.info("POST request failed" + connection.getResponseMessage());
        }
        return null;
    }
}
