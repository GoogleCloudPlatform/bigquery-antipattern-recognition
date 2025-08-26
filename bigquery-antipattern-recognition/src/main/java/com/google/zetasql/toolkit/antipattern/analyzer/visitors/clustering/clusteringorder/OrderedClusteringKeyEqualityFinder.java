// --- Helper Visitor (Revised for Order) ---
package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringorder;

import com.google.zetasql.resolvedast.ResolvedColumn;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedColumnRef;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedExpr;
import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFunctionCall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper visitor to find clustering key columns used in equality predicates ($eq)
 * and return them IN THE ORDER they appear as arguments to top-level ANDs
 * within the visited expression tree.
 * Relies on ResolvedColumn.getTableName() providing the FQN of the source table.
 */
class OrderedClusteringKeyEqualityFinder extends ResolvedNodes.Visitor {

    private final String targetTableName; // Expected format: project.dataset.table
    private final List<String> clusteringKeys; // All keys for the target table
    // Output list - preserves order found in $and arguments
    private final List<String> orderedFoundEqualityKeys;

    private static final String EQ_FUNCTION = "$equal";
    private static final String AND_FUNCTION = "$and";

    OrderedClusteringKeyEqualityFinder(String targetTableName, List<String> clusteringKeys, List<String> orderedFoundEqualityKeys) {
        this.targetTableName = targetTableName;
        this.clusteringKeys = clusteringKeys;
        this.orderedFoundEqualityKeys = orderedFoundEqualityKeys; // Use the list passed in
    }

    @Override
    public void visit(ResolvedFunctionCall functionCall) {
        String funcName = functionCall.getFunction().getName();

        if (funcName.equals(AND_FUNCTION)) {
            // If it's an AND, visit arguments in order. The recursive call to accept
            // will trigger visitResolvedFunctionCall again for nested $eq or $and.
             for (ResolvedExpr arg : functionCall.getArgumentList()) {
                 arg.accept(this);
             }
             // We don't call super here because we explicitly visited children.

        } else if (funcName.equals(EQ_FUNCTION)) {
            // If it's an equality check, see if it involves a target clustering key
            ResolvedColumnRef colRef = null;
            for (ResolvedExpr arg : functionCall.getArgumentList()) {
                if (arg instanceof ResolvedColumnRef) {
                    colRef = (ResolvedColumnRef) arg;
                    break; // Found the column ref argument
                }
            }

            if (colRef != null) {
                ResolvedColumn column = colRef.getColumn();
                String colName = column.getName();
                String sourceTableName = column.getTableName(); // Assumes FQN

                // Check if it's the target table and one of its clustering keys
                if (targetTableName.equals(sourceTableName) && clusteringKeys.contains(colName)) {
                     // Add to the ORDERED list
                     // Avoid adding duplicates if the same key is somehow in multiple predicates
                     if(!orderedFoundEqualityKeys.contains(colName)){
                         orderedFoundEqualityKeys.add(colName);
                     }
                }
            }
             // Don't call super for $eq as we've processed it. We don't need to visit its children (ColumnRef, Literal) for *this* purpose.

        } else {
             // For other functions, just continue traversal to find nested ANDs or EQs
             super.visit(functionCall);
        }
    }
    // We might need to override visitResolvedLiteral, visitResolvedColumnRef etc.
    // to *stop* traversal once we are below an $eq node, but the logic above
    // achieves this by not calling super.visit for $eq.
}
