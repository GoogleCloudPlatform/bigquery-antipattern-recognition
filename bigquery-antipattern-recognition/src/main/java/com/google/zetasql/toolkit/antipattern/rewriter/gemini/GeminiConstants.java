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
