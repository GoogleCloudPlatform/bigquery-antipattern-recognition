package com.google.zetasql.toolkit.antipattern.cli.converters;

import com.google.cloud.bigquery.TableId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class TableIdConverter implements ITypeConverter<TableId> {

  private static final String PROJECT_PATTERN = "[a-zA-Z0-9\\.\\-\\:]+";
  private static final String DATASET_PATTERN = "[a-zA-Z_][a-zA-Z0-9\\_]*";
  private static final String RESOURCE_PATTERN = "[a-zA-Z0-9\\_]+";

  private TypeConversionException error(String input) {
    return new TypeConversionException(
        "Invalid format for BigQuery table: "
            + "must be [PROJECT.]DATASET.TABLE, but was '" + input + "'");
  }

  @Override
  public TableId convert(String input) {
    List<String> elements = Arrays.asList(input.split("\\."));
    int numberOfElements = elements.size();

    if (numberOfElements != 2 && numberOfElements != 3) {
      throw error(input);
    }

    Optional<String> maybeProjectId = numberOfElements == 3
        ? Optional.of(elements.get(0))
        : Optional.empty();
    String datasetId = elements.get(numberOfElements - 2);
    String tableName = elements.get(numberOfElements - 1);

    if (
        (maybeProjectId.isPresent() && !maybeProjectId.get().matches(PROJECT_PATTERN))
        || !datasetId.matches(DATASET_PATTERN)
        || !tableName.matches(RESOURCE_PATTERN)
    ) {
      throw error(input);
    }

    return maybeProjectId
        .map(projectId -> TableId.of(projectId, datasetId, tableName))
        .orElse(TableId.of(datasetId, tableName));
  }

}
