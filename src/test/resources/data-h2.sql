INSERT INTO publisher (publisher_id, name, city) VALUES
    (10, 'Northern Press', 'Beijing'),
    (20, 'Tech House', 'Shanghai'),
    (30, 'River Books', 'Shenzhen'),
    (40, 'Open Shelf', 'Hangzhou'),
    (50, 'Maple Press', 'Chengdu');

INSERT INTO book (book_id, title, category, published_on, price, discount, publisher_id) VALUES
    (1, 'Java Foundations', 'Technology', '2023-10-09', 145.00, NULL, 20),
    (2, 'City of Glass', 'Fiction', '2020-01-01', 80.00, 15.00, 30),
    (3, 'SQL Handbook', 'Technology', '2022-08-08', 100.00, NULL, 20),
    (4, 'Night Market', 'Fiction', '2019-05-20', 70.00, 0.00, 30),
    (5, 'Ledger Basics', 'Finance', '2020-03-11', 150.00, NULL, 10),
    (6, 'Cloud Atlas', 'Technology', '2021-11-23', 220.00, 30.00, 20),
    (7, 'River Stories', 'Fiction', '2024-06-01', 200.00, 20.00, 30),
    (8, 'Pocket Guide', 'Finance', '2024-07-15', 75.00, NULL, 10);

INSERT INTO book_detail (book_id, isbn, language, summary, page_count) VALUES
    (1, '9780000000001', 'English', 'An introduction to Java programming.', 420),
    (2, '9780000000002', 'English', 'Stories of a changing city.', 280),
    (3, '9780000000003', 'English', 'Practical SQL examples.', 310),
    (4, '9780000000004', 'English', 'A city mystery.', 260),
    (5, '9780000000005', 'English', 'Accounting principles.', 360),
    (6, '9780000000006', 'English', 'Distributed systems in practice.', 510),
    (7, '9780000000007', 'English', 'Stories by the river.', 330);

INSERT INTO author (author_id, name, country, birth_date) VALUES
    (101, 'Lin Han', 'China', '1980-03-01'),
    (102, 'Tang Ke', 'China', '1985-09-16'),
    (103, 'Xu Shu', 'China', '1978-12-03');

INSERT INTO book_author (book_id, author_id, contribution) VALUES
    (1, 101, 'Author'),
    (1, 102, 'Co-author'),
    (2, 103, 'Author'),
    (3, 101, 'Author'),
    (3, 103, 'Co-author'),
    (6, 101, 'Author'),
    (6, 102, 'Co-author'),
    (7, 103, 'Author');
