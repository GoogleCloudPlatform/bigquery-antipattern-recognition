SELECT
  date(t1.trip_start_timestamp) dt,
  COUNT(DISTINCT t1.unique_key) ct,
  SUM(t2.ct) ct_month
FROM
  `bigquery-public-data.chicago_taxi_trips.taxi_trips` t1
LEFT JOIN
  (SELECT
    FORMAT_DATE(\"%Y-%m\", trip_start_timestamp) month,
    COUNT(DISTINCT unique_key) ct
  FROM `bigquery-public-data.chicago_taxi_trips.taxi_trips`
  GROUP BY month
  ) t2
    ON month = FORMAT_DATE(\"%Y-%m\", t1.trip_start_timestamp)
WHERE
  FORMAT_DATE('%Y-%m', t1.trip_start_timestamp) = '2023-02'
  AND t2.month = '2023-02'
GROUP BY
  dt
ORDER BY
  dt desc;