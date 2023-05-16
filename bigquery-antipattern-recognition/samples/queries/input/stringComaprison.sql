select
  t1.title
from
  `bigquery-public-data.samples.wikipedia` t1
where
  regexp_contains(title, '.*shakespeare.*')
