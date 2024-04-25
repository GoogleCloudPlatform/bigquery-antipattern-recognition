package com.google.zetasql.toolkit.antipattern.rewriter.gemini;

import com.google.zetasql.SqlException;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder.JoinOrderVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.exceptions.TTLExpiredDuringRewriteException;
import com.google.zetasql.toolkit.antipattern.rewriter.prompt.PromptYamlReader;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class QueryVisitorRewriter {
    private static final Logger logger = LoggerFactory.getLogger(QueryVisitorRewriter.class);
    private final PromptYamlReader promptYamlReader;
    private final AntiPatternHelper antiPatternHelper;
    private final Boolean llmBestEffort;

    public QueryVisitorRewriter(AntiPatternHelper antiPatternHelper, boolean llmBestEffort) throws IOException {
        this.promptYamlReader = new PromptYamlReader();
        this.antiPatternHelper = antiPatternHelper;
        this.llmBestEffort = llmBestEffort;

    }

    public String rewriteSQL(String inputQuery,
                             AntiPatternVisitor visitor,
                             Integer llmRetries) throws Exception {
        String prompt = this.promptYamlReader.getAntiPatternNameToPrompt().get(visitor.getName());

        if (prompt == null) {
            return  inputQuery; // No changes so it can use the same query
        }

        prompt = String.format(prompt, inputQuery).replace("%%","%");
        String queryStr = GeminiRewriter.processPrompt(prompt, this.antiPatternHelper.getProject());

        queryStr = checkAntiPattern(queryStr, visitor, llmRetries);

        return queryStr;
    }

    public String checkAntiPattern(String queryStr,
                                   AntiPatternVisitor visitorThatFoundAntiPattern,
                                   Integer llmRetries) throws Exception {
        InputQuery inputQuery = new InputQuery(queryStr, "intermediate_rewrite");
        List<AntiPatternVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();
        try {

            // get only the visitor that we want to check
            List<AntiPatternVisitor> parserVisitorList = antiPatternHelper
                    .getParserVisitorList(inputQuery.getQuery())
                    .stream()
                    .filter(ap -> Objects.equals(ap.getName(), visitorThatFoundAntiPattern.getName()))
                    .collect(Collectors.toList());

            // parser visitors
            this.antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitorsThatFoundAntiPatterns, parserVisitorList);

            // analyzer visitor
            if (this.antiPatternHelper.getUseAnalizer() && visitorThatFoundAntiPattern.getName().equals(JoinOrderVisitor.NAME)) {
                this.antiPatternHelper.checkForAntiPatternsInQueryWithAnalyzerVisitors(inputQuery, visitorsThatFoundAntiPatterns);
            }

            if (! visitorsThatFoundAntiPatterns.isEmpty()) {
                if (llmRetries <= 0) {
                    if (this.llmBestEffort) {
                        return queryStr;
                    } else {
                        throw new TTLExpiredDuringRewriteException("LLM couldn't solve the specific anti pattern");
                    }
                }
                llmRetries--;
                queryStr = this.rewriteSQL(queryStr, visitorThatFoundAntiPattern, llmRetries);
            }
            return queryStr;
        } catch (SqlException sqlException) {
            logger.error("The generated query has a syntax error :" + sqlException.getMessage());
            if (llmRetries <= 0) {
                if (this.llmBestEffort) {
                    return queryStr;
                } else {
                    throw new TTLExpiredDuringRewriteException("LLM couldn't solve the specific anti pattern");
                }
            }
            llmRetries--;

            queryStr = this.fixSyntaxError(queryStr, sqlException.getMessage(), this.antiPatternHelper.getProject());
            return this.checkAntiPattern(queryStr, visitorThatFoundAntiPattern, llmRetries);
        }
    }

    private String fixSyntaxError(String query, String errorDescription, String project) throws IOException {
        String prompt = String.format("""
        The following SQL query has syntax errors and can not be ran:
        ```
        %s
        ```
        Here is the error description: %s
        
        Please fix this query so the syntax is correct and that error is no more
        """, query, errorDescription);

        return GeminiRewriter.processPrompt(prompt, project);
    }

}