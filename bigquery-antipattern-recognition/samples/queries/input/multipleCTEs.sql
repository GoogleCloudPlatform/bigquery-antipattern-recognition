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
