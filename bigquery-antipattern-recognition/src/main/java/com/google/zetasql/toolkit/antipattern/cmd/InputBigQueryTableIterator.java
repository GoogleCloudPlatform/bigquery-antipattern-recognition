/*
 * Copyright 2024 Google LLC
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

package com.google.zetasql.toolkit.antipattern.cmd;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;

import java.io.IOException;
import java.util.Iterator;

public class InputBigQueryTableIterator implements Iterator<InputQuery> {

  Iterator<FieldValueList> fieldValueListIterator;

  public InputBigQueryTableIterator(String inputTable, String processingProject, String serviceAccountKeyfilePath)
      throws InterruptedException, IOException {

    BigQueryHelper bigQueryHelper = new BigQueryHelper(processingProject,
        serviceAccountKeyfilePath);
    TableResult tableResult = bigQueryHelper.getQueriesFromBQTable(inputTable);

    fieldValueListIterator = tableResult.iterateAll().iterator();
  }

  @Override
  public boolean hasNext() {
    return fieldValueListIterator.hasNext();
  }

  @Override
  public InputQuery next() {
    FieldValueList row = fieldValueListIterator.next();
    String job_id = row.get("id").getStringValue();
    String query = row.get("query").getStringValue();
    return new InputQuery(query, job_id);
  }
}
