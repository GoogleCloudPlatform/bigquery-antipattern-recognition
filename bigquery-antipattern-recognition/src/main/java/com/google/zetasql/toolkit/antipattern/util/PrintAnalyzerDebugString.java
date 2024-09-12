package com.google.zetasql.toolkit.antipattern.util;

import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryAPIResourceProvider;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import java.util.Iterator;
import org.apache.commons.cli.ParseException;

public class PrintAnalyzerDebugString {

  public static void main(String[] args) throws ParseException {
    String processingProject = "bigquery-public-data";

    // setup analyzer
    AnalyzerOptions options = new AnalyzerOptions();
    LanguageOptions languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    options.setLanguageOptions(languageOptions);
    options.setCreateNewColumnForEachProjectedOutput(true);
    ZetaSQLToolkitAnalyzer analyzer = new ZetaSQLToolkitAnalyzer(options);

    // create BQ catalog
    BigQueryService service = BigQueryService.buildDefault();
    BigQueryAPIResourceProvider resourceProvider = BigQueryAPIResourceProvider.build(service);
    BigQueryCatalog catalog = new BigQueryCatalog(processingProject, resourceProvider);

    //what about a view
    String query = "CREATE TABLE mydataset.example \n"
        + "(\n"
        +  "x INT64, \n"
        +  "y STRING \n"
        + "); \n"
        + "CREATE TEMP TABLE my_temp_table AS \n"
        + "SELECT  \n"
        + "  product_id, \n"
        + "  SUM(quantity) AS total_quantity_sold\n"
        + "FROM\n"
        + "  UNNEST([ \n"
        + "      STRUCT(1 AS product_id, 78 AS quantity),\n"
        + "      STRUCT(2 AS product_id, 18 AS quantity),\n"
        + "      STRUCT(3 AS product_id, 95 AS quantity),\n"
        + "      STRUCT(4 AS product_id, 67 AS quantity),\n"
        + "      STRUCT(5 AS product_id, 50 AS quantity)\n"
        + "  ]) \n"
        + "GROUP BY \n"
        + "  product_id; \n"
        + "DROP TABLE Example; \n"

        + "SELECT * \n"
        + "FROM \n"
        + "  my_temp_table \n"
        + "WHERE \n"
        + "  total_quantity_sold > 100;\n\n"

        + "SELECT  \n"
        + "  t1.station_id,\n"
        + "  COUNT(1) num_trips_started\n"
        + "FROM\n"
        + "  `bigquery-public-data.austin_bikeshare.bikeshare_stations` t1\n"
        + "JOIN\n"
        + "  `bigquery-public-data.austin_bikeshare.bikeshare_trips` t2 ON t1.station_id = t2.start_station_id\n"
        + "GROUP BY\n"
        + "  t1.station_id\n"
        + ";";

    catalog.addAllTablesUsedInQuery(query, options);
    Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);
    statementIterator.forEachRemaining(System.out::println);

  }
}
