DROP TABLE IF EXISTS book_author;
DROP TABLE IF EXISTS book_detail;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS author;
DROP TABLE IF EXISTS publisher;

CREATE TABLE publisher (
    publisher_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(80) NOT NULL
);

CREATE TABLE book (
    book_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    category VARCHAR(60) NOT NULL,
    published_on DATE NOT NULL,
    price DECIMAL(12, 2) NOT NULL CHECK (price >= 0),
    discount DECIMAL(12, 2) NULL CHECK (discount IS NULL OR discount >= 0),
    publisher_id INT NOT NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_book_publisher FOREIGN KEY (publisher_id) REFERENCES publisher(publisher_id)
);
CREATE INDEX idx_book_publisher ON book(publisher_id, deleted);
CREATE INDEX idx_book_category ON book(category, deleted);

CREATE TABLE book_detail (
    book_id BIGINT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    language VARCHAR(40) NOT NULL,
    summary VARCHAR(500),
    page_count INT NOT NULL CHECK (page_count > 0),
    CONSTRAINT fk_book_detail_book FOREIGN KEY (book_id) REFERENCES book(book_id)
);

CREATE TABLE author (
    author_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(60) NOT NULL,
    birth_date DATE
);

CREATE TABLE book_author (
    book_id BIGINT NOT NULL,
    author_id INT NOT NULL,
    contribution VARCHAR(40) NOT NULL,
    PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_author_book FOREIGN KEY (book_id) REFERENCES book(book_id),
    CONSTRAINT fk_book_author_author FOREIGN KEY (author_id) REFERENCES author(author_id)
);
CREATE INDEX idx_book_author_author ON book_author(author_id);
