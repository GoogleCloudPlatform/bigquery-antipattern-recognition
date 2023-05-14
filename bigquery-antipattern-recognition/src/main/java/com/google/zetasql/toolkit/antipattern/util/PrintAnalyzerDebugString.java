package com.google.zetasql.toolkit.antipattern.util;

import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.IdentifyJoinOrder;
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
    String query = "SELECT  \n"
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
    //statementIterator.forEachRemaining(System.out::println);
    System.out.println((new IdentifyJoinOrder()).run(query, catalog, analyzer, service));

  }
}
