package com.google.zetasql.toolkit.antipattern.parser.visitors;

import autovalue.shaded.kotlin.Pair;
import com.google.common.collect.ImmutableList;
import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;

import java.util.ArrayList;
import java.util.Objects;


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


    @Override
    public void visit(ASTNodes.ASTSelect selectQuery){
        ASTNodes.ASTTableExpression fromClause = selectQuery.getFromClause().getTableExpression();
        boolean rownumFlag = false;
        boolean rhsOne = false;
        String whereClauseAlias = "" ;
        String rowNumAlias = "";
        int rowNumLineNum = 0;
        // Goto first subquery of from clause to check if rownumber is present or not
        if(fromClause instanceof ASTNodes.ASTTableSubquery){
            ASTNodes.ASTQueryExpression queryExpr = ((ASTNodes.ASTTableSubquery) fromClause).getSubquery().getQueryExpr();
            if(queryExpr instanceof ASTNodes.ASTSelect){
                ImmutableList<ASTNodes.ASTSelectColumn> columns = ((ASTNodes.ASTSelect) queryExpr).getSelectList().getColumns();
                for(ASTNodes.ASTSelectColumn col : columns) {
                    ASTNodes.ASTExpression expr = col.getExpression();
                    if (expr instanceof ASTNodes.ASTAnalyticFunctionCall) {
                        ASTNodes.ASTExpression astExpression = ((ASTNodes.ASTAnalyticFunctionCall) expr).getExpression();
                        if (astExpression instanceof ASTNodes.ASTFunctionCall) {
                            ImmutableList<ASTNodes.ASTIdentifier> identifiers = ((ASTNodes.ASTFunctionCall) astExpression).getFunction().getNames();
                            for (ASTNodes.ASTIdentifier idString : identifiers) {
                                if (idString.getIdString().equals(ROW_NUMBER_STR)) {
                                    rowNumAlias = col.getAlias().getIdentifier().getIdString();
                                    rowNumLineNum = idString.getParseLocationRange().start();
                                    rownumFlag = true;
                                    break;
                                }

                            }

                        }

                    }

                }
                if(!rownumFlag){
                    // if sub-query doesn't have row number go to inner sub-query
                    super.visit(selectQuery);
                }
            }
        }
        // Get where clause of the current node check if condition row_num = 1 is present or not
        if(selectQuery.getWhereClause() != null) {
            ASTNodes.ASTExpression expressions_where = selectQuery.getWhereClause().getExpression();
            if (expressions_where instanceof ASTNodes.ASTAndExpr) {
                ImmutableList<ASTNodes.ASTExpression> list = ((ASTNodes.ASTAndExpr) expressions_where).getConjuncts();
                for (ASTNodes.ASTExpression andExpr : list) {
                    if (andExpr instanceof ASTNodes.ASTBinaryExpression) {
                        Pair<String, Boolean> p = getWhereFilters(andExpr, false, whereClauseAlias, rowNumAlias);
                        whereClauseAlias = p.getFirst();
                        rhsOne = p.getSecond();
                    }
                    if (rhsOne)
                        break;
                }
            } else {
                Pair<String, Boolean> p = getWhereFilters(expressions_where, false, whereClauseAlias, rowNumAlias);
                whereClauseAlias = p.getFirst();
                rhsOne = p.getSecond();
            }
        }
        if(rownumFlag && whereClauseAlias.equals(rowNumAlias) && rhsOne){
            int lineNum = ZetaSQLStringParsingHelper.countLine(query, rowNumLineNum);
            result.add(String.format(LATEST_RECORDS_ROW_NUMBER_PATTERN, lineNum));
        }


    }
    //Traversing where clause to get lhs and rhs 
    public Pair<String, Boolean> getWhereFilters(ASTNodes.ASTExpression expr, boolean rhsOne, String whereClauseAlias, String rowNumAlias){
        ASTNodes.ASTExpression lhs_expression =  ((ASTNodes.ASTBinaryExpression) expr).getLhs();
        ASTNodes.ASTExpression rhs_expression =  ((ASTNodes.ASTBinaryExpression) expr).getRhs();
        if(lhs_expression instanceof ASTNodes.ASTPathExpression && rhs_expression instanceof ASTNodes.ASTIntLiteral){
            ImmutableList<ASTNodes.ASTIdentifier> name_lhs = (((ASTNodes.ASTPathExpression) lhs_expression).getNames());
            String name_rhs = ((ASTNodes.ASTIntLiteral) rhs_expression).getImage();
            for(ASTNodes.ASTIdentifier lhsIdString : name_lhs){
                String lhsAlias = lhsIdString.getIdString();
                if(Objects.equals(name_rhs, "1") && rowNumAlias.equals(lhsAlias)){
                    rhsOne = true;
                    whereClauseAlias = lhsAlias;
                    break;
                }
            }
        }
        return new Pair<>(whereClauseAlias, rhsOne);
    }

}