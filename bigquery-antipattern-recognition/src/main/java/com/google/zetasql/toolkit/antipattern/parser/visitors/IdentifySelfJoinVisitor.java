package com.google.zetasql.toolkit.antipattern.parser.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.zetasql.parser.ASTNodes.ASTJoin;
import com.google.zetasql.parser.ASTNodes.ASTTablePathExpression;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;

public class IdentifySelfJoinVisitor extends ParseTreeVisitor implements AntiPatternVisitor {

    public static final String NAME = "SelfJoin";
    private final String SELF_JOIN_SUGGESTION_MESSAGE =
            "Self Join detected: Table %s is joined on itself at line %d. Consider window (analytic) function.";

    private final ArrayList<String> result = new ArrayList<>();
    private final Map<String, Integer> selfJoinMap = new HashMap<>();
    private String query;

    public IdentifySelfJoinVisitor(String query) {
        this.query = query;
    }

    @Override
    public void visit(ASTJoin joinExpression) {
      ASTTablePathExpression lhs = (ASTTablePathExpression) joinExpression.getLhs();
      ArrayList<String> lhsTableName = new ArrayList<>();
      lhs.getPathExpr()
      .getNames()
      .forEach(
          identifier -> {
              // Get the identifier as a string in lower case.
              String id = identifier.getIdString().toLowerCase();
              lhsTableName.add(id);
          }
      );
      ASTTablePathExpression rhs = (ASTTablePathExpression) joinExpression.getLhs();
      ArrayList<String> rhsTableName = new ArrayList<>();
      rhs.getPathExpr()
      .getNames()
      .forEach(
          identifier -> {
              // Get the identifier as a string in lower case.
              String id = identifier.getIdString().toLowerCase();
              rhsTableName.add(id);
          }
      );
      if(lhsTableName.containsAll(rhsTableName) && rhsTableName.containsAll(lhsTableName)){
        selfJoinMap.put(String.join(".", lhsTableName), joinExpression.getParseLocationRange().start());
      }
    }

   // Getter method to retrieve the list of suggestion messages.
   public String getResult() {
    for (Map.Entry<String, Integer> entry : selfJoinMap.entrySet()) {
      String tableName = entry.getKey();
      int lineNum = ZetaSQLStringParsingHelper.countLine(query, selfJoinMap.get(tableName));
      result.add(String.format(SELF_JOIN_SUGGESTION_MESSAGE, tableName, lineNum));
    }
    return result.stream().distinct().collect(Collectors.joining("\n"));
  }
    @Override
    public String getName() {
        return NAME;
    }
}
