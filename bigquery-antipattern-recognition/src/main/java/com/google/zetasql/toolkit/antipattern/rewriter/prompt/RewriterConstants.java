package com.google.zetasql.toolkit.antipattern.rewriter.prompt;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder.JoinOrderVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.*;
import com.google.zetasql.toolkit.antipattern.parser.visitors.rownum.IdentifyLatestRecordVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;

import java.util.HashMap;
import java.util.Map;

public class RewriterConstants {

    // TODO: add parametrized examples (prompt template structure)
    private final static String WHERE_ORDER_GEMINI_PROMPT = "You are a proficient data engineer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You know that the order of predicates in a where condition impacts performance.\n" +
            "More restrictive filters such as equality filters should be applied first.\n" +
            "More complex filters such as a like filter should be applied last.\n" +
            "This will increase performance.\n" +
            "                                        \n" +
            "For example consider this query\n" +
            "\"select col1 from table1 where col2 like '%%asd%%' AND col3 = 'a' \"\n" +
            "For better performance it should be re written as                                        \n" +
            "\"select col1 from table1 where col3 = 'a' and col2 like '%%asd%%' \"\n" +
            "Since the more restrictive equality filter is applied first then query will perform better.\n" +
            "\n" +
            "Could you please re write the following SQL accordingly\n%s";

    private final static String STRING_COMPARISON_GEMINI_PROMPT = "You are a proficient data engineer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You now that in a predicate in a where string using a \"like\" is simpler and mre efficient than using \"regexp_contains\".\n" +
            "Simple regex such as `col1 = regexp_contains(col1, '.*some_string.*')` can be re written to like `col1 like '%%some_string%%'`.\n" +
            "\n" +
            "For example consider the following query:\n" +
            "\"\n" +
            "select col1 from table1 where col1 = regexp_contains(col1, '.*some_string.*');\n" +
            "\"\n" +
            "\n" +
            "Performance will increase by using a like condition in the filter instead of the regexp_contains\n" +
            "\"\n" +
            "select col1 from table1 where col1 like '%%some_string%%';\n" +
            "\"\n" +
            "\n" +
            "Could you please re write the following SQL accordingly.\n %s";

    private final static String SEMI_JOIN_NO_AGG_GEMINI_PROMPT = "You are a proficient data engineer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You know that having a query predicate in a where without an aggregation might cause an issue.\n" +
            "For example\n" +
            "\"SELECT col1 FROM table1 WHERE col2 in (select col1 from table2)\"\n" +
            "The sub-query used for the \"IN\" filter \"(select col1 from table2)\" does not have an aggregation.\n" +
            "Performance will increase if a distinct is added.\n" +
            "For example, instead of \n" +
            "\"SELECT col1 FROM table1 WHERE col2 in (select col1 from table2)\"\n" +
            "it would be better to have \n" +
            "\"SELECT col1 FROM table1 WHERE col2 in (select distinct col1 from table2)\"\n" +
            "\n" +
            "Could you please re write the following SQL accordingly.\n%s";

    private final static String ORDER_BY_NO_LIMIT_GEMINI_PROMPT = "You are a proficient data engineer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You now that ordering a large amount of rows has a considerable computational cost.\n" +
            "Performance enhancements can be obtained be adding a LIMIT after the outermost order by\n" +
            "\n" +
            "For example consider the following query:\n" +
            "\"\n" +
            "select col1 from table1 order by col1;\n" +
            "\"\n" +
            "\n" +
            "Performance will increase by adding a limit\n" +
            "\"\n" +
            "select col1 from table1 order by col1 limit 1000;\n" +
            "\"\n" +
            "\n" +
            "Could you please re write the following SQL accordingly.\n%s";

    private final static String MULTIPLE_CTE_GEMINI_PROMPT = "You are a proficient data enginer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You now that contents of a WITH statement will be inlined every place the alias is referenced.\n" +
            "This could lead to negative performance impact. For better performance a WITH statement referenced more than once \n" +
            "should be re written as a temp table to avoid computing the same statement more than once.\n" +
            "\n" +
            "For example consider the following query:\n" +
            "\"\n" +
            "with a as (select col1 from table1 group by col1),\n" +
            "b as (select col1 from a),\n" +
            "c as (select col1 from a)\n" +
            "SELECT\n" +
            "    b.col1, c.col1\n" +
            "FROM\n" +
            "    b,c\n" +
            "\"\n" +
            "Performance will increase by making the initial with statement a temp table.\n" +
            "\"\n" +
            "create temp table a as (select col1 from table1 group by col1);\n" +
            "with b as (select col1 from a),\n" +
            "c as (select col1 from a)\n" +
            "SELECT\n" +
            "    b.col1, c.col1\n" +
            "FROM\n" +
            "    b,c\n" +
            "\"\n" +
            "\n" +
            "Could you please re write the following SQL accordingly.\n%s";

