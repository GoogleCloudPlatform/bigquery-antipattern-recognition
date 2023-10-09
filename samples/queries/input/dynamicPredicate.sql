SELECT
 *
FROM
  comments c
JOIN
  users u ON c.user_id = u.id
WHERE
  u.id IN (
    SELECT id
    FROM users
    WHERE location LIKE '%New York'
    GROUP BY id
    ORDER BY SUM(up_votes) DESC
    LIMIT 10
  )
;