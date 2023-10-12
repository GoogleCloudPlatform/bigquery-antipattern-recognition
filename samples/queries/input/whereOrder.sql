SELECT
  repo_name,
  id,
  ref
FROM
  `bigquery-public-data.github_repos.files`
WHERE
  ref like '%master%'
  and repo_name = 'cdnjs/cdnjs'
;