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

package com.google.zetasql.toolkit.antipattern.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZetaSQLStringParsingHelper {

  private static String GET_TABLE_FROM_EXPR_STRING = ".*column=([a-z0-9-]*\\.[\\w]+\\.[\\w\\-]+)";

  public static String getTableNameFromExpr(String exprString) {
    String regex = GET_TABLE_FROM_EXPR_STRING;
    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(exprString);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  public static int countLine(String query, int start) {
    byte[] utf8Bytes = query.getBytes(StandardCharsets.UTF_8);

    int utf16Index = 0;
    int byteIndex = 0;
    while (byteIndex < start && byteIndex < utf8Bytes.length) {
      int currentByte = utf8Bytes[byteIndex] & 0xFF;
      if (currentByte < 0x80) {
        byteIndex++;
      } else if (currentByte < 0xE0) {
        byteIndex += 2;
      } else if (currentByte < 0xF0) {
        byteIndex += 3;
      } else {
        byteIndex += 4;
      }
      utf16Index++;
    }

    int count = 0;
    for (int i = 0; i < utf16Index; i++) {
      if (query.charAt(i) == '\n') {
        count++;
      }
    }
    return count + 1;
  }
}
