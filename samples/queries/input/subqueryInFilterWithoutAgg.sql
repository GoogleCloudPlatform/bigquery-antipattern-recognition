SELECT 
  *
FROM 
`bigquery-public-data`.stackoverflow.comments c
WHERE 
  c.id in (
  SELECT id 
  FROM `bigquery-public-data`.stackoverflow.users 
); 