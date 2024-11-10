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

package com.google.zetasql.toolkit.antipattern.util;

import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder.JoinOrderVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.parser.visitors.*;
import com.google.zetasql.toolkit.antipattern.parser.visitors.rownum.IdentifyLatestRecordVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AntiPatternHelper {
    private static final Logger logger = LoggerFactory.getLogger(AntiPatternHelper.class);
    private String analyzerProject = null;
    private BigQueryAPIResourceProvider resourceProvider;
    private ZetaSQLToolkitAnalyzer analyzer;
    private HashMap<String, Integer> visitorMetricsMap;
    private AnalyzerOptions analyzerOptions;
    private final BigQueryService service;
    private final String project;
    private final LanguageOptions languageOptions;
    private final Boolean useAnalizer;
    public AntiPatternHelper(String project, Boolean useAnalizer) {
        this.project = project;
        this.useAnalizer = useAnalizer;

        this.languageOptions = new LanguageOptions();
        languageOptions.enableMaximumLanguageFeatures();
        languageOptions.setSupportsAllStatementKinds();
        languageOptions.enableReservableKeyword("QUALIFY");

        if (useAnalizer) {
            this.analyzerOptions = new AnalyzerOptions();
            this.analyzer = getAnalyzer(this.analyzerOptions);
            this.service = BigQueryService.buildDefault();
            this.resourceProvider = BigQueryAPIResourceProvider.build(service);
        } else {
            this.service = null;
        }
    }

    public void checkForAntiPatternsInQueryWithParserVisitors(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns) {
        List<AntiPatternVisitor> parserVisitorList = getParserVisitorList(inputQuery.getQuery());

        checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitorsThatFoundAntiPatterns, parserVisitorList);
    }

    public void checkForAntiPatternsInQueryWithParserVisitors(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns, List<AntiPatternVisitor> parserVisitorList) {
        if(this.visitorMetricsMap == null) {
            setVisitorMetricsMap(parserVisitorList);
        }

        for (AntiPatternVisitor visitorThatFoundAntiPattern : parserVisitorList) {
            logger.info("Parsing query with id: " + inputQuery.getQueryId() +
                    " for anti-pattern: " + visitorThatFoundAntiPattern.getName());
            ASTNodes.ASTScript parsedQuery = Parser.parseScript( inputQuery.getQuery(), this.languageOptions);
            try{
                parsedQuery.accept((ParseTreeVisitor) visitorThatFoundAntiPattern);
                String result = visitorThatFoundAntiPattern.getResult();
                if(result.length() > 0) {
                    visitorsThatFoundAntiPatterns.add(visitorThatFoundAntiPattern);
                    this.visitorMetricsMap.merge(visitorThatFoundAntiPattern.getName(), 1, Integer::sum);
                }
            } catch (Exception e) {
                logger.error("Error parsing query with id: " + inputQuery.getQueryId() +
                        " for anti-pattern:" + visitorThatFoundAntiPattern.getName());
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void checkForAntiPatternsInQueryWithAnalyzerVisitors(InputQuery inputQuery, List<AntiPatternVisitor> visitorsThatFoundAntiPatterns) {
        String query = inputQuery.getQuery();
        String currentProject;

        if (inputQuery.getProjectId() == null) {
            currentProject = this.project;
        } else {
            currentProject = inputQuery.getProjectId();
        }

        BigQueryCatalog catalog = new BigQueryCatalog("");
        if ((this.analyzerProject == null || !this.analyzerProject.equals(currentProject))) {
            this.analyzerProject = inputQuery.getProjectId();
            catalog = new BigQueryCatalog(this.analyzerProject, this.resourceProvider);
            catalog.addAllTablesUsedInQuery(query, this.analyzerOptions);
        }
        JoinOrderVisitor visitor = new JoinOrderVisitor(this.service);
        if(this.visitorMetricsMap.get(visitor.getName()) == null) {
            this.visitorMetricsMap.put(visitor.getName(), 0);
            this.visitorMetricsMap.merge(visitor.getName(), 1, Integer::sum);
        }
        try {
            logger.info("Analyzing query with id: " + inputQuery.getQueryId() +
                    " For anti-pattern:" + visitor.getName());
            Iterator<ResolvedNodes.ResolvedStatement> statementIterator = this.analyzer.analyzeStatements(query, catalog);
            statementIterator.forEachRemaining(statement -> statement.accept(visitor));

            String result = visitor.getResult();
            if (result.length() > 0) {
                visitorsThatFoundAntiPatterns.add(visitor);
            }
        } catch (Exception e) {
            logger.error("Error analyzing query with id: " + inputQuery.getQueryId() +
                    " For anti-pattern:" + visitor.getName());
            logger.error(e.getMessage(), e);
        }
    }

    // THE ORDER HERE MATTERS
    // this is also the order in which the rewrites get applied
    public List<AntiPatternVisitor> getParserVisitorList(String query) {
        return new ArrayList<>(Arrays.asList(
                new IdentifySimpleSelectStarVisitor(),
                new IdentifyInSubqueryWithoutAggVisitor(query),
                new IdentifyDynamicPredicateVisitor(query),
                new IdentifyOrderByWithoutLimitVisitor(query),
                new IdentifyRegexpContainsVisitor(query),
                new IdentifyCTEsEvalMultipleTimesVisitor(query),
                new IdentifyLatestRecordVisitor(query),
                new IdentifyWhereOrderVisitor(query),
                new IdentifyMissingDropStatementVisitor(query),
                new IdentifyDroppedPersistentTableVisitor(query)

        ));
    }

    public String getProject() {
        return project;
    }

    public Boolean getUseAnalizer() {
        return useAnalizer;
    }

    private void setVisitorMetricsMap(List<AntiPatternVisitor> parserVisitorList ) {
        this.visitorMetricsMap = new HashMap<>();
        parserVisitorList.stream().forEach(visitor -> this.visitorMetricsMap.put(visitor.getName(), 0));
    }

    private ZetaSQLToolkitAnalyzer getAnalyzer(AnalyzerOptions options) {
        LanguageOptions languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
        languageOptions.setSupportsAllStatementKinds();
        options.setLanguageOptions(languageOptions);
        options.setCreateNewColumnForEachProjectedOutput(true);
        return new ZetaSQLToolkitAnalyzer(options);
    }
}
