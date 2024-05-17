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
    public String analyzeQuery(@RequestBody String query) { // Change to @RequestParam
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));

            // Execute the Main class with the query as a command-line argument
            String[] args = {"--query", query};
            Main.main(args);

            System.setOut(System.out);
            return baos.toString();
        } catch (Exception e) {
            return "Error processing the query: " + e.getMessage();
        }
    }
}
