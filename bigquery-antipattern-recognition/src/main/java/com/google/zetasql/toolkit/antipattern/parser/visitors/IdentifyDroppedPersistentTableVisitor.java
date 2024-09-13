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

 package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ASTNodes.ASTCreateTableStatement;
import com.google.zetasql.parser.ASTNodes.ASTDropStatement;
import com.google.zetasql.parser.ParseTreeVisitor;
 import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
 import com.google.zetasql.toolkit.antipattern.util.ZetaSQLStringParsingHelper;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.stream.Collectors;

 public class IdentifyDroppedPersistentTableVisitor extends ParseTreeVisitor
     implements AntiPatternVisitor {

   public static final String NAME = "ConvertTableToTemp";
   private final String CONVERT_TO_TEMP_SUGGESTION_MESSAGE =
       "Persistent table dropped without TEMP: Table %s defined at line %d is dropped. Consider converting to temporary.";
   // Scope of a non-temporary table
   private final String DEFAULT_SCOPE = "DEFAULT_SCOPE";

   // An array list to store the suggestions.
   private final ArrayList<String> result = new ArrayList<>();

   // A map to keep track of the created tables.
   private final Map<String, Integer> createTableMap = new HashMap<>();

   // An array list to store the dropped tables.
   private final ArrayList<String> droppedTables = new ArrayList<>();

   private String query;

   public IdentifyDroppedPersistentTableVisitor(String query) {
     this.query = query;
   }

   @Override
   public void visit(ASTCreateTableStatement createTableStatement) {
        // check if the table is temporary
        if(createTableStatement.getScope().toString() == DEFAULT_SCOPE){
            ArrayList<String> fqdm = new ArrayList<>();
            createTableStatement.getName()
            .getNames()
            .forEach(
                identifier -> {
                    // Get the identifier as a string in lower case.
                    String id = identifier.getIdString().toLowerCase();
                    fqdm.add(id);
                }
            );
            createTableMap.put(String.join(".", fqdm), createTableStatement.getParseLocationRange().start());
    }
   }

   @Override
   public void visit(ASTDropStatement dropStatement) {
        ArrayList<String> fqdm = new ArrayList<>();
        dropStatement.getName()
        .getNames()
        .forEach(
            identifier -> {
                // Get the identifier as a string in lower case.
                String id = identifier.getIdString().toLowerCase();
                fqdm.add(id);
            }
        );
        droppedTables.add(String.join(".", fqdm));
   }

   // Getter method to retrieve the list of suggestion messages.
   public String getResult() {
     System.out.println(createTableMap);
     for (Map.Entry<String, Integer> entry : createTableMap.entrySet()) {
       String tableName = entry.getKey();
       if (droppedTables.contains(tableName)){
        int lineNum = ZetaSQLStringParsingHelper.countLine(query, createTableMap.get(tableName));
        result.add(String.format(CONVERT_TO_TEMP_SUGGESTION_MESSAGE, tableName, lineNum));
       }
     }
     return result.stream().distinct().collect(Collectors.joining("\n"));
   }

   @Override
   public String getName() {
     return NAME;
   }
 }
