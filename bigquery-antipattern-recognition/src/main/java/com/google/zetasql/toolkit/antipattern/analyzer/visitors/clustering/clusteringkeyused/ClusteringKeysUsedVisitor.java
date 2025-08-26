package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringkeyused; // Adjust package if needed

import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedColumnRef;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedExpr;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedAggregateScan;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFilterScan;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedJoinScan;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor to identify clustered tables whose clustering keys are not found
 * referenced in WHERE, JOIN ON, or GROUP BY clauses.
 * Expects clustering information (table -> keys map) as input.
 */
public class ClusteringKeysUsedVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

    public static final String NAME = "ClusteringKeysUsedCheck";
    private static final String RECOMMENDATION_MESSAGE_FORMAT =
        "Table: %s is clustered by [%s], but these keys were not referenced in WHERE, JOIN ON, or GROUP BY clauses. Clustering might not provide significant benefits for this query.";

    // Input: Map of clustered_table_name -> List<clustering_key_columns>
    private final Map<String, List<String>> clusteredTableKeys;

    // State collected during visit: All unique column names found in relevant clauses
    private final Set<String> referencedColumnNames = new HashSet<>();
    private final ColumnRefCollector columnRefCollector = new ColumnRefCollector(referencedColumnNames);

    // Results calculated after visit
    private final Set<Map.Entry<String, List<String>>> clusteredTablesWithKeysNotUsed = new HashSet<>();
    private boolean analysisDone = false; // Flag to ensure analysis runs only once

    public ClusteringKeysUsedVisitor(Map<String, List<String>> clusteredTableKeys) {
        // Make a defensive copy if the map might be modified elsewhere
        this.clusteredTableKeys = new HashMap<>(clusteredTableKeys);
    }

    @Override
    public String getName() {
        return NAME;
    }

    // Helper to run the collector on an expression
    private void findReferencedColumnsIn(ResolvedExpr expression) {
        if (expression != null) {
            expression.accept(columnRefCollector);
        }
    }

    /**
     * Visits FilterScan nodes to check columns used in WHERE clauses.
     */
    @Override
    public void visit(ResolvedFilterScan node) {
        findReferencedColumnsIn(node.getFilterExpr());
        // Continue traversal to find other relevant nodes deeper in the tree
        super.visit(node);
    }

    /**
     * Visits JoinScan nodes to check columns used in JOIN ON clauses.
     */
    @Override
    public void visit(ResolvedJoinScan node) {
        findReferencedColumnsIn(node.getJoinExpr());
        // Continue traversal
        super.visit(node);
    }

    /**
     * Visits AggregateScan nodes to check columns used in GROUP BY clauses.
     */
    @Override
    public void visit(ResolvedAggregateScan node) {
        // Directly collect column names from GROUP BY list refs
        if (node.getGroupByList() != null) {
            for (ResolvedColumnRef groupByColRef : node.getRollupColumnList()) {
                referencedColumnNames.add(groupByColRef.getColumn().getName());
            }
        }
        // Optionally check columns used within aggregate function arguments as well
        // node.getAggregateList().forEach(aggCall -> findReferencedColumnsIn(aggCall.getExpression()));
        // Continue traversal
        super.visit(node);
    }

    /**
     * Performs the analysis after the AST traversal is complete.
     * Compares the known clustering keys against the set of referenced columns.
     * This method MUST be called after the visitor has finished traversing the AST.
     */
    public void analyzeKeyUsage() {
        if (analysisDone) {
            return; // Prevent running analysis multiple times
        }

        for (Map.Entry<String, List<String>> entry : clusteredTableKeys.entrySet()) {
            List<String> keys = entry.getValue();
            boolean keyReferenced = false;
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    if (referencedColumnNames.contains(key)) {
                        keyReferenced = true;
                        break; // Found at least one key used for this table
                    }
                }
                // If we iterated through all keys and found none referenced
                if (!keyReferenced) {
                    clusteredTablesWithKeysNotUsed.add(entry);
                }
            }
        }
        analysisDone = true;
    }

    /**
     * Returns the result string ONLY after analyzeKeyUsage() has been called.
     * It formats the recommendations for tables whose clustering keys were not used.
     */
    @Override
    public String getResult() {
        // Crucially, the analysis MUST have been run before generating the result.
        if (!analysisDone) {
            // Option 1: Run analysis now (might be unexpected if called prematurely)
             analyzeKeyUsage();
            // Option 2: Return empty or throw error (safer if caller manages lifecycle)
            // return ""; // Or throw new IllegalStateException("analyzeKeyUsage() must be called before getResult()");
        }

        if (clusteredTablesWithKeysNotUsed.isEmpty()) {
            return "";
        }

        // Format the result string
        return clusteredTablesWithKeysNotUsed.stream()
            .sorted(Map.Entry.comparingByKey()) // Sort by table name for consistent output
            .map(entry -> String.format(RECOMMENDATION_MESSAGE_FORMAT,
                                        entry.getKey(), // Table name
                                        String.join(", ", entry.getValue()) // List of clustering keys
                                       ))
            .collect(Collectors.joining("\n"));
    }
}
