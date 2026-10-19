package com.demo2.mapper;

import com.demo2.entity.Book;
import com.demo2.entity.BookQuery;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.BookXmlDemo;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BookXmlMapperTest {
    private static SqlSessionFactory factory;
    private SqlSession session;
    private BookMapper mapper;

    @BeforeClass
    public static void setup() {
        EmbeddedDatabase.initialize("book_xml_mapper");
        factory = MyBatisUtil.buildMyBatis(EmbeddedDatabase.properties("book_xml_mapper"));
        factory.getConfiguration().addMapper(BookTitleMapper.class);
    }

    @Before
    public void openSession() {
        session = factory.openSession(false);
        mapper = session.getMapper(BookMapper.class);
    }

    @After
    public void rollbackAndClose() {
        session.rollback();
        session.close();
    }

    @Test
    public void xmlCrudReturnsGeneratedKey() {
        Book book = newBook("XML Practice", 40, "Technology", "89.00");
        Assert.assertEquals(1, mapper.insertBook(book));
        Assert.assertNotNull(book.getBookId());
        Book stored = mapper.selectBookById(book.getBookId());
        Assert.assertEquals("XML Practice", stored.getTitle());
        Assert.assertEquals(LocalDate.of(2025, 1, 6), stored.getPublishedOn());
        Assert.assertEquals(Integer.valueOf(40), stored.getPublisherId());
        stored.setTitle("XML Practice, Revised");
        stored.setPrice(new BigDecimal("99.00"));
        Assert.assertEquals(1, mapper.updateBook(stored));
        Assert.assertEquals("XML Practice, Revised", mapper.selectBookById(book.getBookId()).getTitle());
        Assert.assertEquals(1, mapper.deleteBookById(book.getBookId()));
        Assert.assertNull(mapper.selectBookById(book.getBookId()));
    }

    @Test
    public void dynamicWhereAndTrimHandleAllOptionalConditions() {
        BookQuery query = new BookQuery();
        query.setTitle("Java");
        query.setCategory("Technology");
        query.setPublisherId(20);
        query.setMinPrice(new BigDecimal("140"));
        Assert.assertEquals(Arrays.asList(1L), ids(mapper.selectByCondition(query)));
        Assert.assertEquals(ids(mapper.selectByCondition(query)), ids(mapper.selectByConditionWithTrim(query)));
        query.setPublisherId(30);
        Assert.assertTrue(mapper.selectByCondition(query).isEmpty());
        Assert.assertEquals(8, mapper.selectByCondition(null).size());
        Assert.assertEquals(8, mapper.selectByCondition(new BookQuery()).size());
        query.setTitle("");
        query.setCategory("");
        query.setPublisherId(20);
        query.setMinPrice(null);
        Assert.assertEquals(3, mapper.selectByCondition(query).size());
        Assert.assertEquals(ids(mapper.selectByCondition(query)), ids(mapper.selectByConditionWithTrim(query)));
    }

    @Test
    public void annotationQueryAndWhitelistedSortAreSafe() {
        Assert.assertTrue(Proxy.isProxyClass(mapper.getClass()));
        Assert.assertTrue(factory.getConfiguration().isMapUnderscoreToCamelCase());
        Assert.assertEquals("Java Foundations", session.getMapper(BookTitleMapper.class).title(1L).getBookTitle());
        Assert.assertEquals(3, mapper.countByPublisher(20));
        Assert.assertEquals(0, mapper.countByPublisher(99));
        Assert.assertEquals(Long.valueOf(6), mapper.selectSorted(BookQuery.SortColumn.PRICE).get(0).getBookId());
        Assert.assertEquals(Long.valueOf(8), mapper.selectSorted(BookQuery.SortColumn.PUBLISHED_ON).get(0).getBookId());
        Assert.assertEquals(Long.valueOf(8), mapper.selectSorted(null).get(0).getBookId());
        Assert.assertThrows(IllegalArgumentException.class,
                () -> BookQuery.SortColumn.valueOf("price; DELETE FROM book"));

        BookQuery malicious = new BookQuery();
        malicious.setTitle("' OR 1=1 --");
        Assert.assertTrue(mapper.selectByCondition(malicious).isEmpty());
        Assert.assertTrue(mapper.selectPreferred("' OR 1=1 --", 20).isEmpty());
    }

    @Test
    public void partialUpdatePreservesUnspecifiedColumns() {
        Book patch = new Book();
        patch.setBookId(1L);
        patch.setPrice(new BigDecimal("151.00"));
        patch.setDiscount(BigDecimal.ZERO);
        Assert.assertEquals(1, mapper.updatePartial(patch));
        Book stored = mapper.selectBookById(1L);
        Assert.assertEquals(new BigDecimal("151.00"), stored.getPrice());
        Assert.assertEquals(BigDecimal.ZERO.setScale(2), stored.getDiscount());
        Assert.assertEquals("Java Foundations", stored.getTitle());
        Assert.assertEquals(Integer.valueOf(20), stored.getPublisherId());
    }

    @Test
    public void foreachBatchAndChoosePreferenceWork() {
        List<Book> batch = Arrays.asList(
                newBook("Batch A", 50, "Technology", "82.00"),
                newBook("Batch B", 50, "Fiction", "86.00"));
        Assert.assertEquals(2, mapper.insertBatch(batch));
        Assert.assertEquals(1, mapper.selectPreferred("Batch A", 20).size());
        Assert.assertEquals(2, mapper.selectPreferred("", 50).size());
        Assert.assertEquals(10, mapper.selectPreferred(null, null).size());
        List<Long> ids = ids(mapper.selectPreferred(null, 50));
        Assert.assertEquals(2, mapper.deleteBatch(ids));
        Assert.assertTrue(mapper.selectPreferred(null, 50).isEmpty());
        Assert.assertEquals(8, mapper.selectAllBooks().size());
    }

    @Test
    public void emptyBatchDeleteDoesNothingAndChooseUsesOnlyFirstMatch() {
        Assert.assertEquals(0, mapper.deleteBatch(Collections.emptyList()));
        Assert.assertEquals(0, mapper.deleteBatch(null));
        Assert.assertEquals(8, mapper.selectAllBooks().size());
        Assert.assertEquals(Arrays.asList(1L), ids(mapper.selectPreferred("Java Foundations", 30)));
        Assert.assertTrue(mapper.selectPreferred("Java", 20).isEmpty());
        Assert.assertEquals(3, mapper.selectPreferred(null, 20).size());
    }

    @Test
    public void consoleXmlExampleRunsAgainstBookDatabase() {
        BookXmlDemo.run(mapper);
        Assert.assertEquals(8, mapper.selectAllBooks().size());
    }

    private static Book newBook(String title, int publisherId, String category, String price) {
        Book book = new Book();
        book.setTitle(title);
        book.setCategory(category);
        book.setPublishedOn(LocalDate.of(2025, 1, 6));
        book.setPrice(new BigDecimal(price));
        book.setPublisherId(publisherId);
        return book;
    }

    private static List<Long> ids(List<Book> books) {
        return books.stream().map(Book::getBookId).collect(Collectors.toList());
    }

    public interface BookTitleMapper {
        @Select("SELECT title AS book_title FROM book WHERE book_id = #{bookId}")
        TitleView title(@Param("bookId") Long bookId);
    }

    public static class TitleView {
        private String bookTitle;
        public String getBookTitle() { return bookTitle; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    }
}
