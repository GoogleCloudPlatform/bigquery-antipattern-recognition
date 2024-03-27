/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zetasql.toolkit.antipattern.parser;

import static org.junit.Assert.*;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyRegexpContainsVisitor;
import org.junit.Before;
import org.junit.Test;

public class IdentifyRegexpContainsTest {
    LanguageOptions languageOptions;

    @Before
    public void setUp() {
        languageOptions = new LanguageOptions();
        languageOptions.enableMaximumLanguageFeatures();
        languageOptions.setSupportsAllStatementKinds();
    }

    @Test
    public void regexContainsOnlyTest() {
        String expected = "REGEXP_CONTAINS at line 3. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
        String query =
            "select dim1 \n"
                + "from `dataset.table` \n"
                + "where regexp_contains(dim1, '.*test.*')";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        String recommendations = visitor.getResult();
        assertEquals(expected, recommendations);
    }

    @Test
    public void regexContainsWithOtherFuncsTest() {
        String expected = "REGEXP_CONTAINS at line 5. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
        String query =
            "select dim1 \n"
                + "from `dataset.table` \n"
                + "where effective_start_dte = current_date() \n"
                + "and dim2 = 'x' \n"
                + "and regexp_contains(dim1, '.*test.*')";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        String recommendations = visitor.getResult();
        assertEquals(expected, recommendations);
    }

    @Test
    public void multipleRegexpTest() {
        String expected = "REGEXP_CONTAINS at line 5. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).\n"
            + "REGEXP_CONTAINS at line 7. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
        String query =
            "select dim1 \n"
                + "from `dataset.table` \n"
                + "where effective_start_dte = current_date() \n"
                + "and dim2 = 'x' \n"
                + "and regexp_contains(dim1, '.*test.*')\n"
                + "and col1 = 1 \n"
                + "and regexp_contains(dim2, '.*test.*')";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        String recommendations = visitor.getResult();
        assertEquals(expected, recommendations);
    }

    @Test
    public void withoutRegexContainsTest() {
        String expected = "";
        String query =
            "select dim1 \n"
                + "from `dataset.table` \n"
                + "where effective_start_dte = current_date()";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        String recommendations = visitor.getResult();
        assertEquals(expected, recommendations);
    }

    @Test
    public void otherFuncTest() {
        String expected = "";
        String query =
            "select dim1 \n"
                + "from `dataset.table` \n"
                + "where effective_start_dte = current_date() \n"
                + "and lower(\"Sunday\") = day_of_week";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        String recommendations = visitor.getResult();
        assertEquals(expected, recommendations);
    }

    @Test
    public void regexpContainWithTest() {
        String expected = "REGEXP_CONTAINS at line 4. Prefer LIKE when the full power of regex is not needed (e.g. wildcard matching).";
        String query =
            "with a as (select \n"
                + "dim1 from `dataset.table` \n"
                + "where effective_start_dte = current_date() \n"
                + "and regexp_contains(dim1, '.*test.*') \n"
                + "and lower('Sunday') = day_of_week) \n"
                + "select * from a";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        IdentifyRegexpContainsVisitor visitor = new IdentifyRegexpContainsVisitor(query);
        parsedQuery.accept(visitor);
        String recommendations = visitor.getResult();
        assertEquals(expected, recommendations);
    }

}
