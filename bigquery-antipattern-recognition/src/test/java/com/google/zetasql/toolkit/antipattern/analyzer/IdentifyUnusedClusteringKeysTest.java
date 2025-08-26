package com.google.zetasql.toolkit.antipattern.analyzer; // Same package? OK.

import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringkeyused.ClusteringKeysUsedVisitor;
import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.LanguageOptions;

import java.util.Arrays; // For List creation
import java.util.HashMap; // For Map creation
import java.util.Iterator;
import java.util.List; // For List creation
import java.util.Map; // For Map creation
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;


public class IdentifyUnusedClusteringKeysTest {

    LanguageOptions languageOptions;
    AnalyzerOptions analyzerOptions;
    ZetaSQLToolkitAnalyzer zetaSQLToolkitAnalyzer;
    BigQueryService service; // May not be needed directly if service calls are mocked/avoided
    BigQueryAPIResourceProvider resourceProvider;
    BigQueryCatalog catalog;
    String PUBLIC_TESTING_PROJECT = "bigquery-public-data";
    String PUBLIC_CLUSTERED_TABLE = "`bigquery-public-data.wikipedia.pageviews_2025`";
    List<String> PUBLIC_CLUSTERING_COLUMNS = Arrays.asList("wiki", "title");

    @Before
    public void setUp() {
      languageOptions = new LanguageOptions();
      languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
      languageOptions.setSupportsAllStatementKinds();
      analyzerOptions = new AnalyzerOptions();
      analyzerOptions.setLanguageOptions(languageOptions);
      zetaSQLToolkitAnalyzer = new ZetaSQLToolkitAnalyzer(analyzerOptions);
      service = BigQueryService.buildDefault();
      resourceProvider = BigQueryAPIResourceProvider.build(service);

      catalog = new BigQueryCatalog(PUBLIC_TESTING_PROJECT, resourceProvider);
      catalog.addAllTablesUsedInQuery("SELECT wiki FROM " + PUBLIC_CLUSTERED_TABLE + " LIMIT 0", analyzerOptions);
    }

    private Map<String, List<String>> getClusteringInfoForTable() {
        Map<String, List<String>> clusteringInfo = new HashMap<>();
        clusteringInfo.put(PUBLIC_CLUSTERED_TABLE.replaceAll("`", ""), PUBLIC_CLUSTERING_COLUMNS);
        return clusteringInfo;
    }

    @Test
    public void clusteringKeysUsedInWhere() {
      String expected = "";
      String query = "SELECT wiki "
          + "FROM " + PUBLIC_CLUSTERED_TABLE + "\n"
          + "WHERE wiki = 'sf'";

      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);

      Map<String, List<String>> clusteringInfo = getClusteringInfoForTable();
      ClusteringKeysUsedVisitor visitor = new ClusteringKeysUsedVisitor(clusteringInfo);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      visitor.analyzeKeyUsage(); // Analyze after visiting
      String recommendation = visitor.getResult();

      assertEquals(expected, recommendation);
    }

    @Test
    public void clusteringKeysNotUsedInWhere() {
      String expected = "Table: bigquery-public-data.wikipedia.pageviews_2025 is clustered by [wiki, title], but these keys were not referenced in WHERE, JOIN ON, or GROUP BY clauses. Clustering might not provide significant benefits for this query.";
      String query = "SELECT wiki "
          + "FROM " + PUBLIC_CLUSTERED_TABLE + "\n"
          + "WHERE views = 1";

      Iterator<ResolvedNodes.ResolvedStatement> statementIterator = zetaSQLToolkitAnalyzer.analyzeStatements(query, catalog);

      Map<String, List<String>> clusteringInfo = getClusteringInfoForTable();
      ClusteringKeysUsedVisitor visitor = new ClusteringKeysUsedVisitor(clusteringInfo);
      statementIterator.forEachRemaining(statement -> statement.accept(visitor));
      visitor.analyzeKeyUsage(); // Analyze after visiting
      String recommendation = visitor.getResult();
      System.out.println(recommendation);

      assertEquals(expected, recommendation);
    }

}