    private final static String LATEST_RECORD_GEMINI_PROMPT = "You are a proficient data enginer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You now that ROW_NUMBER() function is frequently used to get the latest record of a given partition.\n" +
            "You also know using ARRAY_AGG() in BigQuery instead of ROW_NUMBER() for this purpose yields better performance\n" +
            "\n" +
            "For example consider the following query:\n" +
            "\"\n" +
            "select \n" +
            "  * except(rn)\n" +
            "from (\n" +
            "  select *, \n" +
            "    row_number() over(\n" +
            "      partition by id \n" +
            "      order by created_at desc) rn\n" +
            "  from \n" +
            "    `dataset.table` t\n" +
            ")\n" +
            "where rn = 1\n" +
            "\"\n" +
            "\n" +
            "Performance will increase by using an ARRAY_AGG() as shown below.\n" +
            "\"\n" +
            "select  \n" +
            "  event.* \n" +
            "from (\n" +
            "  select array_agg(\n" +
            "    t order by t.created_at desc limit 1\n" +
            "  )[offset(0)] event\n" +
            "  from \n" +
            "    `dataset.table` t \n" +
            "  group by \n" +
            "    id\n" +
            ")\n" +
            "\"\n" +
            "\n" +
            "Could you please re write the following SQL accordingly.\n%s";

    private final static String JOIN_ORDER_PROMPT= "You are a proficient data engineer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You know that for optimal performance the first table in a JOIN should be te largest. \n" +
            " \n" +
            "for example consider the following query:\n" +
            "\"\n" +
            "SELECT \n" +
            "    col1,\n" +
            "    count(1) ct\n" +
            "FROM \n" +
            "    table1 t1\n" +
            "JOIN\n" +
            "    table2 t2 ON t1.col2=t2.col2\n" +
            "\"\n" +
            "\n" +
            "Assuming table2 is bigger than table1 the following syntax would yield a higher performance:\n" +
            "\"\n" +
            "SELECT \n" +
            "    col1,\n" +
            "    count(1) ct\n" +
            "FROM \n" +
            "    table2 t2\n" +
            "JOIN\n" +
            "    table1 t1 ON t1.col2=t2.col2\n" +
            "\"\n" +
            "\n" +
            "\n" +
            "Could you please re write the following SQL accordingly. \n" +
            "\n" +
            "%s\n" +
            "\n" +
            "consider that %s";

    private final static String DYNAMIC_PREDICATE_GEMINI_PROMPT = "You are a proficient data engineer. \n" +
            "You are an expert in BigQuery SQL rewrite and optimization.\n" +
            "\n" +
            "You know that computing a query within a predicate in a where is not ideal for performance.\n" +
            "It is best to compute the sub-query and store the result in a variable\n" +
            "Note this is only applicable when the sub-query returns a single value, this is then operators such as equal, \n" +
            "lesser than or greater than are used\n" +
            "\n" +
            "for example consider this query\n" +
            "\"select col1 from table1 where col2 = (select max(col3) from table2)\"\n" +
            "it would be better to rewrite as follows:\n" +
            "\"\n" +
            "declare var1 [DATATYPE] DEFAULT (select 'a'); --add corresponding datatype here\n" +
            "select col1 from table1 where col2 = var1;\n" +
            "\"\n" +
            "\n" +
            "Could you please re write the following SQL accordingly. \n" +
            "Note that since we do not know the datatype of the column used in the \n" +
            "predicate we will leave a placeholder [dataype] so that somebody can manually replace the datatype\n" +
            "\n" +
            "Just give me the query, no additional text";

    public final static Map<String, String> antiPatternNameToGeminiPrompt = new HashMap<>();
    {
        antiPatternNameToGeminiPrompt.put(IdentifyInSubqueryWithoutAggVisitor.NAME, SEMI_JOIN_NO_AGG_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(IdentifyCTEsEvalMultipleTimesVisitor.NAME, MULTIPLE_CTE_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(IdentifyOrderByWithoutLimitVisitor.NAME, ORDER_BY_NO_LIMIT_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(IdentifyRegexpContainsVisitor.NAME, STRING_COMPARISON_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(IdentifyLatestRecordVisitor.NAME, LATEST_RECORD_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(IdentifyDynamicPredicateVisitor.NAME, DYNAMIC_PREDICATE_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(IdentifyWhereOrderVisitor.NAME, WHERE_ORDER_GEMINI_PROMPT);
        antiPatternNameToGeminiPrompt.put(JoinOrderVisitor.NAME, JOIN_ORDER_PROMPT);
    }
}
