package com.google.zetasql.toolkit.antipattern.analyzer;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.ClusteringCheckVisitor;
import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import com.google.zetasql.resolvedast.ResolvedNodes;

import java.util.Iterator;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import com.google.zetasql.LanguageOptions;


public class IdentifyMissingClusteringTest {
    LanguageOptions languageOptions;
    AnalyzerOptions analyzerOptions;
    ZetaSQLToolkitAnalyzer zetaSQLToolkitAnalyzer;
    BigQueryService service;
    BigQueryAPIResourceProvider resourceProvider;
    BigQueryCatalog catalog;
    String TESTING_PROJECT = "bigquery-public-data";

    @Before
    public void setUp() {
      languageOptions = new LanguageOptions();
      languageOptions.enableMaximumLanguageFeatures();
      languageOptions.setSupportsAllStatementKinds();
      analyzerOptions = new AnalyzerOptions();
      analyzerOptions.setLanguageOptions(languageOptions);
      zetaSQLToolkitAnalyzer = new ZetaSQLToolkitAnalyzer(analyzerOptions);
      analyzerOptions.setCreateNewColumnForEachProjectedOutput(true);
      service = BigQueryService.buildDefault();
      resourceProvider = BigQueryAPIResourceProvider.build(service);
      catalog = new BigQueryCatalog(TESTING_PROJECT, resourceProvider);
    }

    @Test
    public void SimpleTableisNotClustered() {
      String expected = "Table: bigquery-public-data.crypto_bitcoin.transactions is not clustered. Consider clustering large tables for performance and cost optimization.";
      String query = "SELECT  \n"
      + "block_hash\n"
      + "FROM\n"
      + "  `bigquery-public-data.crypto_bitcoin.transactions`\n"
      + ";";
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);
      ClusteringCheckVisitor visitor = new ClusteringCheckVisitor(service);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      String recommendation = visitor.getResult();
      assertEquals(expected, recommendation);
    }

    @Test
    public void SimpleTableisClustered() {
      String expected = "";
      String query = "SELECT  \n"
      + "wiki,\n"
      + "FROM\n"
      + "  `bigquery-public-data.wikipedia.pageviews_2025`\n"
      + ";";
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);
      ClusteringCheckVisitor visitor = new ClusteringCheckVisitor(service);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      String recommendation = visitor.getResult();
      assertEquals(expected, recommendation);
    }

    @Test
    public void SimpleTableisNotClusteredTwoDifferentProjects() {
      String expected = "Table: afleisc-udf-test.test.flow_stats is not clustered. Consider clustering large tables for performance and cost optimization.\n"
      +"Table: bigquery-public-data.crypto_bitcoin.transactions is not clustered. Consider clustering large tables for performance and cost optimization.";
      String query = "SELECT  \n"
      + "t1.block_hash FROM \n"
      + "`bigquery-public-data.crypto_bitcoin.transactions` t1 \n"
      + "JOIN `afleisc-udf-test.test.flow_stats` t2 ON t1.block_hash=t2.block_hash \n"
      + "WHERE t1.block_timestamp_month = DATE('2009-01-01');";
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);
      ClusteringCheckVisitor visitor = new ClusteringCheckVisitor(service);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      String recommendation = visitor.getResult();
      assertEquals(expected, recommendation);
    }
}
