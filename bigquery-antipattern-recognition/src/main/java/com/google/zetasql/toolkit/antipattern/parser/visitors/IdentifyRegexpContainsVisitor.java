package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.common.collect.ImmutableList;
import com.google.zetasql.parser.ASTNodes;

import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IdentifyRegexpContainsVisitor extends ParseTreeVisitor implements AntiPatternVisitor {

    public final static String NAME = "StringComparison";
    private final static String REGEXP_CONTAINS_ANTI_PATTERN_MESSAGE = "REGEXP_CONTAINS at line %d. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
    private final static String REGEXP_CONTAINS_FUN_ID_STR = "regexp_contains";
    private final static String REGEX_STRING = "['\"]\\.\\*.*\\.\\*['\"]";
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
            // search for regexp_contains
            if(identifier.getIdString().toLowerCase().equals(REGEXP_CONTAINS_FUN_ID_STR)){
                ImmutableList<ASTNodes.ASTExpression> arguments = node.getArguments();
                // search for argument with string
                for(ASTNodes.ASTExpression argument: arguments) {
                    if(argument instanceof ASTNodes.ASTStringLiteral){
                        String stringLiteralArg = ((ASTNodes.ASTStringLiteral) argument).getImage();
                        Pattern pattern = Pattern.compile(REGEX_STRING);
                        Matcher matcher = pattern.matcher(stringLiteralArg);
                        if(matcher.find()) {
                            int lineNum = ZetaSQLStringParsingHelper.countLine(query, identifier.getParseLocationRange().start());
                            result.add(String.format(REGEXP_CONTAINS_ANTI_PATTERN_MESSAGE, lineNum));
                        }
                    }
                }
            }
        }

    }

    @Override
    public String getNAME() {
        return NAME;
    }

}


