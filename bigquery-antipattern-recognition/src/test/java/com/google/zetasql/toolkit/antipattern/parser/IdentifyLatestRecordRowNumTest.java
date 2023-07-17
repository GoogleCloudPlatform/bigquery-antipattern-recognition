package com.google.zetasql.toolkit.antipattern.parser;

import static org.junit.Assert.*;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import org.junit.Before;
import org.junit.Test;

public class IdentifyLatestRecordRowNumTest {
    LanguageOptions languageOptions;
    @Before
    public void setUp() {
        languageOptions = new LanguageOptions();
        languageOptions.enableMaximumLanguageFeatures();
        languageOptions.setSupportsAllStatementKinds();
    }
    @Test
    public void simpleTest() {
        String expected = "ROW_NUMBER() function used in line : 1, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query =
                "select * from ( select *, row_number() over( partition by id order by created_at desc) rn from `dataset.table` t ) where rn = 1 ";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }

    @Test
    public void starAndWhereFilterTest() {
        String expected = "ROW_NUMBER() function used in line : 1, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query =
                "select * except(rn) from ( select *, row_number() over( partition by id order by created_at desc) rn from `dataset.table` t where r2 < 200 and rm = 1 and r3 > 400 ) where rn = 1";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }

    @Test
    public void multiWhereFilterTest() {
        String expected = "ROW_NUMBER() function used in line : 1, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query =
                "select col1,col2,col3 from ( select col1,col2, row_number() over( partition by id order by created_at desc) rn from `dataset.table` t where r2 < 200 ) where r1 > 100 and rn = 1 and col2 < 300";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }

    @Test
    public void aliasTest() {
        String expected = "ROW_NUMBER() function used in line : 2, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query =
                "select col1 as r1 ,col2,col3 from \n" +
                        "( select col1,col2, row_number() over( partition by id order by created_at desc) rn " +
                        "from `dataset.table` t where r2 < 200 ) " +
                        "where rm = 1 and r1 > 100 and rn = 1 and col2 < 300 ";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }

    @Test
    public void mutliSubQueryTest() {
        String expected = "ROW_NUMBER() function used in line : 4, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query =
                "select * from \n (select * from \n( select col1, col2, col3 from \n( select col1, col2, col3, row_number() over(order by created_at desc) rn \n from `dataset.table` where col1 < 200 ) \n where col3 > 100 and rn = 1 ) \n where col2 > 400 )";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }

    @Test
    public void orAndFuncTest() {
        String expected = "";
        String query = "select col1 as r1 ,col2,col3 from ( select col1,col2, row_number() over( partition by id order by created_at desc) rn from `dataset.table` t where r2 < 200 ) where rm = 1 and (regexp_contains(col2,r'something') or rn >= 1) and col2 > 300 and 1 = r1 ";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }

    @Test
    public void otherFuncTest() {
        String expected = "ROW_NUMBER() function used in line : 1, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query = "select col1 as r1 ,col2,col3 from ( select col1,col2, row_number() over( partition by id order by created_at desc) rn from `dataset.table` t where r2 < 200 ) where rm = 1 and regexp_contains(col2,r'something') and rn >= 1 and col2 > 300 and 1 = r1 ";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }
    @Test
    public void rNoInTest() {
        String expected = "ROW_NUMBER() function used in line : 1, to find the latest records. Instead use ARRAY_AGG() to run the query more efficiently";
        String query = "select col1 as r1 ,col2,col3 from ( select col1,col2, row_number() over( partition by id order by created_at desc) rno from `dataset.table` t where r2 < 200 ) where rm = 1 and rno in (1,2,3) and col2 > 300 and 1 = r1 and col3 between 1 and 4";
        ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
        String recommendations = (new IdentifyLatestRecordsRowNum()).run(parsedQuery, query);
        assertEquals(expected, recommendations);
    }


}
