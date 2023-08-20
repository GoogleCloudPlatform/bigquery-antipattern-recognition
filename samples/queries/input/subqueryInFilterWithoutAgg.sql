select
	col1
from
	table1 
where
	col2 not in (select col2 from table2)
;

