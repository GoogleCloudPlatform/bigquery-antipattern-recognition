package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringonclustering;

import com.google.zetasql.resolvedast.ResolvedColumn;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedColumnRef;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedExpr;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFunctionCall;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper visitor to find comparisons between two columns where both columns
 * are clustering keys (potentially from different tables).
 * Relies on ResolvedColumn.getTableName() providing the FQN of the source table.
 */
class ClusterKeyComparisonFinder extends ResolvedNodes.Visitor {

    private final Map<String, List<String>> clusteringFields; // All known clustered tables/keys
    private final Set<String> findings; // Set of warning messages generated

    // Common comparison functions
    private static final Set<String> COMPARISON_FUNCTIONS = new HashSet<>(Arrays.asList(
        "$equal", "$less", "$less_or_equal", "$greater", "$greater_or_equal",
        "$not_equal" // Comparing clustering keys for inequality is also usually inefficient
        // Add others? $like probably less common between two keys.
    ));

    ClusterKeyComparisonFinder(
            Map<String, List<String>> clusteringFields,
            Set<String> findings) {
        this.clusteringFields = clusteringFields;
        this.findings = findings;
    }

    @Override
    public void visit(ResolvedFunctionCall funcCall) {
        // Check if it's a comparison function with exactly two arguments
        if (COMPARISON_FUNCTIONS.contains(funcCall.getFunction().getName()) &&
            funcCall.getArgumentList().size() == 2) {

            ResolvedExpr arg1 = funcCall.getArgumentList().get(0);
            ResolvedExpr arg2 = funcCall.getArgumentList().get(1);

            // Check if BOTH arguments are column references
            if (arg1 instanceof ResolvedColumnRef && arg2 instanceof ResolvedColumnRef) {
                ResolvedColumnRef colRef1 = (ResolvedColumnRef) arg1;
                ResolvedColumnRef colRef2 = (ResolvedColumnRef) arg2;

                ResolvedColumn col1 = colRef1.getColumn();
                ResolvedColumn col2 = colRef2.getColumn();

                String colName1 = col1.getName();
                String colName2 = col2.getName();

                // Use getTableName() directly as confirmed by user
                String tableName1 = col1.getTableName();
                String tableName2 = col2.getTableName();

                if (tableName1 != null && tableName2 != null) {
                    // Check if table1 has clustering keys defined
                    List<String> keys1 = clusteringFields.get(tableName1);
                    boolean col1IsClusteringKey = (keys1 != null && keys1.contains(colName1));

                    // Check if table2 has clustering keys defined
                    List<String> keys2 = clusteringFields.get(tableName2);
                    boolean col2IsClusteringKey = (keys2 != null && keys2.contains(colName2));

                    // If BOTH are clustering keys, it's the anti-pattern
                    if (col1IsClusteringKey && col2IsClusteringKey) {
                        String warning = String.format(
                            "Potential anti-pattern: Comparison (%s) between two clustering keys: '%s.%s' and '%s.%s'. " +
                            "Comparing clustering keys directly against each other can be inefficient and may hinder cluster pruning.",
                            funcCall.getFunction().getName(), // e.g., $equal
                            tableName1, colName1,
                            tableName2, colName2
                        );
                        findings.add(warning);
                    }
                }
            }
        }
        // Continue traversal for nested expressions
        super.visit(funcCall);
    }
}
