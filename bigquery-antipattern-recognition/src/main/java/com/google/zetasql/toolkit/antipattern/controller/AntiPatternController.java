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

import com.google.zetasql.toolkit.antipattern.Main;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AntiPatternController {

    @PostMapping("/")
    public String analyzeQuery(@RequestBody String query) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));
            String[] args = {"--query", query};
            Main.main(args);
            System.setOut(System.out);
            return baos.toString();
        } catch (Exception e) {
            return "Error processing the query: " + e.getMessage();
        }
    }
}
