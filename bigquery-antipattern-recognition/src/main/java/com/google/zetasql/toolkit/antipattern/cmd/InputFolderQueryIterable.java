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

import com.google.cloud.storage.*;
import com.google.zetasql.toolkit.antipattern.util.GCSHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class InputFolderQueryIterable implements Iterator<InputQuery> {

  Iterator<String> filePathIterator;
  GCSHelper gcsHelper = new GCSHelper();

  public InputFolderQueryIterable(List<String> filePathList) {
    this.filePathIterator = filePathList.iterator();
  }

  @Override
  public boolean hasNext() {
    return filePathIterator.hasNext();
  }

  @Override
  public InputQuery next() {
    String filePathStr = filePathIterator.next();
    Path fileName = Path.of(filePathStr);
    try {
      if (GCSHelper.isGCSPath(filePathStr)) {
        return gcsHelper.getInputQueryFromGCSPath(filePathStr);
      } else {
        return new InputQuery(Files.readString(fileName), filePathStr);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
