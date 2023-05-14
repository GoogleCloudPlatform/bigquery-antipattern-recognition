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