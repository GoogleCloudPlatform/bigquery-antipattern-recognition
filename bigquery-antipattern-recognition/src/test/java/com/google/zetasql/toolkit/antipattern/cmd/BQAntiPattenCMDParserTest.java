package com.google.zetasql.toolkit.antipattern.cmd;

import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class BQAntiPattenCMDParserTest {
    private final String args[];
    private final boolean shouldFail;
    private final String errorMessage;

    public BQAntiPattenCMDParserTest(String args[], boolean shouldFail, String errorMessage) {
        this.args =  args;
        this.shouldFail = shouldFail;
        this.errorMessage = errorMessage;
    }
    @Parameterized.Parameters
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][] {
                { new String[0], false, null},
                { new String[] {"--random_option"}, true, "Unrecognized option: --random_option"},
                { new String[] {
                        "--read_from_info_schema",
                        "--read_from_info_schema_days", "4",
                        "--info_schema_table_name", "`region-us`.INFORMATION_SCHEMA.JOBS",
                        "--processing_project_id", "a-gcp-project-for-analysis",
                        "--output_table", "a-gcp-project-for-analysis.bq_recommendations.antipattern_output_table"
                }, false, null},
                { new String[] {
                        "--read_from_info_schema",
                        "--read_from_info_schema_start_time", "'2023-08-15'",
                        "--read_from_info_schema_end_time", "'2023-08-16'",
                        "--read_from_info_schema_timeout_in_secs", "60",
                        "--info_schema_table_name", "`region-us`.INFORMATION_SCHEMA.JOBS",
                        "--processing_project_id", "a-gcp-project-for-analysis",
                        "--output_table", "a-gcp-project-for-analysis.bq_recommendations.antipattern_output_table"
                }, false, null},
                { new String[] {
                        "--read_from_info_schema_start_time",
                }, true, "Missing argument for option: read_from_info_schema_start_time"},
                { new String[] {
                        "--read_from_info_schema_end_time",
                }, true, "Missing argument for option: read_from_info_schema_end_time"},
                { new String[] {
                        "--read_from_info_schema_timeout_in_secs",
                }, true, "Missing argument for option: read_from_info_schema_timeout_in_secs"},
        });
    }
    @Test
    public void testMethod() {
        try {
            new BQAntiPatternCMDParser(args);
            if (shouldFail) {
                Assert.fail("parsing should fail, but did not fail, for args: " + Arrays.toString(args));
            }
        } catch (ParseException e) {
            if (shouldFail) {
                Assert.assertEquals(this.errorMessage, e.getMessage());
            } else {
                Assert.fail(e.getMessage());
            }
        }
    }
}
