-- 单独执行建表或插入数据块时，请同时选中它前面的 USE 语句。
CREATE DATABASE IF NOT EXISTS persistence_lab
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE persistence_lab;

DROP TABLE IF EXISTS emp_project;
DROP TABLE IF EXISTS emp_detail;
DROP TABLE IF EXISTS emp_lifecycle;
DROP TABLE IF EXISTS emp;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS dept;

DROP TABLE IF EXISTS book_author;
DROP TABLE IF EXISTS book_detail;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS author;
DROP TABLE IF EXISTS publisher;

USE persistence_lab;
CREATE TABLE publisher (
    publisher_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

USE persistence_lab;
CREATE TABLE book (
    book_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    category VARCHAR(60) NOT NULL,
    published_on DATE NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    discount DECIMAL(12, 2) NULL,
    publisher_id INT NOT NULL,
    create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_book_publisher FOREIGN KEY (publisher_id) REFERENCES publisher(publisher_id),
    CONSTRAINT chk_book_price CHECK (price >= 0),
    CONSTRAINT chk_book_discount CHECK (discount IS NULL OR discount >= 0),
    CONSTRAINT chk_book_deleted CHECK (deleted IN (0, 1)),
    INDEX idx_book_publisher (publisher_id, deleted),
    INDEX idx_book_category (category, deleted)
) ENGINE=InnoDB;

USE persistence_lab;
CREATE TABLE book_detail (
    book_id BIGINT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    language VARCHAR(40) NOT NULL,
    summary VARCHAR(500),
    page_count INT NOT NULL,
    CONSTRAINT fk_book_detail_book FOREIGN KEY (book_id) REFERENCES book(book_id),
    CONSTRAINT chk_book_detail_page_count CHECK (page_count > 0)
) ENGINE=InnoDB;

USE persistence_lab;
CREATE TABLE author (
    author_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(60) NOT NULL,
    birth_date DATE
) ENGINE=InnoDB;

USE persistence_lab;
CREATE TABLE book_author (
    book_id BIGINT NOT NULL,
    author_id INT NOT NULL,
    contribution VARCHAR(40) NOT NULL,
    PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_author_book FOREIGN KEY (book_id) REFERENCES book(book_id),
    CONSTRAINT fk_book_author_author FOREIGN KEY (author_id) REFERENCES author(author_id),
    INDEX idx_book_author_author (author_id)
) ENGINE=InnoDB;

USE persistence_lab;
INSERT INTO publisher (publisher_id, name, city) VALUES
    (10, 'Northern Press', 'Beijing'),
    (20, 'Tech House', 'Shanghai'),
    (30, 'River Books', 'Shenzhen'),
    (40, 'Open Shelf', 'Hangzhou'),
    (50, 'Maple Press', 'Chengdu');

USE persistence_lab;
INSERT INTO book (book_id, title, category, published_on, price, discount, publisher_id) VALUES
    (1, 'Java Foundations', 'Technology', '2023-10-09', 145.00, NULL, 20),
    (2, 'City of Glass', 'Fiction', '2020-01-01', 80.00, 15.00, 30),
    (3, 'SQL Handbook', 'Technology', '2022-08-08', 100.00, NULL, 20),
    (4, 'Night Market', 'Fiction', '2019-05-20', 70.00, 0.00, 30),
    (5, 'Ledger Basics', 'Finance', '2020-03-11', 150.00, NULL, 10),
    (6, 'Cloud Atlas', 'Technology', '2021-11-23', 220.00, 30.00, 20),
    (7, 'River Stories', 'Fiction', '2024-06-01', 200.00, 20.00, 30),
    (8, 'Pocket Guide', 'Finance', '2024-07-15', 75.00, NULL, 10);

USE persistence_lab;
INSERT INTO book_detail (book_id, isbn, language, summary, page_count) VALUES
    (1, '9780000000001', 'English', 'An introduction to Java programming.', 420),
    (2, '9780000000002', 'English', 'Stories of a changing city.', 280),
    (3, '9780000000003', 'English', 'Practical SQL examples.', 310),
    (4, '9780000000004', 'English', 'A city mystery.', 260),
    (5, '9780000000005', 'English', 'Accounting principles.', 360),
    (6, '9780000000006', 'English', 'Distributed systems in practice.', 510),
    (7, '9780000000007', 'English', 'Stories by the river.', 330);

USE persistence_lab;
INSERT INTO author (author_id, name, country, birth_date) VALUES
    (101, 'Lin Han', 'China', '1980-03-01'),
    (102, 'Tang Ke', 'China', '1985-09-16'),
    (103, 'Xu Shu', 'China', '1978-12-03');

USE persistence_lab;
INSERT INTO book_author (book_id, author_id, contribution) VALUES
    (1, 101, 'Author'),
    (1, 102, 'Co-author'),
    (2, 103, 'Author'),
    (3, 101, 'Author'),
    (3, 103, 'Co-author'),
    (6, 101, 'Author'),
    (6, 102, 'Co-author'),
    (7, 103, 'Author');
