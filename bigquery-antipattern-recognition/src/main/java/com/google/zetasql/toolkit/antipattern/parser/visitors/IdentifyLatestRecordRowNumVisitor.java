package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.common.collect.ImmutableList;
import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;

import java.util.*;
import java.util.stream.Collectors;


public class IdentifyLatestRecordRowNumVisitor extends ParseTreeVisitor {
    private final static String LATEST_RECORDS_ROW_NUMBER_PATTERN = "ROW_NUMBER() function used in line : %d, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
    private final static String ROW_NUMBER_STR = "row_number";
    private final ArrayList<String> result = new ArrayList<>();
    private final String query;
    public ArrayList<String> getResult() {
        return result;
    }
    public IdentifyLatestRecordRowNumVisitor(String query) {
        this.query = query;
    }
    String rNoString = "";
    boolean isPattern = false;
    boolean rowNum = false ;
    String rowNumAlias = "";
    int rowNumLineNum = 0;
    List<String> alias_list = new ArrayList<>();


    @Override
    public void visit(ASTNodes.ASTSelect select) {
//        System.out.println("SELECT ==> " + select);
        // Traverse Select node & fetch analyticaFunction node (row_number)
        ImmutableList<ImmutableList<ASTNodes.ASTIdentifier>> astIdentifiers =
                select.getSelectList().getColumns()
                        .stream()
                        .map(ASTNodes.ASTSelectColumn::getExpression)
                        .filter(ASTNodes.ASTAnalyticFunctionCall.class::isInstance)
                        .map(astExpression -> ((ASTNodes.ASTAnalyticFunctionCall) astExpression).getExpression())
                        .filter(ASTNodes.ASTFunctionCall.class::isInstance)
                        .map(x -> ((ASTNodes.ASTFunctionCall) x).getFunction().getNames())
                        .collect(ImmutableList.toImmutableList());
        if(!astIdentifiers.isEmpty()) {
            rNoString = astIdentifiers.get(0).stream()
                    .map(ASTNodes.ASTIdentifier::getIdString)
                    .collect(Collectors.joining());
           if(rNoString.equals(ROW_NUMBER_STR)) {
               rowNum = true;
//               System.out.println("Row Number String ==> " + rNoString);
               //Fetch row number function's line number
               for (ImmutableList<ASTNodes.ASTIdentifier> identifiersList : astIdentifiers) {
                   for(ASTNodes.ASTIdentifier identifier : identifiersList){
                       rowNumLineNum = identifier.getParseLocationRange().start();
                   }
               }
               // Fetch row number alias
               ImmutableList<ASTNodes.ASTSelectColumn> selectColumns = select.getSelectList().getColumns();
               rowNumAlias = selectColumns.stream()
                       .map(ASTNodes.ASTSelectColumn::getAlias)
                       .filter(Objects::nonNull)
                       .map(alias -> alias.getIdentifier().getIdString())
                       .collect(Collectors.joining());
//               System.out.println("ROW NUM ALIAS ==> " + rowNumAlias);
           }
        }
        else if(!rowNum) {
            if(select.getWhereClause() != null) {
                ASTNodes.ASTWhereClause whereClause = select.getWhereClause();
                this.visitWhere(whereClause);
            }
//            System.out.println("Super node => ");
            super.visit(select);
        }
//        System.out.println(alias_list);
        if(alias_list != null ) {
            for (String alias : alias_list) {
//                System.out.println("alias ==> " + alias);
                if (alias.equals(rowNumAlias)) {
                    isPattern = true;
//                    System.out.println("Pattern detected!!");
                    break;
                }
            }
        }
        if (isPattern && rowNum) {
                int lineNum = ZetaSQLStringParsingHelper.countLine(query, rowNumLineNum);
                result.add(String.format(LATEST_RECORDS_ROW_NUMBER_PATTERN, lineNum));
        }
    }

