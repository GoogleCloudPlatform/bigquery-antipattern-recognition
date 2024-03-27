/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

SELECT
  taxi_id, trip_seconds, fare
FROM
  (
  SELECT
    taxi_id, trip_seconds, fare,
    row_number() over(partition by taxi_id order by fare desc) rn
  FROM
    `bigquery-public-data.chicago_taxi_trips.taxi_trips`
)
WHERE
  rn = 1;
