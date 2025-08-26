package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering; // Adjust package if needed

import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.Table; // Using google-cloud-bigquery Table
import com.google.cloud.bigquery.TableDefinition;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedTableScan;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor to identify non-partitioned tables being scanned in a query.
 */
public class ClusteringCheckVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

    public static final String NAME = "Clustering Check";
    // Concise recommendation message
    private static final String RECOMMENDATION_MESSAGE_FORMAT =
            "Table: %s is not clustered. Consider clustering large tables for performance and cost optimization.";

    private final BigQueryService service;
    // Use a Set to store names of tables found to be unclustered (avoids duplicates)
    private boolean containsUnclusteredTables = false;
    private final Set<String> unclusteredTablesFound = new HashSet<>();
    private HashMap<String, List<String>> clusteringFields = new HashMap<>();

    public ClusteringCheckVisitor(BigQueryService service) {
        this.service = service;
    }

    public HashMap<String, List<String>> getClustering() {
        return clusteringFields;
      }

      public boolean getContainsUnclusteredTables() {
        return containsUnclusteredTables;
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
        if (unclusteredTablesFound.isEmpty()) {
            return "";
        }
        // Format the result, sorting for consistent output
        return unclusteredTablesFound.stream()
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

              // Check clustering only for Standard Tables
              if (definition instanceof StandardTableDefinition) {
                StandardTableDefinition stdDef = (StandardTableDefinition) definition;
                // Check if clustering is defined by seeing if the Clustering object exists
                boolean isClustered = stdDef.getClustering() != null;

                if (!isClustered) {
                    // Store the fully qualified name if table is not clustered
                    // Assumes you rename unclusteredTablesFound -> unclusteredTablesFound
                    unclusteredTablesFound.add(actualTableName);
                    containsUnclusteredTables = true;
                }
                else {
                    Clustering clustering = stdDef.getClustering();
                    //System.out.println(clustering.getFields());
                    clusteringFields.put(actualTableName, clustering.getFields());
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
