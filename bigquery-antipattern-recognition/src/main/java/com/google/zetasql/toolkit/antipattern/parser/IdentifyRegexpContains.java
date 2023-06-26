package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.toolkit.antipattern.Recommendation;
import com.google.zetasql.toolkit.antipattern.RecommendationType;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyRegexpContainsVisitor;
import java.util.Optional;
import java.util.stream.Collectors;

public class IdentifyRegexpContains implements BasePatternDetector{
    @Override
    public Optional<Recommendation> run(ASTNodes.ASTStatement parsedQuery, String query) {
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);

        String description =
            visitor.getResult().stream().distinct().collect(Collectors.joining("\n"));

        Recommendation recommendation =
            new Recommendation(RecommendationType.StringComparison, description);

        return description.isEmpty() ? Optional.empty() : Optional.of(recommendation);
    }
}
