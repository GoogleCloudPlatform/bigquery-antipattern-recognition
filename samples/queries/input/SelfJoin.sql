SELECT
  t1.*,
  t2.ct ct_prev_day
FROM
  `bigquery-public-data.chicago_taxi_trips.taxi_trips` t1
LEFT JOIN
  `bigquery-public-data.chicago_taxi_trips.taxi_trips` t2
  ON
    t1.taxi_id = t2.taxi_id
    AND DATE(t2.dt) = DATE(t1.dt) - INTERVAL 1 DAY;