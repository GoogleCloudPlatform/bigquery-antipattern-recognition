package com.google.zetasql.toolkit.antipattern.rewriter.prompt;
import com.google.zetasql.toolkit.antipattern.analyzer.visitors.joinorder.JoinOrderVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.*;
import com.google.zetasql.toolkit.antipattern.parser.visitors.rownum.IdentifyLatestRecordVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.whereorder.IdentifyWhereOrderVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewriterConstants {

    public final static String PROMPT_TEMPLATE = "%s"
        + "\n%s"
        + "\nConsider the following examples:"
        + "\n%s"
        + "\n%s";

    public static final String PROMPT_HEADER = "You are a proficient data engineer. \n" +
        "You are an expert in BigQuery SQL rewrite and optimization.";

    public static final String PROMPT_FOOTER = "re-write the following SQL accordingly\n%s";
}
