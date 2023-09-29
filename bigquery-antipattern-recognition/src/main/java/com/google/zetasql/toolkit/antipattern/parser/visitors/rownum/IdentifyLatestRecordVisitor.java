/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zetasql.toolkit.antipattern.parser.visitors.rownum;

import com.google.zetasql.parser.ASTNodes;
import com.google.zetasql.parser.ASTNodes.ASTFromClause;
import com.google.zetasql.parser.ASTNodes.ASTSelect;
import com.google.zetasql.parser.ASTNodes.ASTSelectColumn;
import com.google.zetasql.parser.ASTNodes.ASTTableExpression;
import com.google.zetasql.parser.ASTNodes.ASTWhereClause;
import com.google.zetasql.parser.ParseTreeVisitor;
import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class IdentifyLatestRecordVisitor extends ParseTreeVisitor {

  private String query;
  private ASTNodes.ASTSelectColumn selectColWithRowNumNode = null;
  private ASTNodes.ASTBinaryExpression filterWithRowNumNode = null;
  private String stringIdAnalyticalFunBeingUsed = null;
  private ArrayList<String> result = new ArrayList<String>();

  private static final String[] STRING_IDS_ANALYTIC_FUN = {"row_number", "rank"};
  private final String LATEST_RECORD_SUGGESTION_MESSAGE = "Seems like you might be using analytical function %s in line %d to filter the latest record in line %d.";

  public IdentifyLatestRecordVisitor(String query) {
    System.out.println(query);
    this.query = query;
  }

  @Override
  public void visit(ASTNodes.ASTSelect selectNode) {
    ASTWhereClause whereNode = selectNode.getWhereClause();
    if(whereNode != null && selectNode.getFromClause() != null) {
      searchForRowNumInFrom(selectNode.getFromClause().getTableExpression(), whereNode);
    }
    super.visit(selectNode);
  }

  public void searchForRowNumInFrom(ASTTableExpression tableExpression, ASTWhereClause whereNode) {
    if (tableExpression instanceof ASTNodes.ASTTableSubquery) {
      searchRowNumFunUsedInFilter((ASTNodes.ASTTableSubquery) tableExpression, whereNode);
    } else if (tableExpression instanceof ASTNodes.ASTJoin) {
      ASTNodes.ASTJoin joinNode = (ASTNodes.ASTJoin) tableExpression;
      searchForRowNumInFrom(joinNode.getLhs(), whereNode);
      searchForRowNumInFrom(joinNode.getRhs(), whereNode);
    }
  }

  public String searchRowNumFunUsedInFilter(ASTNodes.ASTTableSubquery subqueryNode, ASTWhereClause whereNode) {
    selectColWithRowNumNode = null;
    stringIdAnalyticalFunBeingUsed = null;
    ASTNodes.ASTQueryExpression queryExpr = subqueryNode.getSubquery().getQueryExpr() ;
    if(queryExpr instanceof ASTNodes.ASTSelect) {
      ASTNodes.ASTSelectList selectList = ((ASTSelect) queryExpr).getSelectList();
      //search each column until we find a row_number() or rank()
      Iterator<ASTSelectColumn> columnsIterator = selectList.getColumns().iterator();
      while(columnsIterator.hasNext()) {
        ASTSelectColumn col = columnsIterator.next();
        if(col.getExpression() instanceof ASTNodes.ASTAnalyticFunctionCall){
          ASTNodes.ASTAnalyticFunctionCall analyticFunctionNode = (ASTNodes.ASTAnalyticFunctionCall) col.getExpression();
          ASTNodes.ASTFunctionCall functionCallNode = (ASTNodes.ASTFunctionCall) analyticFunctionNode.getExpression();
          //search alias for col with row_num or window function
          String windowFunIdString = functionCallNode.getFunction().getNames().get(0).getIdString();
          if(Arrays.stream(STRING_IDS_ANALYTIC_FUN).anyMatch(windowFunIdString::equals)){
            selectColWithRowNumNode = col;
            stringIdAnalyticalFunBeingUsed = windowFunIdString;
            String rowNumColAlias = col.getAlias().getIdentifier().getIdString();
            if(searchRowNumFilterInWhere(whereNode, rowNumColAlias)){
              int rowNumColLineNum = ZetaSQLStringParsingHelper.countLine(query, selectColWithRowNumNode.getParseLocationRange().start());
              int rowNumFilterLineNum = ZetaSQLStringParsingHelper.countLine(query, filterWithRowNumNode.getParseLocationRange().start());
              result.add(String.format(LATEST_RECORD_SUGGESTION_MESSAGE, stringIdAnalyticalFunBeingUsed, rowNumColLineNum, rowNumFilterLineNum));
            }
          }
        }
      }
    }
    return null;
  }

  public boolean searchRowNumFilterInWhere(ASTNodes.ASTWhereClause whereNode, String rowNumColAlias) {
    filterWithRowNumNode = null;
    SearchRowNumFilterInWhereVisitor searchRowNumFilterInWhereVisitor = new SearchRowNumFilterInWhereVisitor(rowNumColAlias);
    whereNode.accept(searchRowNumFilterInWhereVisitor);
    boolean foundFilter = searchRowNumFilterInWhereVisitor.getfoundFilter();
    if(foundFilter) {
      filterWithRowNumNode = searchRowNumFilterInWhereVisitor.getFilterWithRowNumNode();
    }
    return foundFilter;
  }

  public ArrayList<String> getResult() {
    return result;
  }
}
