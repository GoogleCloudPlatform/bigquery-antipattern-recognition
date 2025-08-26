package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringkeyfunction;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFilterScan;
// Import other necessary Resolved nodes if checking JOINs etc.
// import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedJoinScan;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor to identify cases where functions are applied to clustering key columns
 * within filter predicates, potentially hindering cluster pruning.
 * Requires clustering info as input. Assumes ResolvedColumn.getTableName() works.
 */
public class ClusteringKeyFunctionVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

    public static final String NAME = "FunctionOnClusteringKeyCheck";

    private final Map<String, List<String>> clusteringFields; // Input
    private final Set<String> findings = new HashSet<>(); // Output

    public ClusteringKeyFunctionVisitor(
            Map<String, List<String>> clusteringFields) {
        // Defensive copy might be good
        this.clusteringFields = new HashMap<>(clusteringFields);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getResult() {
        if (findings.isEmpty()) {
            return "";
        }
        return findings.stream().sorted().collect(Collectors.joining("\n"));
    }

    /**
     * Visits ResolvedFilterScan nodes and uses a helper visitor to analyze
     * the filter expression, relying on ResolvedColumn.getTableName().
     */
    @Override
    public void visit(ResolvedFilterScan node) {
        // Analyze the filter expression universally using the helper.
        // The helper now gets the table name directly from the column object.
        if (node.getFilterExpr() != null) {
            // Helper visitor just needs the clustering map and the findings set now
            FunctionOnClusteringColumnFinder functionFinder =
                new FunctionOnClusteringColumnFinder(clusteringFields, findings);
            node.getFilterExpr().accept(functionFinder);
        }

        // Continue traversal to find ALL filter scans in the tree
        super.visit(node);
    }

    // Add overrides for visitResolvedJoinScan etc. if desired, using the same helper
    // to check join expressions.
}
