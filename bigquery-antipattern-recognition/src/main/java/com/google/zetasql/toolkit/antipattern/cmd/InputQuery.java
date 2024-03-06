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

public class InputQuery {

  private String query = null;
  private String queryIdentifier  = null;
  private String projectId = null;
  private String userEmail = null;
  private String optimizedQuery = null;
  private float slotHours = -1;

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

  public InputQuery(String query, String jobId, String projectId, String userEmail, float slotHours) {
    this.projectId = projectId;
    this.query = query;
    this.queryIdentifier = jobId;
    this.userEmail = userEmail;
    this.slotHours = slotHours;
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

  public String getUserEmail() {
    return userEmail;
  }

  public float getSlotHours() {
    return slotHours;
  }

  public String getOptimizedQuery() {
    return optimizedQuery;
  }

  public void setOptimizedQuery(String optimizedQuery) {
    this.optimizedQuery = optimizedQuery;
  }
}
