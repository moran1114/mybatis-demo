package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Author;
import com.demo2.entity.Book;
import com.demo2.entity.Publisher;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LibraryMapperTest {
    private static SqlSessionFactory factory;
    private SqlSession session;
    private BookMapper bookMapper;
    private PublisherMapper publisherMapper;
    private AuthorMapper authorMapper;

    @BeforeClass
    public static void createDatabase() {
        EmbeddedDatabase.initialize("library_mapper");
        factory = MyBatisUtil.build(EmbeddedDatabase.properties("library_mapper"), DbType.H2);
    }

    @Before
    public void openSession() {
        session = factory.openSession(false);
        bookMapper = session.getMapper(BookMapper.class);
        publisherMapper = session.getMapper(PublisherMapper.class);
        authorMapper = session.getMapper(AuthorMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void publisherHasManyBooksAndCanHaveNone() {
        Publisher publisher = publisherMapper.selectWithBooks(20);
        Assert.assertEquals("Tech House", publisher.getName());
        Assert.assertEquals(Arrays.asList(1L, 3L, 6L),
                publisher.getBooks().stream().map(Book::getBookId).collect(Collectors.toList()));
        publisher.getBooks().forEach(book -> Assert.assertEquals(Integer.valueOf(20), book.getPublisherId()));

        Assert.assertTrue(publisherMapper.selectWithBooks(40).getBooks().isEmpty());
        Assert.assertNull(publisherMapper.selectWithBooks(99));
    }

    @Test
    public void bookHasOptionalOneToOneDetail() {
        Book book = bookMapper.selectWithDetail(1L);
        Assert.assertEquals("Java Foundations", book.getTitle());
        Assert.assertEquals(Long.valueOf(1), book.getDetail().getBookId());
        Assert.assertEquals("9780000000001", book.getDetail().getIsbn());
        Assert.assertEquals(Integer.valueOf(420), book.getDetail().getPageCount());
        Assert.assertNull(bookMapper.selectWithDetail(8L).getDetail());
        Assert.assertNull(bookMapper.selectWithDetail(99L));
    }

    @Test
    public void bookBelongsToPublisher() {
        Book book = bookMapper.selectWithPublisher(3L);
        Assert.assertEquals(Integer.valueOf(20), book.getPublisherId());
        Assert.assertEquals(Integer.valueOf(20), book.getPublisher().getPublisherId());
        Assert.assertEquals("Tech House", book.getPublisher().getName());
    }

    @Test
    public void authorsAndBooksResolveManyToManyInBothDirections() {
        Book book = bookMapper.selectWithAuthors(1L);
        Assert.assertEquals(Arrays.asList(101, 102),
                book.getAuthors().stream().map(Author::getAuthorId).collect(Collectors.toList()));
        Assert.assertTrue(bookMapper.selectWithAuthors(8L).getAuthors().isEmpty());
        Assert.assertNull(bookMapper.selectWithAuthors(99L));

        Author author = authorMapper.selectWithBooks(101);
        Assert.assertEquals("Lin Han", author.getName());
        Assert.assertEquals(Arrays.asList(1L, 3L, 6L),
                author.getBooks().stream().map(Book::getBookId).collect(Collectors.toList()));
        Assert.assertNull(authorMapper.selectWithBooks(999));
    }

    @Test
    public void lambdaConditionsNestedOrAndPaginationWorkOnBooks() {
        List<Book> books = bookMapper.selectList(Wrappers.<Book>lambdaQuery()
                .eq(Book::getCategory, "Technology")
                .and(q -> q.ge(Book::getPrice, new BigDecimal("140"))
                        .or().eq(Book::getTitle, "SQL Handbook"))
                .orderByAsc(Book::getBookId));
        Assert.assertEquals(Arrays.asList(1L, 3L, 6L),
                books.stream().map(Book::getBookId).collect(Collectors.toList()));

        Page<Book> page = bookMapper.selectPage(new Page<>(2, 2),
                Wrappers.<Book>lambdaQuery().eq(Book::getCategory, "Technology")
                        .orderByAsc(Book::getBookId));
        Assert.assertEquals(3L, page.getTotal());
        Assert.assertEquals(1, page.getRecords().size());
        Assert.assertEquals(Long.valueOf(6), page.getRecords().get(0).getBookId());
    }

    @Test
    public void partialFieldsMapsGroupingAndSelectByMapWork() {
        Book partial = bookMapper.selectList(Wrappers.<Book>lambdaQuery()
                .select(Book::getBookId, Book::getTitle).eq(Book::getBookId, 1L)).get(0);
        Assert.assertEquals("Java Foundations", partial.getTitle());
        Assert.assertNull(partial.getPrice());

        List<Map<String, Object>> grouped = bookMapper.selectMaps(new QueryWrapper<Book>()
                .select("category", "COUNT(*) AS book_count")
                .groupBy("category").orderByAsc("category"));
        Assert.assertEquals(3, grouped.size());
        Map<String, Long> byCategory = grouped.stream().collect(Collectors.toMap(
                row -> (String) row.get("category"),
                row -> ((Number) row.get("book_count")).longValue()));
        Assert.assertEquals(Long.valueOf(2), byCategory.get("Finance"));
        Assert.assertEquals(Long.valueOf(3), byCategory.get("Fiction"));
        Assert.assertEquals(Long.valueOf(3), byCategory.get("Technology"));
        Assert.assertEquals(3, bookMapper.selectByMap(Map.of("publisher_id", 20)).size());
        Assert.assertEquals(Long.valueOf(3), bookMapper.selectCount(
                Wrappers.<Book>lambdaQuery().eq(Book::getCategory, "Technology")));
        Assert.assertEquals("SQL Handbook", bookMapper.selectOne(
                Wrappers.<Book>lambdaQuery().eq(Book::getBookId, 3L)).getTitle());
    }

    @Test
    public void insertFillsTimesAndNullDiscountDoesNotOverwriteExistingValue() {
        Book created = new Book();
        created.setTitle("New Book");
        created.setCategory("Technology");
        created.setPublishedOn(LocalDate.of(2025, 1, 1));
        created.setPrice(new BigDecimal("50.00"));
        created.setPublisherId(20);
        Assert.assertEquals(1, bookMapper.insert(created));
        Assert.assertNotNull(created.getBookId());
        Assert.assertNotNull(created.getCreateTime());
        Assert.assertNotNull(created.getUpdateTime());
        Assert.assertEquals(Integer.valueOf(0), bookMapper.selectById(created.getBookId()).getVersion());

        Book existing = bookMapper.selectById(2L);
        existing.setTitle("City of Glass, Revised");
        existing.setDiscount(null);
        Assert.assertEquals(1, bookMapper.updateById(existing));
        Book stored = bookMapper.selectById(2L);
        Assert.assertEquals("City of Glass, Revised", stored.getTitle());
        Assert.assertEquals(new BigDecimal("15.00"), stored.getDiscount());
        Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
        Assert.assertNotNull(stored.getUpdateTime());
    }

    @Test
    public void logicalDeleteFiltersAllQueriesAndOptimisticLockRejectsStaleUpdate() throws Exception {
        Book first = bookMapper.selectById(1L);
        session.clearCache();
        Book stale = bookMapper.selectById(1L);
        first.setPrice(new BigDecimal("155.00"));
        Assert.assertEquals(1, bookMapper.updateById(first));
        stale.setPrice(new BigDecimal("165.00"));
        Assert.assertEquals(0, bookMapper.updateById(stale));
        Assert.assertEquals(new BigDecimal("155.00"), bookMapper.selectById(1L).getPrice());

        Assert.assertEquals(1, bookMapper.deleteById(1L));
        Assert.assertNull(bookMapper.selectById(1L));
        Assert.assertNull(bookMapper.selectWithPublisher(1L));
        Assert.assertEquals(Arrays.asList(3L, 6L), publisherMapper.selectWithBooks(20)
                .getBooks().stream().map(Book::getBookId).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(3L, 6L), authorMapper.selectWithBooks(101)
                .getBooks().stream().map(Book::getBookId).collect(Collectors.toList()));
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT deleted FROM book WHERE book_id = 1");
             ResultSet row = statement.executeQuery()) {
            Assert.assertTrue(row.next());
            Assert.assertEquals(1, row.getInt(1));
        }
    }

    @Test
    public void collectionsBecomeEmptyWhenAllLinkedBooksAreLogicallyDeleted() {
        bookMapper.deleteById(2L);
        bookMapper.deleteById(3L);
        bookMapper.deleteById(4L);
        bookMapper.deleteById(7L);
        Assert.assertTrue(authorMapper.selectWithBooks(103).getBooks().isEmpty());
        Assert.assertTrue(publisherMapper.selectWithBooks(30).getBooks().isEmpty());
    }
}
