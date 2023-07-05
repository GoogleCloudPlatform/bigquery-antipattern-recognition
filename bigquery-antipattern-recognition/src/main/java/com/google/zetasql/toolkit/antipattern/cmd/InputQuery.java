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

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.Optional;

public class InputQuery {

  private String query;
  private String queryIdentifier;
  private String projectId;
  private float slotHours;
  private Optional<String> region = Optional.empty();
  private Optional<Instant> startTime = Optional.empty();

  public InputQuery(String query, String queryIdentifier) {
    this.query = query;
    this.queryIdentifier = queryIdentifier;
  }

  public InputQuery(String query, String jobId, float slotHours) {
    this.query = query;
    this.queryIdentifier = jobId;
    this.slotHours = slotHours;
  }

  public InputQuery(String query, String jobId, String projectId, float slotHours) {
    this.projectId = projectId;
    this.query = query;
    this.queryIdentifier = jobId;
    this.slotHours = slotHours;;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getQuery() {
    return query;
  }

  public String getQueryId() {
    return queryIdentifier;
  }

  public float getSlotHours() {
    return slotHours;
  }

  public Optional<String> getRegion() {
    return region;
  }

  public Optional<Instant> getStartTime() {
    return startTime;
  }

  public void setRegion(String region) {
    Preconditions.checkNotNull(region);
    this.region = Optional.of(region);
  }

  public void setStartTime(Instant startTime) {
    Preconditions.checkNotNull(startTime);
    this.startTime = Optional.of(startTime);
  }

}
