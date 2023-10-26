package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.common.collect.ImmutableList;
import com.google.zetasql.parser.ASTNodes;

import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class IdentifyRegexpContainsVisitor extends ParseTreeVisitor implements AntiPatternVisitor {

    public final static String NAME = "StringComparison";
    private final static String REGEXP_CONTAINS = "REGEXP_CONTAINS at line %d. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
    private final static String REGEXP_CONTAINS_STR = "regexp_contains";
    private ArrayList<String> result = new ArrayList<String>();
    private String query;

    public String getResult() {
        return result.stream().distinct().collect(Collectors.joining("\n"));
    }

    public IdentifyRegexpContainsVisitor(String query) {
        this.query = query;
    }

    @Override
    public void visit(ASTNodes.ASTFunctionCall node) {
        ImmutableList<ASTNodes.ASTIdentifier> identifiers = node.getFunction().getNames();

        for (ASTNodes.ASTIdentifier identifier : identifiers) {
            if(identifier.getIdString().equals(REGEXP_CONTAINS_STR)){
                int lineNum = ZetaSQLStringParsingHelper.countLine(query, identifier.getParseLocationRange().start());
                result.add(String.format(REGEXP_CONTAINS, lineNum));
            }
        }

    }

    @Override
    public String getNAME() {
        return NAME;
    }

}


