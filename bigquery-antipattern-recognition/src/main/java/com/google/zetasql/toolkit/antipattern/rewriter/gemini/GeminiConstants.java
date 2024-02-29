package com.google.zetasql.toolkit.antipattern.rewriter.gemini;

public final class GeminiConstants {

    public static final String GEMINI_API_HTTP_POST_BODY = "{\n" +
            "  \"contents\": {\n" +
            "    \"role\": \"user\",\n" +
            "    \"parts\": {\n" +
            "        \"text\": \"%s\"\n" +
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
}
