package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringonclustering;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFilterScan;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedJoinScan; // Import if checking joins
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor to identify direct comparisons between two clustering key columns
 * within filter or join predicates.
 * Requires clustering info as input. Assumes ResolvedColumn.getTableName() works.
 */
public class ClusterColComparisonVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

    public static final String NAME = "ClusteringColumnComparisonCheck";

    private final Map<String, List<String>> clusteringFields; // Input
    private final Set<String> findings = new HashSet<>(); // Output

    public ClusterColComparisonVisitor(
            Map<String, List<String>> clusteringFields) {
        // Defensive copy
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
        // Sort findings for consistent output
        return findings.stream().sorted().collect(Collectors.joining("\n"));
    }

    /**
     * Visits ResolvedFilterScan nodes and uses a helper visitor to analyze
     * the filter expression for cluster key comparisons.
     */
    @Override
    public void visit(ResolvedFilterScan node) {
        if (node.getFilterExpr() != null) {
            ClusterKeyComparisonFinder comparisonFinder =
                new ClusterKeyComparisonFinder(clusteringFields, findings);
            node.getFilterExpr().accept(comparisonFinder);
        }
        // Continue traversal
        super.visit(node);
    }

    /**
     * Visits ResolvedJoinScan nodes and uses a helper visitor to analyze
     * the join expression for cluster key comparisons.
     */
    // @Override
    // public void visit(ResolvedJoinScan node) {
    //      if (node.getJoinExpr() != null) {
    //         ClusterKeyComparisonFinder comparisonFinder =
    //             new ClusterKeyComparisonFinder(clusteringFields, findings);
    //         node.getJoinExpr().accept(comparisonFinder);
    //     }
    //     // Continue traversal
    //     super.visit(node);
    // }
}
