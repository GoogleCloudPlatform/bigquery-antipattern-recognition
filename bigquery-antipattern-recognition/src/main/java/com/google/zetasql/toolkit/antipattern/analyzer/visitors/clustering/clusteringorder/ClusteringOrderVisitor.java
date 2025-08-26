package com.google.zetasql.toolkit.antipattern.analyzer.visitors.clustering.clusteringorder; // Adjust package if needed


import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFilterScan;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedTableScan;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Visitor to check if filter predicates on clustering keys (using equality)
 * appear in the same order as the defined clustering order.
 * Requires a map of clustered tables to their ordered keys as input.
 * Assumes ResolvedColumn.getTableName() works reliably.
 */
public class ClusteringOrderVisitor extends ResolvedNodes.Visitor implements AntiPatternVisitor {

    public static final String NAME = "ClusteringOrderCheck";
    // Updated message to reflect order issue
    private static final String RECOMMENDATION_MESSAGE_FORMAT =
        "Table: %s is clustered by [%s]. Filters were found on clustering keys in the order [%s]. " +
        "This differs from the defined clustering order. For optimal cluster pruning, " +
        "filter predicates using equality should reference keys sequentially (e.g., filter '%s' first, then '%s', etc.).";

    private final Map<String, List<String>> clusteringFields; // Input: table_name -> ordered keys
    private final Map<String, String> findings = new HashMap<>(); // Output: table_name -> warning message

    public ClusteringOrderVisitor(Map<String, List<String>> clusteringFields) {
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
        return findings.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.joining("\n"));
    }

    @Override
    public void visit(ResolvedFilterScan node) {
        String targetTableName = null;
        List<String> definedOrderedKeys = null;


        // 1. Identify Input Table and its defined clustering order
        if (node.getInputScan() instanceof ResolvedTableScan) {
            ResolvedTableScan inputTableScan = (ResolvedTableScan) node.getInputScan();
            targetTableName = inputTableScan.getTable().getFullName();
            definedOrderedKeys = clusteringFields.get(targetTableName);
        }

        System.out.println("+++++++");
        System.out.println(definedOrderedKeys);

        // 2. Proceed only if it's a single table scan with known, non-empty clustering keys
        if (targetTableName != null && definedOrderedKeys != null && !definedOrderedKeys.isEmpty()) {

            // 3. Find the ORDERED list of clustering keys used in equality predicates
            List<String> filteredKeysInOrder = new ArrayList<>();
            OrderedClusteringKeyEqualityFinder keyFinder =
                new OrderedClusteringKeyEqualityFinder(targetTableName, definedOrderedKeys, filteredKeysInOrder);
            if (node.getFilterExpr() != null) {
                node.getFilterExpr().accept(keyFinder);
            }
            // 4. Compare the order of filtered keys to the defined prefix order
            if (!filteredKeysInOrder.isEmpty()) {
                boolean orderMatchesPrefix = true;
                // Check if the filtered list matches the start of the defined list
                if (filteredKeysInOrder.size() > definedOrderedKeys.size()) {
                     // This shouldn't happen if helper logic is correct, but indicates an issue.
                     orderMatchesPrefix = false;
                } else {
                    for (int i = 0; i < filteredKeysInOrder.size(); i++) {
                        if (!filteredKeysInOrder.get(i).equals(definedOrderedKeys.get(i))) {
                            orderMatchesPrefix = false;
                            break;
                        }
                    }
                }

                // 5. Generate warning if order doesn't match prefix and not already reported
                if (!orderMatchesPrefix && !findings.containsKey(targetTableName)) {
                    String warning = String.format(
                        RECOMMENDATION_MESSAGE_FORMAT,
                        targetTableName,                                    // %s: Table name
                        String.join(", ", definedOrderedKeys),              // %s: Defined clustering order
                        String.join(", ", filteredKeysInOrder),             // %s: Order found in filter
                        definedOrderedKeys.get(0),                          // %s: Suggest starting with the first key
                        definedOrderedKeys.size() > 1 ? definedOrderedKeys.get(1) : "" // Suggest the second if it exists
                    );
                    findings.put(targetTableName, warning);
                }
            }
        }

        // Continue traversal
        super.visit(node);
    }
}
