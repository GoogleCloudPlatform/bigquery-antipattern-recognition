package com.google.zetasql.toolkit.antipattern.analyzer;

import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedStatement;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder.JoinOrderVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;
import java.util.ArrayList;
import java.util.Iterator;

public class IdentifyJoinOrder {

  private ArrayList<String> result = new ArrayList<String>();
  private static final String SELECTED_COLUMNS_MESSAGE =
      "All columns on table: %s are being selected. Please be sure that all columns are needed";

  public String run(String query, BigQueryCatalog catalog, ZetaSQLToolkitAnalyzer analyzer, BigQueryService service) {
    JoinOrderVisitor visitor = new JoinOrderVisitor(service);

    Iterator<ResolvedStatement> statementIterator = analyzer.analyzeStatements(query, catalog);
    statementIterator.forEachRemaining(statement -> statement.accept(visitor));
    return visitor.getResult();

  }
}