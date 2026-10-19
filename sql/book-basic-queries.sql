USE persistence_lab;

-- 1. 查询所有书籍及全部字段。
SELECT * FROM book WHERE deleted = 0 ORDER BY book_id;

-- 2. 只查询书名、价格和出版社编号。
SELECT title, price, publisher_id FROM book WHERE deleted = 0 ORDER BY book_id;

-- 3. 查询价格高于 100 的书籍。
SELECT * FROM book WHERE deleted = 0 AND price > 100 ORDER BY book_id;

-- 4. 查询出版社 20 或 30 的书籍。
SELECT * FROM book WHERE deleted = 0 AND (publisher_id = 20 OR publisher_id = 30) ORDER BY book_id;

-- 5. 查询 2021-01-01 后出版的书籍。
SELECT * FROM book WHERE deleted = 0 AND published_on > '2021-01-01' ORDER BY book_id;

-- 6. 查询设置了折扣的书籍，包括折扣为 0 的书籍。
SELECT * FROM book WHERE deleted = 0 AND discount IS NOT NULL ORDER BY book_id;

-- 7. 查询小说类书籍。
SELECT * FROM book WHERE deleted = 0 AND category = 'Fiction' ORDER BY book_id;

-- 8. 查询价格在 80 到 200 之间的书籍（包含边界）。
SELECT * FROM book WHERE deleted = 0 AND price BETWEEN 80 AND 200 ORDER BY book_id;

-- 9. 查询没有折扣且价格低于 150 的书籍。
SELECT * FROM book WHERE deleted = 0 AND discount IS NULL AND price < 150 ORDER BY book_id;

-- 10. 按价格从高到低排序；相同价格再按主键排序。
SELECT * FROM book WHERE deleted = 0 ORDER BY price DESC, book_id;
