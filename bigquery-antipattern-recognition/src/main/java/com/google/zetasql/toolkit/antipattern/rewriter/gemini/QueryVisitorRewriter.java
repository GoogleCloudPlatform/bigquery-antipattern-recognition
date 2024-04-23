package com.google.zetasql.toolkit.antipattern.rewriter.gemini;

import com.google.zetasql.SqlException;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.rewriter.prompt.PromptYamlReader;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class QueryVisitorRewriter {
    private static final Logger logger = LoggerFactory.getLogger(QueryVisitorRewriter.class);
    private final PromptYamlReader promptYamlReader;
    private final AntiPatternHelper antiPatternHelper;

    public QueryVisitorRewriter(AntiPatternHelper antiPatternHelper) throws IOException {
        this.promptYamlReader = new PromptYamlReader();
        this.antiPatternHelper = antiPatternHelper;
    }

    public String rewriteSQL(String inputQuery,
                             AntiPatternVisitor visitor,
                             Integer TTL) throws Exception {
        TTL--;
        if (TTL < 0) {
            throw new Exception("LLM couldn't solve the specific anti pattern");
        }
        String prompt = this.promptYamlReader.getAntiPatternNameToPrompt().get(visitor.getName());

        if (prompt == null) {
            return  inputQuery; // No changes so it can use the same query
        }

        prompt = String.format(prompt, inputQuery).replace("%%","%");
        String queryStr = GeminiRewriter.processPrompt(prompt, this.antiPatternHelper.getProject());

        queryStr = checkAntiPattern(queryStr, visitor, TTL);

        return queryStr;
    }

    public String checkAntiPattern(String queryStr,
                                   AntiPatternVisitor visitor,
                                   Integer TTL) throws Exception {
        InputQuery inputQuery = new InputQuery(queryStr, "intermediate_rewrite");
        List<AntiPatternVisitor> visitorsThatFoundAntiPatterns = new ArrayList<>();
        try {
            // parser visitors
            this.antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitorsThatFoundAntiPatterns);

            // analyzer visitor
            if (this.antiPatternHelper.getUseAnalizer()) {
                this.antiPatternHelper.checkForAntiPatternsInQueryWithAnalyzerVisitors(inputQuery, visitorsThatFoundAntiPatterns);
            }
            Optional<AntiPatternVisitor> newAntiPattern = visitorsThatFoundAntiPatterns.stream().filter(ap -> Objects.equals(ap.getName(), visitor.getName())).findFirst();
            if (newAntiPattern.isPresent()) { // Check if the problem is no more
                queryStr = this.rewriteSQL(queryStr, visitor, TTL);
            }
            return queryStr;
        } catch (SqlException sqlException) {
            logger.error("The generated query has a syntax error :" + sqlException.getMessage());
            queryStr = this.fixSyntaxError(queryStr, sqlException.getMessage(), this.antiPatternHelper.getProject());
            if (TTL < 0) {
                throw new Exception("LLM couldn't solve the specific anti pattern");
            }
            TTL--;
            return this.checkAntiPattern(queryStr, visitor, TTL);
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