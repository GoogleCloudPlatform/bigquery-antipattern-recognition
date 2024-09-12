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

 package com.google.zetasql.toolkit.antipattern.parser;

 import static org.junit.Assert.assertEquals;

 import com.google.zetasql.LanguageOptions;
 import com.google.zetasql.Parser;
 import com.google.zetasql.parser.ASTNodes.ASTStatement;
 import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyCTEsEvalMultipleTimesVisitor;
 import org.junit.Before;
 import org.junit.Test;

 public class IdentifyConvertTablesToTempTest {
   LanguageOptions languageOptions;

   @Before
   public void setUp() {
     languageOptions = new LanguageOptions();
     languageOptions.enableMaximumLanguageFeatures();
     languageOptions.setSupportsAllStatementKinds();
   }

   // Test with a query that uses two CTEs aliased (b,c) referencing CTE aliased (a) multiple times
   @Test
   public void withThreeCTEsTest() {
     String expected = "CTE with multiple references: alias a defined at line 2 is referenced 2 times.";
     String query =
         "WITH\n"
             + "    a AS (\n"
             + "        SELECT\n"
             + "            *\n"
             + "        FROM\n"
             + "            `project.dataset.table1` t1\n"
             + "    ),\n"
             + "    b AS (\n"
             + "        SELECT\n"
             + "            *\n"
             + "        FROM\n"
             + "            a t2\n"
             + "    ),\n"
             + "    c AS (\n"
             + "        SELECT\n"
             + "            *\n"
             + "        FROM\n"
             + "            a t3\n"
             + "    )\n"
             + "SELECT\n"
             + "    b.col1,\n"
             + "    c.col2\n"
             + "FROM\n"
             + "    b, c;";

     ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
     IdentifyCTEsEvalMultipleTimesVisitor visitor = new IdentifyCTEsEvalMultipleTimesVisitor(query);
     parsedQuery.accept(visitor);
     String recommendations = visitor.getResult();
     assertEquals(expected, recommendations);
   }

   // Test with a query that uses one CTE aliased (a) referenced  multiple times in from clause and a
   // subquery
   @Test
   public void withOneCTETest() {
     String expected = "CTE with multiple references: alias a defined at line 2 is referenced 2 times.";
     String query =
         "WITH\n"
             + "  a AS (\n"
             + "  SELECT\n"
             + "    *\n"
             + "  FROM\n"
             + "    `project.dataset.table1 t1` )\n"
             + "SELECT\n"
             + "  a.col1,\n"
             + "  a.col2,\n"
             + "  total\n"
             + "FROM\n"
             + "  a\n"
             + "LEFT JOIN (\n"
             + "  SELECT\n"
             + "    a.col1,\n"
             + "    SUM(a.col3) total\n"
             + "  FROM\n"
             + "    a\n"
             + "  GROUP BY\n"
             + "    a.col1) t2\n"
             + "ON\n"
             + "  t2.col1 = a.col2;";

     ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
     IdentifyCTEsEvalMultipleTimesVisitor visitor = new IdentifyCTEsEvalMultipleTimesVisitor(query);
     parsedQuery.accept(visitor);
     String recommendation = visitor.getResult();
     assertEquals(expected, recommendation);
   }

   // Test with a query that uses two CTEs aliased (b,c) not referenced more than once.
   @Test
   public void withTwoCTEsTest() {
     String expected = "";
     String query =
         "WITH \n"
             + "    b AS ( \n"
             + "        SELECT \n"
             + "            * \n"
             + "        FROM \n"
             + "            `project.dataset.b` \n"
             + "    ), \n"
             + "    c AS ( \n"
             + "        SELECT \n"
             + "            * \n"
             + "        FROM \n"
             + "            `project.dataset.c` \n"
             + "    ) \n"
             + "SELECT \n"
             + "    b.dim1, \n"
             + "    c.dim2 \n"
             + "FROM \n"
             + "    b, \n"
             + "    c;";

     ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
     IdentifyCTEsEvalMultipleTimesVisitor visitor = new IdentifyCTEsEvalMultipleTimesVisitor(query);
     parsedQuery.accept(visitor);
     String recommendation = visitor.getResult();
     assertEquals(expected, recommendation);
   }

   // Test with a query that uses joins and sub-queries like nested patterns.
   @Test
   public void nestedJoinsTest(){
     String expected = "CTE with multiple references: alias a defined at line 1 is referenced 3 times.";
     String query = "with a as (\n" +
         "select col1, col2, col3\n" +
         "from `dataset.table`\n" +
         "where col1 = 10\n" +
         "),\n" +
         "b as (\n" +
         "select col4, col5\n" +
         "from `dataset.table` a\n" +
         "left join `dataset.table2` b on a.id = b.id\n" +
         "left join `dataset.table3` c on b.id = c.id\n" +
         "left join (select * from `dataset.table3`) d on c.id = d.id \n" +
         "),\n" +
         "c as (\n" +
         "select col5, col6\n" +
         "from ( select * from `dataset.table5` ) x\n" +
         "left join a on x.id = a.id\n" +
         "),\n" +
         "d as (\n" +
         "select col6, col7,\n" +
         "from a\n" +
         ")\n" +
         "select *\n" +
         "from `dataset.table5` b\n" +
         "left join a on a.id = b.id";
     ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
     IdentifyCTEsEvalMultipleTimesVisitor visitor = new IdentifyCTEsEvalMultipleTimesVisitor(query);
     parsedQuery.accept(visitor);
     String recommendation = visitor.getResult();
     assertEquals(expected, recommendation);
   }
 }
