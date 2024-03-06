SELECT 
  *
FROM 
`bigquery-public-data`.stackoverflow.comments c
WHERE 
  c.id = (
  SELECT max(id) 
  FROM `bigquery-public-data`.stackoverflow.users 
);
