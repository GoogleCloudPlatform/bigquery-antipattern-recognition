package com.google.zetasql.toolkit.antipattern.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OutputResult {
    private String jobId;
    private String query;
    private Float slotHours;
    private List<Map<String, String>> recommendation;

    public OutputResult(String jobId, String query, Float slotHours, List<Map<String, String>> recommendation) {
        this.jobId = jobId;
        this.query = query;
        this.slotHours = slotHours;
        this.recommendation = recommendation;
    }

    public String getJobId() {
        return jobId;
    }

    public String getQuery() {
        return query;
    }

    public Float getSlotHours() {
        return slotHours;
    }

    public List<RecommendOutput> getListRecommend() {
        List<RecommendOutput> ListRecommendOutput = new ArrayList<>();
        for (Map<String, String> rec : recommendation) {
            ListRecommendOutput.add(new RecommendOutput(rec.get("name"), rec.get("description")));
        }
        return ListRecommendOutput;
    }
}

