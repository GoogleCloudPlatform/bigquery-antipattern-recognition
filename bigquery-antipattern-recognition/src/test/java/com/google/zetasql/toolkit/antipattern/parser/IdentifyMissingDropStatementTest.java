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
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyMissingDropStatementVisitor;

import org.junit.Before;
 import org.junit.Test;

 public class IdentifyMissingDropStatementTest {
   LanguageOptions languageOptions;

   @Before
   public void setUp() {
     languageOptions = new LanguageOptions();
     languageOptions.enableMaximumLanguageFeatures();
     languageOptions.setSupportsAllStatementKinds();
   }

   // Test with a query that creates a temp table and does not drop it
   @Test
   public void oneTempTableTest() {
     String expected = "TEMP table created without DROP statement: TEMP table example defined at line 1 is created and not dropped.";
     String query = "CREATE TEMP TABLE mydataset.example \n"
     + "(\n"
     +  "x INT64, \n"
     +  "y STRING \n"
     + "); \n"
     + ";";

     ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
     IdentifyMissingDropStatementVisitor visitor = new IdentifyMissingDropStatementVisitor(query);
     parsedQuery.accept(visitor);
     String recommendations = visitor.getResult();
     assertEquals(expected, recommendations);
   }

 }