    public void visitWhere(ASTNodes.ASTWhereClause whereClause) {
        ASTNodes.ASTExpression whereClauseExpression = whereClause.getExpression();
        ASTNodes.ASTBinaryExpression binaryExpression ;
        ASTNodes.ASTBetweenExpression betweenExpression ;
        ASTNodes.ASTInExpression inExpression ;
//        System.out.println("WHERE EXPRESSION ==> " + whereClauseExpression);
        if(whereClauseExpression instanceof ASTNodes.ASTAndExpr){
            ImmutableList<ASTNodes.ASTExpression> expression_list = ((ASTNodes.ASTAndExpr) whereClauseExpression).getConjuncts();
            for(ASTNodes.ASTExpression expression : expression_list){
//                System.out.print("LIST of expresison " + expression);
                if(expression instanceof ASTNodes.ASTBinaryExpression) {
                    binaryExpression = (ASTNodes.ASTBinaryExpression) expression;
                    alias_list = this.visitBinaryExpression(binaryExpression); // visit BinaryExpression to find the alias
                } else if(expression instanceof ASTNodes.ASTInExpression) {
                    inExpression = (ASTNodes.ASTInExpression) expression;
                    alias_list = this.visitInExpression(inExpression); // visit InExpression to find the alias
                }
                else if(expression instanceof ASTNodes.ASTBetweenExpression) {
                    betweenExpression = (ASTNodes.ASTBetweenExpression) expression ;
                    alias_list = this.visitBetweenExpression(betweenExpression); // visit BetweenExpression to find the alias
                }
            }
        }
        else if(whereClauseExpression instanceof ASTNodes.ASTInExpression){
            inExpression = (ASTNodes.ASTInExpression) whereClauseExpression;
            alias_list = this.visitInExpression(inExpression);
        }
        else if(whereClauseExpression instanceof ASTNodes.ASTBetweenExpression){
            betweenExpression = ((ASTNodes.ASTBetweenExpression) whereClauseExpression);
            alias_list = this.visitBetweenExpression(betweenExpression);
        }
        else if(whereClauseExpression instanceof ASTNodes.ASTBinaryExpression){
            binaryExpression = (ASTNodes.ASTBinaryExpression) whereClauseExpression;
            alias_list = this.visitBinaryExpression(binaryExpression);
        }
    }

    public List<String> visitInExpression(ASTNodes.ASTInExpression inExpression){
        ASTNodes.ASTPathExpression pathExpression = (ASTNodes.ASTPathExpression) inExpression.getLhs();
        alias_list = getResults(pathExpression, alias_list);
        return alias_list ;
    }

    public List<String> visitBetweenExpression(ASTNodes.ASTBetweenExpression betweenExpression){
        ASTNodes.ASTPathExpression pathExpression = (ASTNodes.ASTPathExpression) betweenExpression.getLhs();
        alias_list = getResults(pathExpression, alias_list);
        return alias_list ;
    }

    public List<String> visitBinaryExpression(ASTNodes.ASTBinaryExpression binaryExpression) {

        // Get where clause of the current node check if condition present or not
        if(binaryExpression.getLhs() instanceof ASTNodes.ASTPathExpression && binaryExpression.getRhs() instanceof ASTNodes.ASTIntLiteral){
            ASTNodes.ASTPathExpression pathExpression = (ASTNodes.ASTPathExpression) binaryExpression.getLhs();
            alias_list = getResults(pathExpression, alias_list);
            return alias_list;
        }
        else if (binaryExpression.getRhs() instanceof ASTNodes.ASTPathExpression && binaryExpression.getLhs() instanceof ASTNodes.ASTIntLiteral)
        {
            ASTNodes.ASTPathExpression pathExpression = (ASTNodes.ASTPathExpression) binaryExpression.getRhs();
            alias_list = getResults(pathExpression, alias_list);
            return alias_list;
        }
        return alias_list;
    }

    public List<String> getResults(ASTNodes.ASTPathExpression pathExpression , List<String> alias_list){
        ImmutableList<ASTNodes.ASTIdentifier> nameId = pathExpression.getNames();
        String rnoAlias = "";
        for(ASTNodes.ASTIdentifier idString : nameId){
            rnoAlias = idString.getIdString();
//            System.out.println("RNO ALIAS => " + rnoAlias);
        }
        alias_list.add(rnoAlias);
//        System.out.println("ROW NUM ALIAS IN WHERE CLAUSE :====> " + alias_list);
        return alias_list;
    }

}