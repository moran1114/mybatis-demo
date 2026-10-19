package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Book;
import com.demo2.handler.AuditMetaObjectHandler;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookLifecycleTest {
    private static final Instant INSERT_TIME = Instant.parse("2025-01-01T10:00:00Z");
    private static SqlSessionFactory factory;
    private SqlSession session;
    private BookMapper mapper;

    @BeforeClass
    public static void setup() {
        EmbeddedDatabase.initialize("book_lifecycle");
        factory = MyBatisUtil.build(EmbeddedDatabase.properties("book_lifecycle"), DbType.H2);
    }

    @Before
    public void openSession() {
        useClock(INSERT_TIME);
        session = factory.openSession(false);
        mapper = session.getMapper(BookMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void insertFillsAuditTimesAndUsesDefaultFlags() {
        Book book = insertBook("New Book");
        LocalDateTime expected = LocalDateTime.ofInstant(INSERT_TIME, ZoneOffset.UTC);
        Assert.assertEquals(expected, book.getCreateTime());
        Assert.assertEquals(expected, book.getUpdateTime());
        Book stored = mapper.selectById(book.getBookId());
        Assert.assertEquals(expected, stored.getCreateTime());
        Assert.assertEquals(expected, stored.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(0), stored.getDeleted());
        Assert.assertEquals(Integer.valueOf(0), stored.getVersion());
    }

    @Test
    public void updateRefreshesTimeWithoutChangingCreationTime() {
        Book book = mapper.selectById(insertBook("Updated Book").getBookId());
        LocalDateTime created = book.getCreateTime();
        useClock(INSERT_TIME.plusSeconds(60));
        book.setCreateTime(created.minusYears(1));
        book.setPrice(new BigDecimal("130.00"));
        Assert.assertEquals(1, mapper.updateById(book));
        Book stored = mapper.selectById(book.getBookId());
        Assert.assertEquals(created, stored.getCreateTime());
        Assert.assertEquals(created.plusSeconds(60), stored.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
    }

    @Test
    public void logicalDeleteKeepsRowButHidesItFromGeneratedAndCustomQueries() throws Exception {
        Long id = insertBook("Deleted Book").getBookId();
        Assert.assertEquals(1, mapper.deleteById(id));
        Assert.assertNull(mapper.selectById(id));
        Assert.assertNull(mapper.selectBookById(id));
        Assert.assertNull(mapper.selectWithPublisher(id));
        Assert.assertTrue(mapper.selectByMap(Map.of("book_id", id)).isEmpty());
        Assert.assertEquals(Long.valueOf(0), mapper.selectCount(
                Wrappers.lambdaQuery(Book.class).eq(Book::getCategory, "Demo")));
        Assert.assertEquals(0, mapper.deleteById(id));
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT deleted, version FROM book WHERE book_id = ?")) {
            statement.setLong(1, id);
            try (ResultSet row = statement.executeQuery()) {
                Assert.assertTrue(row.next());
                Assert.assertEquals(1, row.getInt("deleted"));
                Assert.assertEquals(0, row.getInt("version"));
            }
        }
    }

    @Test
    public void optimisticLockRejectsStaleSnapshotThenAllowsReloadedUpdate() {
        Long id = insertBook("Concurrent Book").getBookId();
        Book first = mapper.selectById(id);
        session.clearCache();
        Book stale = mapper.selectById(id);
        first.setPrice(new BigDecimal("130.00"));
        Assert.assertEquals(1, mapper.updateById(first));
        stale.setPrice(new BigDecimal("140.00"));
        Assert.assertEquals(0, mapper.updateById(stale));
        Book current = mapper.selectById(id);
        Assert.assertEquals(new BigDecimal("130.00"), current.getPrice());
        Assert.assertEquals(Integer.valueOf(1), current.getVersion());
        current.setTitle("Reviewed Title");
        Assert.assertEquals(1, mapper.updateById(current));
        Assert.assertEquals(Integer.valueOf(2), mapper.selectById(id).getVersion());
        Assert.assertEquals(new BigDecimal("130.00"), mapper.selectById(id).getPrice());
    }

    @Test
    public void entityWrapperChecksVersionAndFillsTimeButWrapperOnlyDoesNot() {
        Long id = insertBook("Wrapper Book").getBookId();
        Book patch = new Book();
        patch.setVersion(0);
        patch.setPrice(new BigDecimal("135.00"));
        useClock(INSERT_TIME.plusSeconds(120));
        Assert.assertEquals(1, mapper.update(patch, Wrappers.lambdaUpdate(Book.class)
                .eq(Book::getBookId, id)));
        Book stored = mapper.selectById(id);
        Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
        Assert.assertEquals(stored.getCreateTime().plusSeconds(120), stored.getUpdateTime());

        Book stale = new Book();
        stale.setVersion(0);
        stale.setPrice(new BigDecimal("999.00"));
        Assert.assertEquals(0, mapper.update(stale, Wrappers.lambdaUpdate(Book.class)
                .eq(Book::getBookId, id)));

        useClock(INSERT_TIME.plusSeconds(180));
        Assert.assertEquals(1, mapper.update(null, Wrappers.lambdaUpdate(Book.class)
                .eq(Book::getBookId, id).set(Book::getPrice, new BigDecimal("150.00"))));
        stored = mapper.selectById(id);
        Assert.assertEquals(stored.getCreateTime().plusSeconds(120), stored.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
    }

    @Test
    public void paginationCountsOnlyActiveBooksAndCapsPageSize() {
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            ids.add(insertBook("Page " + index).getBookId());
        }
        mapper.deleteById(ids.remove(1));
        Page<Book> first = page(1, 2);
        Page<Book> second = page(2, 2);
        Assert.assertEquals(4L, first.getTotal());
        Assert.assertEquals(2L, first.getPages());
        Assert.assertEquals(ids.subList(0, 2), bookIds(first));
        Assert.assertEquals(ids.subList(2, 4), bookIds(second));
        Assert.assertTrue(page(3, 2).getRecords().isEmpty());
        Assert.assertEquals(100L, page(1, 1000).getSize());
    }

    private Book insertBook(String title) {
        Book book = new Book();
        book.setTitle(title);
        book.setCategory("Demo");
        book.setPublishedOn(LocalDate.of(2025, 1, 1));
        book.setPrice(new BigDecimal("120.00"));
        book.setPublisherId(20);
        Assert.assertEquals(1, mapper.insert(book));
        return book;
    }

    private Page<Book> page(long current, long size) {
        return mapper.selectPage(Page.of(current, size), Wrappers.lambdaQuery(Book.class)
                .eq(Book::getCategory, "Demo").orderByAsc(Book::getBookId));
    }

    private static List<Long> bookIds(Page<Book> page) {
        return page.getRecords().stream().map(Book::getBookId).collect(Collectors.toList());
    }

    private static void useClock(Instant instant) {
        GlobalConfigUtils.getGlobalConfig(factory.getConfiguration())
                .setMetaObjectHandler(new AuditMetaObjectHandler(Clock.fixed(instant, ZoneOffset.UTC)));
    }
}
