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

WITH A AS (
 SELECT
   reference_bases,
   start_position,
   count(1) ct
 FROM
   `bigquery-public-data.human_genome_variants.1000_genomes_phase_3_optimized_schema_variants_20150220`
 WHERE
   reference_bases in ('AT', 'TA', 'AT')
 GROUP BY
   1, 2
),
B as (
 SELECT
   reference_bases, 
   count(1) ct
 FROM
   A
 GROUP BY
   1
)
SELECT
 A.*,
 A.ct / B.ct
FROM
 A
JOIN
 B ON B.reference_bases = A.reference_bases
