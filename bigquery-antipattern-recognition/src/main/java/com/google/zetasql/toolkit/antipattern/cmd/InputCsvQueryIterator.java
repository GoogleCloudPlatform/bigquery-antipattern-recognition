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

package com.google.zetasql.toolkit.antipattern.cmd;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class InputCsvQueryIterator implements Iterator<InputQuery> {

  private Iterator<String[]> reader;

  public InputCsvQueryIterator(String csvPath) throws IOException {
    reader = (new CSVReader(new FileReader(csvPath))).iterator();
    // pop header
    reader.next();
  }

  @Override
  public boolean hasNext() {
    return reader.hasNext();
  }

  @Override
  public InputQuery next() {
    String[] next = reader.next();
    return new InputQuery(next[1].replace("\"\"", "\""), next[0]);
  }
}
