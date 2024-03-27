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
  t1.station_id,
  COUNT(1) num_trips_started
FROM
  `bigquery-public-data.austin_bikeshare.bikeshare_stations` t1
JOIN
  `bigquery-public-data.austin_bikeshare.bikeshare_trips` t2 ON t1.station_id = t2.start_station_id
GROUP BY
  t1.station_id
;