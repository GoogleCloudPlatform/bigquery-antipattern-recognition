package com.google.zetasql.toolkit.antipattern.io;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.TableField;
import com.google.cloud.bigquery.BigQuery.TableOption;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;

public class BigQueryWriter {

  private static final int DEFAULT_BATCH_SIZE = 1000;
  private final BigQuery client;
  private final int batchSize;

  public BigQueryWriter(BigQuery client, int batchSize) {
    this.client = client;
    this.batchSize = batchSize;
  }

  public BigQueryWriter(BigQuery client) {
    this(client, DEFAULT_BATCH_SIZE);
  }

  public void ensureTableExists(TableId tableId, TableDefinition tableDefinition) {
    Table table = this.client.getTable(
        tableId,
        TableOption.fields(
            TableField.ID,
            TableField.TABLE_REFERENCE
        ));

    if (table == null) {
      TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();
      this.client.create(tableInfo);
    }
  }

  public void writeRows(TableId tableId, List<RowToInsert> rows) {
    List<List<RowToInsert>> batches = Lists.partition(rows, this.batchSize);

    for (List<RowToInsert> batch : batches) {
      InsertAllRequest request = InsertAllRequest.newBuilder(tableId, batch).build();
      InsertAllResponse response = this.client.insertAll(request);

      Optional<List<BigQueryError>> firstInsertErrors = response.getInsertErrors()
          .values()
          .stream()
          .findFirst();

      firstInsertErrors.ifPresent(errors -> {
        throw new BigQueryException(errors);
      });

    }
  }

}
