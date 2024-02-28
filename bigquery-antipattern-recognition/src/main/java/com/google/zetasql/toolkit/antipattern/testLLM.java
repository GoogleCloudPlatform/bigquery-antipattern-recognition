package com.google.zetasql.toolkit.antipattern;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class testLLM {

    public static void main(String[] args) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        credentials.refreshIfExpired();
        String accessToken = credentials.getAccessToken().getTokenValue();

        String projectId = "pso-dev-cs-cdwp";
        String apiUrl = "https://us-central1-aiplatform.googleapis.com/v1/projects/" + projectId + "/locations/us-central1/publishers/google/models/gemini-1.0-pro:streamGenerateContent?alt=sse";
        String requestJsonPath = "request.json";

        // Prepare request
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true); // Enable sending a request body

        String prompt = "{\n" +
                "  \"contents\": {\n" +
                "    \"role\": \"user\",\n" +
                "    \"parts\": {\n" +
                "        \"text\": \"Give me a recipe for banana bread.\"\n" +
                "    },\n" +
                "  },\n" +
                "  \"safety_settings\": {\n" +
                "    \"category\": \"HARM_CATEGORY_SEXUALLY_EXPLICIT\",\n" +
                "    \"threshold\": \"BLOCK_LOW_AND_ABOVE\"\n" +
                "  },\n" +
                "  \"generation_config\": {\n" +
                "    \"temperature\": 0.2,\n" +
                "    \"topP\": 0.8,\n" +
                "    \"topK\": 40\n" +
                "  }\n" +
                "}";

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = prompt.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        System.out.println("POST Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Response: " + response.toString());
            }
        } else {
            System.out.println("POST request failed");
        }

    }
}
