package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Book;
import com.demo2.entity.BookQuery;
import com.demo2.util.LibraryDemo;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 只在显式指定 -Dtest=LocalMySqlIT 时运行；所有写入在本地 MySQL 事务中回滚。
 */
public class LocalMySqlIT {
    private static SqlSessionFactory factory;
    private static int initialBookRows;

    @BeforeClass
    public static void setup() {
        Properties properties = MyBatisUtil.localDatabaseProperties();
        Assert.assertTrue("本地集成验证需要 MySQL JDBC URL",
                properties.getProperty("jdbc.url", "").startsWith("jdbc:mysql:"));
        factory = MyBatisUtil.build(properties, DbType.MYSQL);
        try (SqlSession session = factory.openSession(false)) {
            initialBookRows = scalar(session.getConnection().prepareStatement("SELECT COUNT(*) FROM book"));
            session.rollback();
        } catch (SQLException exception) {
            throw new IllegalStateException("无法读取本地 book 表", exception);
        }
    }

    @AfterClass
    public static void verifyNoBookRowsPersisted() throws Exception {
        try (SqlSession session = factory.openSession(false)) {
            int finalRows = scalar(session.getConnection().prepareStatement("SELECT COUNT(*) FROM book"));
            Assert.assertEquals("本地集成验证不应保留或删除书籍行", initialBookRows, finalRows);
            session.rollback();
        }
    }

