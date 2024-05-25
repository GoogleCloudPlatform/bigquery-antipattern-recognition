package com.google.zetasql.toolkit.antipattern.udf;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zetasql.toolkit.antipattern.controller.AntiPatternController;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class AntiPatternControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntiPatternController antiPatternController = new AntiPatternController();

    @Test
    public void testAnalyzeQueriesWithSimpleSelectStarAntiPattern() throws Exception {
        BigQueryRemoteFnRequest request = createRequest("SELECT * FROM dataset.table");

        ObjectNode response = antiPatternController.analyzeQueries(request);

        JsonNode replies = response.getReplies();
        assertEquals(1, replies.size());

        JsonNode antipattern = replies.get(0);
        assertTrue(antipattern.has("name"));
        assertTrue(antipattern.has("result"));
        assertNull(response.getErrorMessage());  // No error message expected
    }

    @Test
    public void testAnalyzeQueriesNoAntiPattern() throws Exception {
        BigQueryRemoteFnRequest request = createRequest("SELECT column1, column2 FROM dataset.table"); 

        BigQueryRemoteFnResponse response = antiPatternController.analyzeQueries(request);

        JsonNode replies = response.getReplies();
        assertEquals(1, replies.size());

        JsonNode reply = replies.get(0);
        assertTrue(reply.has("None")); 
        assertNull(response.getErrorMessage()); // No error message expected
    }

    @Test
    public void testAnalyzeQueriesMultipleCallsWithMixedResults() throws Exception {
        BigQueryRemoteFnRequest request = new BigQueryRemoteFnRequest(
            "requestId",
            "caller",
            "sessionUser",
            new HashMap<>(),
            List.of(
                objectMapper.createArrayNode().add("SELECT * FROM dataset.table1"),
                objectMapper.createArrayNode().add("SELECT column1 FROM dataset.table3")
            ));

        BigQueryRemoteFnResponse response = antiPatternController.analyzeQueries(request);

        JsonNode replies = response.getReplies();
        assertEquals(2, replies.size());

        // First query should have antipatterns
        JsonNode reply1 = replies.get(0);
        assertTrue(reply1.has("name"));
        assertTrue(reply1.has("result"));

        // Second query should not have antipatterns
        JsonNode reply2 = replies.get(1);
        assertTrue(reply2.has("None"));

        assertNull(response.getErrorMessage()); // No error message expected
    }

    @Test
    public void testAnalyzeQueriesInvalidQuery() throws Exception {
        BigQueryRemoteFnRequest request = createRequest("123"); // Invalid query

        BigQueryRemoteFnResponse response = antiPatternController.analyzeQueries(request);
        assertNull(response.getReplies());   // Replies should be null

        String errorMessage = response.getErrorMessage();
        assertTrue(errorMessage != null && !errorMessage.isEmpty());  // Error message should be present
    }
    
    // Helper method to create a request with a single call
    private BigQueryRemoteFnRequest createRequest(String query) {
        return new BigQueryRemoteFnRequest(
                "requestId",
                "caller",
                "sessionUser",
                new HashMap<>(),
                List.of(objectMapper.createArrayNode().add(query)));
    }

}
