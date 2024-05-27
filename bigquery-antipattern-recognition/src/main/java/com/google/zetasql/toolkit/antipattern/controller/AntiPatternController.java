package com.google.zetasql.toolkit.antipattern.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResponse;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResult;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AntiPatternController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/")
    public ObjectNode analyzeQueries(@RequestBody BigQueryRemoteFnRequest request) {
        ArrayNode replies = objectMapper.createArrayNode(); 

        for (JsonNode call : request.getCalls()) {
            BigQueryRemoteFnResponse queryResponse = analyzeSingleQuery(call); 
            ObjectNode resultNode = objectMapper.valueToTree(queryResponse); 
            replies.add(resultNode); 
        }

        ObjectNode finalResponse = objectMapper.createObjectNode();
        finalResponse.set("replies", replies); 
        return finalResponse;
    }

    private BigQueryRemoteFnResponse analyzeSingleQuery(JsonNode call) {
        try {
            InputQuery inputQuery = new InputQuery(call.get(0).asText(), "query provided by UDF:");
            List<AntiPatternVisitor> visitors = findAntiPatterns(inputQuery);
            List<BigQueryRemoteFnResult> formattedAntiPatterns = new ArrayList<>();
            if (visitors.isEmpty()) {
                formattedAntiPatterns.add(new BigQueryRemoteFnResult("None", "No antipatterns found"));
            } else {
                formattedAntiPatterns = BigQueryRemoteFnResponse.formatAntiPatterns(visitors);
            }
            return new BigQueryRemoteFnResponse(formattedAntiPatterns, null); 
        } catch (Exception e) {
            return new BigQueryRemoteFnResponse(null, e.getMessage());
        }
    }

    private List<AntiPatternVisitor> findAntiPatterns(InputQuery inputQuery) {
        List<AntiPatternVisitor> visitors = new ArrayList<>();
        AntiPatternHelper antiPatternHelper = new AntiPatternHelper(null, false);
        antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitors);
        return visitors;
    }

    public static List<BigQueryRemoteFnResult> formatAntiPatterns(List<AntiPatternVisitor> visitors) {
        return visitors.stream()
                .map(visitor -> new BigQueryRemoteFnResult(visitor.getName(), visitor.getResult()))
                .collect(Collectors.toList());
    }
}
