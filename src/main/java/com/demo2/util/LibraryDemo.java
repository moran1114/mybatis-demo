package com.demo2.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Author;
import com.demo2.entity.Book;
import com.demo2.entity.Publisher;
import com.demo2.mapper.AuthorMapper;
import com.demo2.mapper.BookMapper;
import com.demo2.mapper.PublisherMapper;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Scanner;

public final class LibraryDemo {
    private LibraryDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.openSession();
             Scanner scanner = new Scanner(System.in)) {
            BookMapper books = session.getMapper(BookMapper.class);
            PublisherMapper publishers = session.getMapper(PublisherMapper.class);
            AuthorMapper authors = session.getMapper(AuthorMapper.class);
            while (true) {
                System.out.println("\n=== 图书系统 MyBatis 演示 ===");
                System.out.println("1 出版社 → 书籍（一对多）");
                System.out.println("2 书籍 → 详情（一对一）");
                System.out.println("3 书籍 → 出版社（多对一）");
                System.out.println("4 书籍 → 作者（多对多）");
                System.out.println("5 作者 → 书籍（多对多）");
                System.out.println("6 条件、聚合与分页查询");
                System.out.println("7 自动填充、字段策略、逻辑删除与乐观锁");
                System.out.println("0 退出");
                System.out.print("请选择：");
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        showPublisher(publishers.selectWithBooks((int) readId(scanner)));
                        break;
                    case "2":
                        showBookDetail(books.selectWithDetail(readId(scanner)));
                        break;
                    case "3":
                        showBookPublisher(books.selectWithPublisher(readId(scanner)));
                        break;
                    case "4":
                        showBookAuthors(books.selectWithAuthors(readId(scanner)));
                        break;
                    case "5":
                        showAuthor(authors.selectWithBooks((int) readId(scanner)));
                        break;
                    case "6":
                        showAdvancedQueries(books);
                        break;
                    case "7":
                        showLifecycle(session, books);
                        break;
                    case "0":
                        return;
                    default:
                        System.out.println("请输入 0 到 7 之间的选项。");
                }
            }
        }
    }

    private static long readId(Scanner scanner) {
        while (true) {
            System.out.print("请输入编号：");
            try {
                long id = Long.parseLong(scanner.nextLine().trim());
                if (id > 0 && id <= Integer.MAX_VALUE) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // Continue until a valid number is entered.
            }
            System.out.println("请输入正整数编号。");
        }
    }

    private static void showPublisher(Publisher publisher) {
        if (publisher == null) {
            System.out.println("未找到出版社。");
            return;
        }
        System.out.println(publisher);
        publisher.getBooks().forEach(book -> System.out.println("  " + book));
    }

    private static void showBookDetail(Book book) {
        if (book == null) {
            System.out.println("未找到书籍。");
            return;
        }
        System.out.println(book);
        System.out.println(book.getDetail() == null ? "  暂无详情" : "  " + book.getDetail());
    }

    private static void showBookPublisher(Book book) {
        if (book == null) {
            System.out.println("未找到书籍。");
            return;
        }
        System.out.println(book);
        System.out.println("  " + book.getPublisher());
    }

    private static void showBookAuthors(Book book) {
        if (book == null) {
            System.out.println("未找到书籍。");
            return;
        }
        System.out.println(book);
        book.getAuthors().forEach(author -> System.out.println("  " + author));
    }

    private static void showAuthor(Author author) {
        if (author == null) {
            System.out.println("未找到作者。");
            return;
        }
        System.out.println(author);
        author.getBooks().forEach(book -> System.out.println("  " + book));
    }

    private static void showAdvancedQueries(BookMapper books) {
        System.out.println("技术类书籍，价格至少 140 或标题为 SQL Handbook：");
        books.selectList(Wrappers.<Book>lambdaQuery()
                .eq(Book::getCategory, "Technology")
                .and(q -> q.ge(Book::getPrice, new BigDecimal("140"))
                        .or().eq(Book::getTitle, "SQL Handbook")))
                .forEach(System.out::println);
        System.out.println("只查书名：" + books.selectList(Wrappers.<Book>lambdaQuery()
                .select(Book::getBookId, Book::getTitle).orderByAsc(Book::getBookId)));
        System.out.println("按分类统计：" + books.selectMaps(new QueryWrapper<Book>()
                .select("category", "COUNT(*) AS book_count").groupBy("category")));
        System.out.println("技术类书籍数量：" + books.selectCount(Wrappers.<Book>lambdaQuery()
                .eq(Book::getCategory, "Technology")));
        System.out.println("书籍 1：" + books.selectOne(Wrappers.<Book>lambdaQuery()
                .eq(Book::getBookId, 1L)));
        Page<Book> page = books.selectPage(new Page<>(1, 3),
                Wrappers.<Book>lambdaQuery().orderByAsc(Book::getBookId));
        System.out.println("第一页：总数=" + page.getTotal() + "，记录=" + page.getRecords());
        System.out.println("出版社 20：" + books.selectByMap(Map.of("publisher_id", 20)));
    }

    private static void showLifecycle(SqlSession session, BookMapper books) {
        try {
            Book created = new Book();
            created.setTitle("事务演示书籍");
            created.setCategory("Technology");
            created.setPublishedOn(LocalDate.now());
            created.setPrice(new BigDecimal("50.00"));
            created.setDiscount(new BigDecimal("5.00"));
            created.setPublisherId(20);
            books.insert(created);
            System.out.println("新增并自动填充：" + books.selectById(created.getBookId()));

            Book current = books.selectById(created.getBookId());
            session.clearCache();
            Book stale = books.selectById(created.getBookId());
            current.setTitle("已更新的书籍");
            current.setDiscount(null);
            books.updateById(current);
            System.out.println("更新后折扣仍是 5.00：" + books.selectById(created.getBookId()).getDiscount());
            stale.setPrice(new BigDecimal("60.00"));
            System.out.println("旧版本更新影响行数：" + books.updateById(stale));
            Book reloaded = books.selectById(created.getBookId());
            reloaded.setPrice(new BigDecimal("60.00"));
            System.out.println("重新读取后更新影响行数：" + books.updateById(reloaded));
            books.deleteById(created.getBookId());
            System.out.println("逻辑删除后查询：" + books.selectById(created.getBookId()));
        } finally {
            session.rollback();
            session.clearCache();
            System.out.println("演示事务已回滚。");
        }
    }
}
