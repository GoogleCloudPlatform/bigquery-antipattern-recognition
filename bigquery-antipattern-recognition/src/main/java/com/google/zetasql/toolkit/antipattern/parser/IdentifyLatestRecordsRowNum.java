package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyLatestRecordRowNumVisitor;
import java.util.stream.Collectors;
public class IdentifyLatestRecordsRowNum implements BasePatternDetector {
    @Override
    public String run(ASTNodes.ASTStatement parsedQuery, String query) {
        IdentifyLatestRecordRowNumVisitor visitor = new IdentifyLatestRecordRowNumVisitor(query);
        parsedQuery.accept(visitor);
        return visitor.getResult().stream().distinct().collect(Collectors.joining("\n"));
    }
}
