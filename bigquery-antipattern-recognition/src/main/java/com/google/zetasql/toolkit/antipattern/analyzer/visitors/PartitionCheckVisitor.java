package com.google.zetasql.toolkit.antipattern.analyzer.visitors; // Adjust package if needed

import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.Table; // Using google-cloud-bigquery Table
import com.google.cloud.bigquery.TableDefinition;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedTableScan;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor to identify non-partitioned tables being scanned in a query.
 */
public class PartitionCheckVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

    public static final String NAME = "PartitionCheck";
    // Concise recommendation message
    private static final String RECOMMENDATION_MESSAGE_FORMAT =
            "Table: %s is not partitioned. Consider partitioning large tables for performance and cost optimization.";

    private final BigQueryService service;
    // Use a Set to store names of tables found to be unpartitioned (avoids duplicates)
    private final Set<String> unpartitionedTablesFound = new HashSet<>();

    public PartitionCheckVisitor(BigQueryService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Returns a string containing recommendations for tables found without partitioning,
     * or an empty string if none were found or an error occurred.
     */
    @Override
    public String getResult() {
        if (unpartitionedTablesFound.isEmpty()) {
            return "";
        }
        // Format the result, sorting for consistent output
        return unpartitionedTablesFound.stream()
                .sorted()
                .map(tableName -> String.format(RECOMMENDATION_MESSAGE_FORMAT, tableName))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Visits ResolvedTableScan nodes to check the partitioning status of the scanned table.
     * @param node The ResolvedTableScan node from the ZetaSQL AST.
     */
    @Override
    public void visit (ResolvedTableScan node) {
        // Get table name reference from the AST node (e.g., "project.dataset.table" or "dataset.table")
        String tableFullNameFromAST = node.getTable().getFullName();

        try {
            // Fetch table metadata using the service. Assumes service handles null projectId.
            // Assumes service returns Result<com.google.cloud.bigquery.Table>

            Table bigQueryTable = service.fetchTable(null, tableFullNameFromAST).get(); // Uses .get() like reference visitor

            if (bigQueryTable != null) {
              TableDefinition definition = bigQueryTable.getDefinition();
              // Use the fully qualified name returned by the API for reporting
              String actualTableName = String.format("%s.%s.%s",
                                                  bigQueryTable.getTableId().getProject(),
                                                  bigQueryTable.getTableId().getDataset(),
                                                  bigQueryTable.getTableId().getTable());

              // Check partitioning only for Standard Tables
              if (definition instanceof StandardTableDefinition) {
                  StandardTableDefinition stdDef = (StandardTableDefinition) definition;
                  boolean isPartitioned = stdDef.getTimePartitioning() != null ||
                                          stdDef.getRangePartitioning() != null;

                  if (!isPartitioned) {
                      // Store the fully qualified name if table is not partitioned
                      unpartitionedTablesFound.add(actualTableName);
                  }
              }
            }

            // Ignore failures (table not found, API errors) silently for conciseness
        } catch (Exception e) {
            // Ignore all exceptions during fetch/check silently for conciseness
            // WARNING: In production code, logging or handling exceptions is recommended.
        }
    }
}
