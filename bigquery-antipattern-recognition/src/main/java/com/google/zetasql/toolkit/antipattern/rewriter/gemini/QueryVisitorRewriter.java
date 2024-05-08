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
    private final Boolean llmStrictValidation;

    public QueryVisitorRewriter(AntiPatternHelper antiPatternHelper, boolean llmStrictValidation) throws IOException {
        this.promptYamlReader = new PromptYamlReader();
        this.antiPatternHelper = antiPatternHelper;
        this.llmStrictValidation = llmStrictValidation;

    }

    // Retries will be used:
    // 1- In case the antipattern is still there
    // 2- In case the query is syntactically invalid
    public String rewriteSQL(String inputQuery,
                             AntiPatternVisitor visitorThatFoundAntiPattern,
                             Integer llmRetries) throws Exception {
        String prompt = this.promptYamlReader.getAntiPatternNameToPrompt().get(visitorThatFoundAntiPattern.getName());

        if (prompt == null) {
            return  inputQuery; // No changes so it can use the same query
        }

        prompt = String.format(prompt, inputQuery).replace("%%","%");
        String queryStr = GeminiRewriter.processPrompt(prompt, this.antiPatternHelper.getProject());
        if(this.llmStrictValidation && llmRetries>0) {
            queryStr = checkAntiPattern(queryStr, visitorThatFoundAntiPattern, llmRetries);
        }

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
                    if (this.llmStrictValidation) {
                        throw new TTLExpiredDuringRewriteException("LLM couldn't solve the specific anti pattern");
                    }
                    return queryStr;
                }
                llmRetries--;
                queryStr = this.rewriteSQL(queryStr, visitorThatFoundAntiPattern, llmRetries);
            }
            return queryStr;
        } catch (SqlException sqlException) {
            logger.error("The generated query has a syntax error :" + sqlException.getMessage());
            if (llmRetries <= 0) {
                if (this.llmStrictValidation) {
                    throw new TTLExpiredDuringRewriteException("LLM couldn't solve the specific anti pattern");
                }
                return queryStr;
            }
            llmRetries--;

            queryStr = this.fixSyntaxError(queryStr, sqlException.getMessage(), this.antiPatternHelper.getProject());
            return this.checkAntiPattern(queryStr, visitorThatFoundAntiPattern, llmRetries);
        }
    }

    private String fixSyntaxError(String query, String errorDescription, String project) throws IOException {
        String prompt = String.format(
                " The following SQL query has syntax errors and can not be ran:\n" +
                "```\n" +
                "%s\n" +
                "```\n" +
                "Here is the error description: %s\n" +
                "Please fix this query so the syntax is correct and that error is no more\n", query, errorDescription);

        return GeminiRewriter.processPrompt(prompt, project);
    }

}
