package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringkeyused; // Or a sub-package

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedColumnRef;
import java.util.Set;

/**
 * A simple visitor that collects the names of all ResolvedColumnRef nodes
 * it encounters within the AST subtree it visits.
 */
class ColumnRefCollector extends ResolvedNodes.Visitor {
    private final Set<String> referencedColumnNames;

    ColumnRefCollector(Set<String> referencedColumnNames) {
        this.referencedColumnNames = referencedColumnNames;
    }

    @Override
    public void visit(ResolvedColumnRef node) {
        // Add the simple column name (without table qualification)
        referencedColumnNames.add(node.getColumn().getName());
        // Don't need to visit children of a column reference
    }
}
