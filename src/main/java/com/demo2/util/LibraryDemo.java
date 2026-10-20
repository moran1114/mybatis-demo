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

import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public final class LibraryDemo {
    private LibraryDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.openSession();
             Scanner scanner = new Scanner(System.in)) {
            run(session, scanner, System.out);
        }
    }

    /** 复用已打开的会话，便于在 H2 单元测试中检验菜单反馈。 */
    public static void run(SqlSession session, Scanner scanner, PrintStream out) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(scanner);
        Objects.requireNonNull(out);
        BookMapper books = session.getMapper(BookMapper.class);
        PublisherMapper publishers = session.getMapper(PublisherMapper.class);
        AuthorMapper authors = session.getMapper(AuthorMapper.class);
        while (true) {
            printMenu(out);
            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": {
                    int id = readIntId(scanner, out);
                    showPublisher(publishers.selectWithBooks(id), id, out);
                    break;
                }
                case "2": {
                    long id = readLongId(scanner, out);
                    showBookDetail(books.selectWithDetail(id), id, out);
                    break;
                }
                case "3": {
                    long id = readLongId(scanner, out);
                    showBookPublisher(books.selectWithPublisher(id), id, out);
                    break;
                }
                case "4": {
                    long id = readLongId(scanner, out);
                    showBookAuthors(books.selectWithAuthors(id), id, out);
                    break;
                }
                case "5": {
                    int id = readIntId(scanner, out);
                    showAuthor(authors.selectWithBooks(id), id, out);
                    break;
                }
                case "6":
                    showConditions(books, out);
                    break;
                case "7":
                    showAggregation(books, out);
                    break;
                case "8":
                    showPagination(books, out);
                    break;
                case "9":
                    showAutoFill(session, books, out);
                    break;
                case "10":
                    showFieldStrategy(session, books, out);
                    break;
                case "11":
                    showLogicalDelete(session, books, out);
                    break;
                case "12":
                    showOptimisticLock(session, books, out);
                    break;
                case "0":
                    out.println("已退出演示。");
                    return;
                default:
                    out.println("请输入 0 到 12 之间的菜单编号。");
            }
        }
    }

    private static void printMenu(PrintStream out) {
        out.println("\n=== 图书系统 MyBatis 演示 ===");
        out.println("1 出版社 → 书籍（一对多）");
        out.println("2 书籍 → 详情（一对一）");
        out.println("3 书籍 → 出版社（多对一）");
        out.println("4 书籍 → 作者（多对多）");
        out.println("5 作者 → 书籍（多对多）");
        out.println("6 条件查询（链式条件、OR 嵌套、字段投影；仅作演示）");
        out.println("7 聚合查询（计数、分组、Map 查询；仅作演示）");
        out.println("8 分页查询（仅作演示）");
        out.println("提示：菜单 6–8 使用预设参数展示查询效果，不接收自定义输入。");
        out.println("9 自动填充演示");
        out.println("10 字段策略演示");
        out.println("11 逻辑删除演示");
        out.println("12 乐观锁与冲突处理演示");
        out.println("0 退出");
        out.print("请选择：");
    }

    private static long readLongId(Scanner scanner, PrintStream out) {
        while (true) {
            out.print("请输入书籍编号：");
            try {
                long id = Long.parseLong(scanner.nextLine().trim());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // 重新提示用户输入。
            }
            out.println("编号必须是正整数，请重新输入。");
        }
    }

    private static int readIntId(Scanner scanner, PrintStream out) {
        while (true) {
            out.print("请输入编号：");
            try {
                int id = Integer.parseInt(scanner.nextLine().trim());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // 重新提示用户输入。
            }
            out.println("编号必须是正整数，且不能超过 2147483647。");
        }
    }

    private static void showPublisher(Publisher publisher, int id, PrintStream out) {
        if (publisher == null) {
            out.println("编号 " + id + " 的出版社不存在，请检查 publisher 表的 publisher_id。");
            return;
        }
        out.println(publisher);
        if (publisher.getBooks() == null || publisher.getBooks().isEmpty()) {
            out.println("出版社 " + id + " 暂无书籍。");
        } else {
            publisher.getBooks().forEach(book -> out.println("  " + book));
        }
    }

    private static void showBookDetail(Book book, long id, PrintStream out) {
        if (book == null) {
            printMissingBook(id, out);
            return;
        }
        out.println(book);
        out.println(book.getDetail() == null ? "书籍 " + id + " 暂无详情。" : "  " + book.getDetail());
    }

    private static void showBookPublisher(Book book, long id, PrintStream out) {
        if (book == null) {
            printMissingBook(id, out);
            return;
        }
        out.println(book);
        out.println(book.getPublisher() == null ? "书籍 " + id + " 暂无关联出版社。" : "  " + book.getPublisher());
    }

    private static void showBookAuthors(Book book, long id, PrintStream out) {
        if (book == null) {
            printMissingBook(id, out);
            return;
        }
        out.println(book);
        if (book.getAuthors() == null || book.getAuthors().isEmpty()) {
            out.println("书籍 " + id + " 暂无作者。");
        } else {
            book.getAuthors().forEach(author -> out.println("  " + author));
        }
    }

    private static void showAuthor(Author author, int id, PrintStream out) {
        if (author == null) {
            out.println("编号 " + id + " 的作者不存在，请检查 author 表的 author_id。");
            return;
        }
        out.println(author);
        if (author.getBooks() == null || author.getBooks().isEmpty()) {
            out.println("作者 " + id + " 暂无书籍。");
        } else {
            author.getBooks().forEach(book -> out.println("  " + book));
        }
    }

    private static void printMissingBook(long id, PrintStream out) {
        out.println("编号 " + id + " 的书籍不存在，请检查 book 表的 book_id。");
    }

    private static void showConditions(BookMapper books, PrintStream out) {
        out.println("\n【条件查询】QueryWrapper：出版社 20，且书名含 Java 或价格至少 140。");
        explainReadOnlyDemo(out);
        List<Book> nested = books.selectList(new QueryWrapper<Book>()
                .eq("publisher_id", 20)
                .and(q -> q.like("title", "Java").or().ge("price", new BigDecimal("140")))
                .orderByAsc("book_id"));
        nested.forEach(out::println);
        out.println("LambdaQueryWrapper：类型安全地查询 Technology 且价格至少 100。");
        books.selectList(Wrappers.<Book>lambdaQuery()
                .eq(Book::getCategory, "Technology")
                .ge(Book::getPrice, new BigDecimal("100"))
                .orderByAsc(Book::getBookId)).forEach(out::println);
        out.println("select() 只取编号和书名：");
        books.selectList(Wrappers.<Book>lambdaQuery()
                .select(Book::getBookId, Book::getTitle)
                .orderByAsc(Book::getBookId))
                .forEach(book -> out.println("  编号=" + book.getBookId() + "，书名=" + book.getTitle()));
    }

    private static void showAggregation(BookMapper books, PrintStream out) {
        out.println("\n【聚合查询】");
        explainReadOnlyDemo(out);
        out.println("selectCount：Technology 书籍数量 = "
                + books.selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getCategory, "Technology")));
        out.println("selectMaps：按分类分组，保留至少 2 本的分类：");
        books.selectMaps(new QueryWrapper<Book>()
                .select("category", "COUNT(*) AS book_count")
                .groupBy("category")
                .having("COUNT(*) >= {0}", 2)).forEach(out::println);
        Book one = books.selectOne(Wrappers.<Book>lambdaQuery().eq(Book::getBookId, 1L));
        out.println("selectOne：编号 1 = " + (one == null
                ? "书籍不存在，请检查 book 表的 book_id" : one));
        out.println("selectByMap：publisher_id=20 的书籍：");
        books.selectByMap(Map.of("publisher_id", 20)).forEach(out::println);
    }

    private static void showPagination(BookMapper books, PrintStream out) {
        out.println("\n【物理分页】每页 3 本，按书籍编号升序。");
        explainReadOnlyDemo(out);
        for (int pageNumber = 1; pageNumber <= 2; pageNumber++) {
            Page<Book> page = books.selectPage(new Page<>(pageNumber, 3),
                    Wrappers.<Book>lambdaQuery().orderByAsc(Book::getBookId));
            out.println("第 " + pageNumber + " 页：总数=" + page.getTotal()
                    + "，总页数=" + page.getPages() + "，每页=" + page.getSize());
            page.getRecords().forEach(out::println);
        }
    }

    private static void explainReadOnlyDemo(PrintStream out) {
        out.println("【仅作演示】本项参数已写在示例代码中，不接收自定义输入，也不会修改数据。");
    }

    private static void showAutoFill(SqlSession session, BookMapper books, PrintStream out) {
        out.println("\n【自动填充】MetaObjectHandler 在 INSERT 填 createTime/updateTime，UPDATE 再填 updateTime。");
        try {
            Book created = createDemoBook("自动填充演示书籍", new BigDecimal("5.00"));
            books.insert(created);
            Book inserted = books.selectById(created.getBookId());
            out.println("新增后：createTime=" + inserted.getCreateTime() + "，updateTime=" + inserted.getUpdateTime());
            inserted.setPrice(new BigDecimal("55.00"));
            books.updateById(inserted);
            Book updated = books.selectById(created.getBookId());
            out.println("更新后：createTime=" + updated.getCreateTime() + "，updateTime=" + updated.getUpdateTime());
            out.println("createTime 只在新增时写入；普通实体更新会刷新 updateTime。"
                    + "如果两次操作发生在同一秒，数据库显示的时间可能相同。");
        } finally {
            rollbackDemo(session, out);
        }
    }

    private static void showFieldStrategy(SqlSession session, BookMapper books, PrintStream out) {
        out.println("\n【字段策略】discount 的 updateStrategy=NOT_NULL：实体属性为 null 时，UPDATE 不写这个字段。");
        try {
            Book created = createDemoBook("字段策略演示书籍", new BigDecimal("5.00"));
            books.insert(created);
            Book patch = new Book();
            patch.setBookId(created.getBookId());
            patch.setTitle("字段策略已更新");
            patch.setDiscount(null);
            books.updateById(patch);
            out.println("实体部分更新，discount=null 后数据库折扣仍为 "
                    + books.selectById(created.getBookId()).getDiscount());
            books.update(null, Wrappers.<Book>lambdaUpdate()
                    .eq(Book::getBookId, created.getBookId())
                    .set(Book::getDiscount, null));
            out.println("明确要清空折扣时，LambdaUpdateWrapper.set(discount, null) 后折扣为 "
                    + books.selectById(created.getBookId()).getDiscount());
            out.println("wrapper 直接更新不带实体，不能把它当作自动填充或乐观锁演示。");
        } finally {
            rollbackDemo(session, out);
        }
    }

    private static void showLogicalDelete(SqlSession session, BookMapper books, PrintStream out) {
        out.println("\n【逻辑删除】@TableLogic 把 deleteById 改写为 deleted=1；普通查询自动加 deleted=0。");
        try {
            Book created = createDemoBook("逻辑删除演示书籍", new BigDecimal("5.00"));
            books.insert(created);
            long id = created.getBookId();
            out.println("删除影响行数=" + books.deleteById(id));
            out.println("BaseMapper.selectById(" + id + ") = " + books.selectById(id));
            try (PreparedStatement statement = session.getConnection()
                    .prepareStatement("SELECT deleted FROM book WHERE book_id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        out.println("数据库原始行仍在，deleted=" + rows.getInt(1));
                    } else {
                        out.println("数据库原始行不存在，请检查 book 表的 book_id=" + id);
                    }
                }
            } catch (SQLException e) {
                out.println("读取数据库原始标记失败：" + e.getMessage());
            }
            out.println("自定义 XML SQL 不会自动加逻辑删除条件，编写时需显式限定 deleted=0。");
        } finally {
            rollbackDemo(session, out);
        }
    }

    private static void showOptimisticLock(SqlSession session, BookMapper books, PrintStream out) {
        out.println("\n【乐观锁】@Version 更新时校验 WHERE version=旧值，成功后版本加 1。");
        try {
            Book created = createDemoBook("乐观锁演示书籍", new BigDecimal("5.00"));
            books.insert(created);
            long id = created.getBookId();
            Book first = books.selectById(id);
            session.clearCache();
            Book stale = books.selectById(id);
            out.println("两个读取者拿到的旧版本都是 " + first.getVersion());
            first.setPrice(new BigDecimal("60.00"));
            int firstRows = books.updateById(first);
            out.println("先更新：影响行数=" + firstRows + "，当前版本=" + books.selectById(id).getVersion());
            stale.setPrice(new BigDecimal("70.00"));
            int staleRows = books.updateById(stale);
            out.println("旧版本再更新：影响行数=" + staleRows + "（0 表示版本冲突，不能当成功）");
            Book latest = books.selectById(id);
            latest.setPrice(new BigDecimal("70.00"));
            int retryRows = books.updateById(latest);
            out.println("重新读取后重试：影响行数=" + retryRows
                    + "，当前版本=" + books.selectById(id).getVersion());
        } finally {
            rollbackDemo(session, out);
        }
    }

    private static Book createDemoBook(String title, BigDecimal discount) {
        Book book = new Book();
        book.setTitle(title);
        book.setCategory("Demo");
        book.setPublishedOn(LocalDate.now());
        book.setPrice(new BigDecimal("50.00"));
        book.setDiscount(discount);
        book.setPublisherId(20);
        return book;
    }

    private static void rollbackDemo(SqlSession session, PrintStream out) {
        session.rollback();
        session.clearCache();
        out.println("本次演示事务已回滚，不会改动示例数据。");
    }
}
