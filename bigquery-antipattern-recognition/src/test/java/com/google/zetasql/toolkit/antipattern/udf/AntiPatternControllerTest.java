package com.google.zetasql.toolkit.antipattern.udf;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zetasql.toolkit.antipattern.controller.AntiPatternController;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResponse;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResult;

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
    public void testRequestWithOneAntipattern() throws Exception {
        BigQueryRemoteFnRequest request = createRequest("SELECT * FROM dataset.table");

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);

        List<BigQueryRemoteFnResult> results = responseObj.getAntipatterns();
        assertEquals("SimpleSelectStar", results.get(0).getName());

    }

    @Test
    public void testRequestWithNoAntipattern() throws Exception {
        BigQueryRemoteFnRequest request = createRequest("SELECT id FROM dataset.table");

        ObjectNode response = antiPatternController.analyzeQueries(request);
        BigQueryRemoteFnResponse responseObj = objectMapper.convertValue(response.get("replies").get(0),
                BigQueryRemoteFnResponse.class);

        List<BigQueryRemoteFnResult> results = responseObj.getAntipatterns();
        assertEquals("None", results.get(0).getName());

    }
    
    private BigQueryRemoteFnRequest createRequest(String query) {
        return new BigQueryRemoteFnRequest(
                "requestId",
                "caller",
                "sessionUser",
                new HashMap<>(),
                List.of(objectMapper.createArrayNode().add(query)));
    }

}