    @Test
    public void existingTablesAndQueriesWorkAgainstLocalMySql() throws Exception {
        try (SqlSession session = factory.openSession(false)) {
            Connection connection = session.getConnection();
            for (String table : Arrays.asList("publisher", "book", "book_detail", "author", "book_author")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema = DATABASE() AND table_name = ?")) {
                    statement.setString(1, table);
                    Assert.assertEquals(table + " 表不存在", 1, scalar(statement));
                }
            }

            BookMapper books = session.getMapper(BookMapper.class);
            int active = scalar(connection.prepareStatement(
                    "SELECT COUNT(*) FROM book WHERE deleted = 0"));
            Assert.assertTrue("book 表应有可查询的样例书籍", active > 0);
            Assert.assertEquals(active, books.selectCount(null).intValue());
            Assert.assertEquals(active, books.selectAllBooks().size());
            Page<Book> page = books.selectPage(Page.of(1, 2),
                    Wrappers.<Book>lambdaQuery().orderByAsc(Book::getBookId));
            Assert.assertEquals(active, page.getTotal());
            Assert.assertEquals(Math.min(active, 2), page.getRecords().size());

            Book first = books.selectAllBooks().get(0);
            BookQuery fuzzy = new BookQuery();
            fuzzy.setTitle(first.getTitle().substring(0, 1));
            Assert.assertTrue(books.selectByCondition(fuzzy).stream()
                    .anyMatch(book -> first.getBookId().equals(book.getBookId())));
            Assert.assertNotNull(books.selectWithPublisher(first.getBookId()).getPublisher());
            Assert.assertFalse(books.selectByMap(
                    Map.of("publisher_id", first.getPublisherId())).isEmpty());
            Assert.assertFalse(books.selectMaps(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Book>()
                    .select("category", "COUNT(*) AS book_count")
                    .groupBy("category")).isEmpty());
            System.out.println("本地 MySQL 查询验证通过：5 张表，未删除书籍 " + active + " 本。");
            session.rollback();
        }
    }

    @Test
    public void allTenBasicSqlStatementsMatchMapperOnLocalMySql() throws Exception {
        String script = Files.readString(Paths.get("sql/book-basic-queries.sql"), StandardCharsets.UTF_8);
        List<String> statements = Arrays.stream(script.replaceAll("(?m)^--.*$", "").split(";"))
                .map(String::trim).filter(sql -> !sql.isEmpty()).collect(Collectors.toList());
        Assert.assertEquals("每条基础 SQL 前都应有 USE persistence_lab", 20, statements.size());
        try (SqlSession session = factory.openSession(false);
             Statement statement = session.getConnection().createStatement()) {
            BookMapper mapper = session.getMapper(BookMapper.class);
            for (int number = 1; number <= 10; number++) {
                String use = statements.get((number - 1) * 2);
                Assert.assertEquals("USE PERSISTENCE_LAB", use.toUpperCase(Locale.ROOT));
                statement.execute(use);
                List<String> sqlTitles = new ArrayList<>();
                try (ResultSet rows = statement.executeQuery(statements.get((number - 1) * 2 + 1))) {
                    Assert.assertEquals(number == 2 ? 3 : 11, rows.getMetaData().getColumnCount());
                    while (rows.next()) {
                        sqlTitles.add(rows.getString("title"));
                    }
                }
                List<String> mapperTitles = basicMapperQuery(mapper, number).stream()
                        .map(Book::getTitle).collect(Collectors.toList());
                Assert.assertEquals("基础 SQL " + number + " 与 Mapper 结果不同", sqlTitles, mapperTitles);
            }
            System.out.println("本地 MySQL 的 10 条基础 SQL（含 USE persistence_lab）与 Mapper 对照通过。");
            session.rollback();
        }
    }

    @Test
    public void xmlCrudAndBatchOperationsRollbackOnLocalMySql() throws Exception {
        try (SqlSession session = factory.openSession(false)) {
            BookMapper books = session.getMapper(BookMapper.class);
            int publisherId = firstPublisherId(session.getConnection());
            try {
                Book one = newBook("本地 XML 验证", publisherId);
                Assert.assertEquals(1, books.insertBook(one));
                Assert.assertNotNull("XML 主键回填失败", one.getBookId());
                Book patch = new Book();
                patch.setBookId(one.getBookId());
                patch.setPrice(new BigDecimal("61.00"));
                Assert.assertEquals(1, books.updatePartial(patch));
                Assert.assertEquals(0, new BigDecimal("61.00")
                        .compareTo(books.selectBookById(one.getBookId()).getPrice()));

                String batchPrefix = "本地批量验证-" + System.nanoTime();
                Book two = newBook(batchPrefix + " A", publisherId);
                Book three = newBook(batchPrefix + " B", publisherId);
                Assert.assertTrue(books.insertBatch(Arrays.asList(two, three)) > 0);
                List<Long> batchIds = books.selectPreferred(null, publisherId).stream()
                        .filter(book -> book.getTitle().startsWith(batchPrefix))
                        .map(Book::getBookId).collect(java.util.stream.Collectors.toList());
                Assert.assertEquals(2, batchIds.size());
                Assert.assertEquals(2, books.deleteBatch(batchIds));
                Assert.assertEquals(1, books.deleteBookById(one.getBookId()));
                System.out.println("本地 MySQL XML CRUD、主键回填、动态更新及批量操作验证通过。");
            } finally {
                session.rollback();
            }
        }
    }

    @Test
    public void plusLifecycleRollbackOnLocalMySql() throws Exception {
        long insertedId = -1L;
        try (SqlSession session = factory.openSession(false)) {
            BookMapper books = session.getMapper(BookMapper.class);
            try {
                Book created = newBook("本地 Plus 验证", firstPublisherId(session.getConnection()));
                created.setDiscount(new BigDecimal("5.00"));
                Assert.assertEquals(1, books.insert(created));
                insertedId = created.getBookId();
                Book stored = books.selectById(insertedId);
                Assert.assertNotNull(stored.getCreateTime());
                Assert.assertNotNull(stored.getUpdateTime());
                Assert.assertEquals(Integer.valueOf(0), stored.getVersion());

                Book patch = new Book();
                patch.setBookId(insertedId);
                patch.setTitle("本地 Plus 已更新");
                patch.setDiscount(null);
                Assert.assertEquals(1, books.updateById(patch));
                Assert.assertEquals(0, new BigDecimal("5.00")
                        .compareTo(books.selectById(insertedId).getDiscount()));

                Book first = books.selectById(insertedId);
                session.clearCache();
                Book stale = books.selectById(insertedId);
                first.setPrice(new BigDecimal("60.00"));
                Assert.assertEquals(1, books.updateById(first));
                stale.setPrice(new BigDecimal("70.00"));
                Assert.assertEquals(0, books.updateById(stale));
                Assert.assertEquals(Integer.valueOf(1), books.selectById(insertedId).getVersion());

                Assert.assertEquals(1, books.deleteById(insertedId));
                Assert.assertNull(books.selectById(insertedId));
                try (PreparedStatement statement = session.getConnection().prepareStatement(
                        "SELECT deleted FROM book WHERE book_id = ?")) {
                    statement.setLong(1, insertedId);
                    Assert.assertEquals(1, scalar(statement));
                }
                System.out.println("本地 MySQL 自动填充、NOT_NULL、乐观锁及逻辑删除验证通过。");
            } finally {
                session.rollback();
            }
        }
        try (SqlSession check = factory.openSession(false);
             PreparedStatement statement = check.getConnection().prepareStatement(
                     "SELECT COUNT(*) FROM book WHERE book_id = ?")) {
            statement.setLong(1, insertedId);
            Assert.assertEquals("演示事务未回滚", 0, scalar(statement));
            check.rollback();
        }
    }

    @Test
    public void interactiveMenusExplainRealMySqlResults() throws Exception {
        String input = "2\n9223372036854775807\n6\n7\n8\n9\n10\n11\n12\n0\n";
        try (SqlSession session = factory.openSession(false);
             Scanner scanner = new Scanner(input);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8.name())) {
            LibraryDemo.run(session, scanner, out);
            String output = bytes.toString(StandardCharsets.UTF_8.name());
            Assert.assertTrue(output.contains("编号 9223372036854775807 的书籍不存在"));
            Assert.assertTrue(output.contains("【条件查询】"));
            Assert.assertTrue(output.contains("【聚合查询】"));
            Assert.assertTrue(output.contains("【物理分页】"));
            Assert.assertTrue(output.contains("实体部分更新，discount=null 后数据库折扣仍为 5.00"));
            Assert.assertTrue(output.contains("数据库原始行仍在，deleted=1"));
            Assert.assertTrue(output.contains("旧版本再更新：影响行数=0"));
            Assert.assertEquals(4, output.split("本次演示事务已回滚", -1).length - 1);
            session.rollback();
            System.out.println("本地 MySQL 的中文菜单与不存在编号反馈验证通过。");
        }
    }

    private static Book newBook(String title, int publisherId) {
        Book book = new Book();
        book.setTitle(title);
        book.setCategory("Demo");
        book.setPublishedOn(LocalDate.of(2026, 10, 20));
        book.setPrice(new BigDecimal("50.00"));
        book.setPublisherId(publisherId);
        return book;
    }

    private static List<Book> basicMapperQuery(BookMapper mapper, int number) {
        switch (number) {
            case 1: return mapper.selectAllBooks();
            case 2: return mapper.selectTitlePricePublisher();
            case 3: return mapper.selectPriceAbove100();
            case 4: return mapper.selectPublisher20Or30();
            case 5: return mapper.selectPublishedAfter2021();
            case 6: return mapper.selectWithDiscount();
            case 7: return mapper.selectFiction();
            case 8: return mapper.selectPriceBetween80And200();
            case 9: return mapper.selectWithoutDiscountAndPriceBelow150();
            case 10: return mapper.selectOrderByPriceDesc();
            default: throw new AssertionError(number);
        }
    }

    private static int firstPublisherId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT publisher_id FROM publisher ORDER BY publisher_id LIMIT 1")) {
            try (ResultSet rows = statement.executeQuery()) {
                Assert.assertTrue("publisher 表没有可用的出版社", rows.next());
                return rows.getInt(1);
            }
        }
    }

    private static int scalar(PreparedStatement statement) throws SQLException {
        try (PreparedStatement close = statement;
             ResultSet rows = close.executeQuery()) {
            Assert.assertTrue(rows.next());
            return rows.getInt(1);
        }
    }
}
