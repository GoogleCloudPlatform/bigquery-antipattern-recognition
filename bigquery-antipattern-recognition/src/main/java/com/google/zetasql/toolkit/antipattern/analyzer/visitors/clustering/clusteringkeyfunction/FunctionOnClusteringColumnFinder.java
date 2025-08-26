package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringkeyfunction;

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
 * Helper visitor to find instances where a clustering key column is used
 * inside a function call, which itself is an argument to a comparison function.
 * Relies on ResolvedColumn.getTableName() providing the source table FQN.
 */
class FunctionOnClusteringColumnFinder extends ResolvedNodes.Visitor {

    private final Map<String, List<String>> clusteringFields; // All known clustered tables/keys
    private final Set<String> findings; // Set of warning messages generated

    // Common comparison functions where cluster pruning might be affected
    private static final Set<String> COMPARISON_FUNCTIONS = new HashSet<>(Arrays.asList(
        "$equal", "$less", "$less_or_equal", "$greater", "$greater_or_equal",
        "$not_equal", "$like", "$in", "$between"
        // Add others if needed
    ));

    FunctionOnClusteringColumnFinder(
            Map<String, List<String>> clusteringFields,
            Set<String> findings) {
        this.clusteringFields = clusteringFields;
        this.findings = findings;
    }

    @Override
    public void visit(ResolvedFunctionCall outerFuncCall) {
        // Is this function a comparison operator we care about?
        if (COMPARISON_FUNCTIONS.contains(outerFuncCall.getFunction().getName())) {

            // Check each argument of the comparison function
            for (ResolvedExpr outerArg : outerFuncCall.getArgumentList()) { // Use getArgumentList()

                // Is this argument *itself* a function call?
                if (outerArg instanceof ResolvedFunctionCall) {
                    ResolvedFunctionCall innerFuncCall = (ResolvedFunctionCall) outerArg;

                    // Check arguments of the inner function call
                    for (ResolvedExpr innerArg : innerFuncCall.getArgumentList()) { // Use getArgumentList()

                        // Is this inner argument a column reference?
                        if (innerArg instanceof ResolvedColumnRef) {
                            ResolvedColumnRef colRef = (ResolvedColumnRef) innerArg;
                            ResolvedColumn column = colRef.getColumn();
                            String colName = column.getName();

                            // *** USE getTableName() DIRECTLY ***
                            String sourceTableName = "`" + column.getTableName() + "`"; // Assumes FQN like project.dataset.table


                            if (sourceTableName != null) {
                                // Check if this source table is clustered
                                List<String> keysForTable = clusteringFields.get(sourceTableName);
                                // Check if this column is one of its keys
                                if (keysForTable != null && keysForTable.contains(colName)) {
                                    // Found the anti-pattern!
                                    String warning = String.format(
                                        "Potential anti-pattern: Function '%s' used on clustering key '%s' of table '%s' within a predicate (%s). " +
                                        "This often prevents cluster pruning. Consider applying functions to constants/parameters instead (e.g., `key = FUNC(value)` instead of `FUNC(key) = value`).",
                                        innerFuncCall.getFunction().getName(),
                                        colName,
                                        sourceTableName, // Use the name from getTableName()
                                        outerFuncCall.getFunction().getName()
                                    );
                                    findings.add(warning);
                                }
                            }
                            // else: getTableName() returned null, handle if necessary
                        }
                    }
                }
            }
        }
        // IMPORTANT: Always continue traversal
        super.visit(outerFuncCall);
    }
}
