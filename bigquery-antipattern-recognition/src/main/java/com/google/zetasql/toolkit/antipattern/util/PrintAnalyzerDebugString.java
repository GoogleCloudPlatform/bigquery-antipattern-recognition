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
    String processingProject = "pso-dev-whaite";

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
    String query = "SELECT t1.* \n"
        + "        FROM \n"
        + "         `dataset.tbl1` t1 \n"
        + "        join \n"
        + "         (select * from `pso-dev-whaite.dataset.tbl2`) t2 ON t1.a=t2.a \n"
        + "        join \n"
        + "          `pso-dev-whaite.dataset.tbl3` t3 ON t1.a=t3.a ";

    catalog.addAllTablesUsedInQuery(query, options);
    Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);
    //statementIterator.forEachRemaining(System.out::println);
    System.out.println((new IdentifyJoinOrder()).run(query, catalog, analyzer, service));

  }
}
