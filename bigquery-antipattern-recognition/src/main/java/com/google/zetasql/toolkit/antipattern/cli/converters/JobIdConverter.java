package com.google.zetasql.toolkit.antipattern.cli.converters;

import com.google.cloud.bigquery.JobId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class JobIdConverter implements ITypeConverter<JobId> {

  private static final Pattern jobIdPattern =
      Pattern.compile("^([^:]+)[:\\.]([^\\.]+)\\.(.+)$");

  @Override
  public JobId convert(String input) {
    Matcher matcher = jobIdPattern.matcher(input);

    if(!matcher.find()) {
      throw new TypeConversionException(
          "Invalid format for BigQuery Job ID: "
          + "must be 'PROJECT:REGION.ID' but was '" + input + "'");
    }

    String project = matcher.group(1);
    String region = matcher.group(2);
    String job = matcher.group(3);

    return JobId.newBuilder()
        .setProject(project)
        .setLocation(region)
        .setJob(job)
        .build();
  }

}
