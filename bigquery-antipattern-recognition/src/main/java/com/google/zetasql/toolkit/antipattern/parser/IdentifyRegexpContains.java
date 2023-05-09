package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyRegexpContainsVisitor;
import java.util.stream.Collectors;

public class IdentifyRegexpContains implements BasePatternDetector{
    @Override
    public String run(ASTNodes.ASTStatement parsedQuery, String query) {
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        return visitor.getResult().stream().distinct().collect(Collectors.joining("\n"));
    }
}
