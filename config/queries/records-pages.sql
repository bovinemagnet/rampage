-- Generates one row per page that exists when the `items` table is
-- paginated at pageSize=100. The feeder column `pageNumber` is then used as
-- the GraphQL Records($pageNumber) variable, so each request hits a real
-- page that holds at least one record.
--
-- If the API uses a different default page size, change both the divisor
-- below and `pageSize` in scenarios/records-page.yaml to match.
--
-- If you'd rather hammer just the first N pages, replace the body of the
-- subquery with `generate_series(0, N - 1)`.

SELECT page_number AS "pageNumber"
FROM generate_series(
       0,
       GREATEST(((SELECT COUNT(*) FROM items) / 100) - 1, 0)
     ) AS page_number;
