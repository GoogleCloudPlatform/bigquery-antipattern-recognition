package com.google.zetasql.toolkit.antipattern.analyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.PartitionCheckVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyDynamicPredicateVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import com.google.zetasql.resolvedast.ResolvedNodes;

import java.util.Iterator;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import com.google.zetasql.LanguageOptions;


public class IdentifyMissingPartitioningTest {
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
    public void SimpleTableisNotPartitioned() {
      String expected = "Table: bigquery-public-data.austin_bikeshare.bikeshare_stations is not partitioned. Consider partitioning large tables for performance and cost optimization.";
      String query = "SELECT  \n"
      + "station_id,\n"
      + "FROM\n"
      + "  `bigquery-public-data.austin_bikeshare.bikeshare_stations`\n"
      + ";";
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);
      PartitionCheckVisitor visitor = new PartitionCheckVisitor(service);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      String recommendation = visitor.getResult();
      assertEquals(expected, recommendation);
    }

    @Test
    public void SimpleTableisPartitioned() {
      String expected = "";
      String query = "SELECT  \n"
      + "block_hash\n"
      + "FROM\n"
      + "  `bigquery-public-data.crypto_bitcoin.transactions`\n"
      + ";";
      catalog.addAllTablesUsedInQuery(query, analyzerOptions);
      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);
      PartitionCheckVisitor visitor = new PartitionCheckVisitor(service);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      String recommendation = visitor.getResult();
      assertEquals(expected, recommendation);
    }
}
