package com.google.zetasql.toolkit.antipattern.udf;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zetasql.toolkit.antipattern.controller.AntiPatternController;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResponse;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResult;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class AntiPatternControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntiPatternController antiPatternController = new AntiPatternController();

    @Test
    public void testRequestWithNoAntipattern() throws Exception {
        BigQueryRemoteFnRequest request = createRequest(List.of("SELECT id FROM dataset.table"));

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);

        List<BigQueryRemoteFnResult> results = responseObj.getAntipatterns();
        assertEquals("None", results.get(0).getName());

    }

    @Test
    public void testRequestWithOneAntipattern() throws Exception {
        BigQueryRemoteFnRequest request = createRequest(List.of("SELECT * FROM dataset.table"));

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);

        List<BigQueryRemoteFnResult> results = responseObj.getAntipatterns();
        assertEquals("SimpleSelectStar", results.get(0).getName());

    }

    @Test
    public void testRequestWithTwoAntipatterns() throws Exception {
        BigQueryRemoteFnRequest request = createRequest(List.of("SELECT * FROM dataset.table ORDER BY id"));

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);

        List<BigQueryRemoteFnResult> results = responseObj.getAntipatterns();
        assertEquals("SimpleSelectStar", results.get(0).getName());
        assertEquals("OrderByWithoutLimit", results.get(1).getName());

    }

    @Test
    public void testRequestWithTwoQueriesWithAntipatterns() throws Exception {
        BigQueryRemoteFnRequest request = createRequest(
                List.of("SELECT * FROM dataset.table", "SELECT id FROM dataset.table ORDER BY id"));

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj1 = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);
        BigQueryRemoteFnResponse responseObj2 = objectMapper.convertValue(response.get("replies").get(1),
                BigQueryRemoteFnResponse.class);

        List<BigQueryRemoteFnResult> results1 = responseObj1.getAntipatterns();
        List<BigQueryRemoteFnResult> results2 = responseObj2.getAntipatterns();

        assertEquals("SimpleSelectStar", results1.get(0).getName());
        assertEquals("OrderByWithoutLimit", results2.get(0).getName());

    }

    @Test
    public void testRequestWithInvalidQuery() throws Exception {
        BigQueryRemoteFnRequest request = createRequest(
                List.of("123"));

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);

        String results = responseObj.getErrorMessage();

        assertEquals("Syntax error: Unexpected integer literal \"123\"", results);

    }

    @Test
    public void testRequestWithAntipatternAndInvalidQuery() throws Exception {
        BigQueryRemoteFnRequest request = createRequest(
                List.of("SELECT * FROM dataset.table", "123"));

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj1 = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);
        BigQueryRemoteFnResponse responseObj2 = objectMapper.convertValue(response.get("replies").get(1),
                BigQueryRemoteFnResponse.class);
        
        List<BigQueryRemoteFnResult> results1 = responseObj1.getAntipatterns();

        String results2 = responseObj2.getErrorMessage();

        assertEquals("SimpleSelectStar", results1.get(0).getName());
        assertEquals("Syntax error: Unexpected integer literal \"123\"", results2);

    }

    private BigQueryRemoteFnRequest createRequest(List<String> queries) {
        List<JsonNode> calls = new ArrayList<>();
    
        for (String query : queries) {
            ArrayNode queryArray = objectMapper.createArrayNode(); 
            queryArray.add(query); 
            calls.add(queryArray); 
        }
    
        return new BigQueryRemoteFnRequest(
            "requestId",
            "caller",
            "sessionUser",
            new HashMap<>(),
            calls 
        );
    }
    


}
