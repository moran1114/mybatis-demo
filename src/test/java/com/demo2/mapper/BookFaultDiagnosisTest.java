package com.demo2.mapper;

import com.demo2.entity.Book;
import com.demo2.support.EmbeddedDatabase;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Properties;

public class BookFaultDiagnosisTest {
    private static String mapperXml;

    @BeforeClass
    public static void setup() throws Exception {
        EmbeddedDatabase.initialize("book_fault_diagnosis");
        try (InputStream stream = Resources.getResourceAsStream("mapper/BookMapper.xml")) {
            mapperXml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void wrongJavaPropertyFailsWhenBindingInsert() {
        String broken = mapperXml.replace("VALUES (#{title}, #{category}",
                "VALUES (#{missingTitle}, #{category}");
        try (SqlSession session = build(broken).openSession()) {
            RuntimeException error = Assert.assertThrows(RuntimeException.class,
                    () -> session.getMapper(BookMapper.class).insertBook(sampleBook()));
            Throwable root = rootCause(error);
            Assert.assertTrue(root instanceof ReflectionException);
            Assert.assertTrue(root.getMessage().contains("missingTitle"));
            session.rollback();
        }
        assertCorrectInsert();
    }

    @Test
    public void wrongNamedParameterReportsBindingException() {
        String broken = mapperXml.replace("WHERE book_id = #{bookId} AND deleted = 0",
                "WHERE book_id = #{missingBookId} AND deleted = 0");
        try (SqlSession session = build(broken).openSession()) {
            RuntimeException error = Assert.assertThrows(RuntimeException.class,
                    () -> session.getMapper(BookMapper.class).selectBookById(1L));
            Throwable root = rootCause(error);
            Assert.assertTrue(root instanceof BindingException);
            Assert.assertTrue(root.getMessage().contains("missingBookId"));
        }
        try (SqlSession session = build(mapperXml).openSession()) {
            Assert.assertEquals("Java Foundations",
                    session.getMapper(BookMapper.class).selectBookById(1L).getTitle());
        }
    }

    @Test
    public void wrongResultColumnLeavesTitleNullWithoutAutomaticMapping() {
        String broken = mapperXml.replace("id=\"bookMap\"", "id=\"bookMap\" autoMapping=\"false\"")
                .replace("property=\"title\" column=\"title\"",
                        "property=\"title\" column=\"wrong_title\"");
        try (SqlSession session = build(broken).openSession()) {
            Book book = session.getMapper(BookMapper.class).selectBookById(1L);
            Assert.assertNotNull(book);
            Assert.assertNull(book.getTitle());
            Assert.assertEquals("Technology", book.getCategory());
        }
        try (SqlSession session = build(mapperXml).openSession()) {
            Assert.assertEquals("Java Foundations",
                    session.getMapper(BookMapper.class).selectBookById(1L).getTitle());
        }
    }

    @Test
    public void missingGeneratedKeysInsertsButDoesNotPopulateBookId() {
        String broken = mapperXml.replace(" useGeneratedKeys=\"true\"", "");
        try (SqlSession session = build(broken).openSession()) {
            Book book = sampleBook();
            BookMapper mapper = session.getMapper(BookMapper.class);
            Assert.assertEquals(1, mapper.insertBook(book));
            Assert.assertNull(book.getBookId());
            Assert.assertEquals(1, mapper.selectPreferred(book.getTitle(), null).size());
            session.rollback();
        }
        assertCorrectInsert();
    }

    private static void assertCorrectInsert() {
        try (SqlSession session = build(mapperXml).openSession()) {
            Book book = sampleBook();
            Assert.assertEquals(1, session.getMapper(BookMapper.class).insertBook(book));
            Assert.assertNotNull(book.getBookId());
            session.rollback();
        }
    }

    private static SqlSessionFactory build(String xml) {
        Properties properties = EmbeddedDatabase.properties("book_fault_diagnosis");
        UnpooledDataSource dataSource = new UnpooledDataSource(properties.getProperty("jdbc.driver"),
                properties.getProperty("jdbc.url"), "sa", "");
        Configuration configuration = new Configuration(
                new Environment("fault", new JdbcTransactionFactory(), dataSource));
        configuration.setLogImpl(Log4jImpl.class);
        configuration.setMapUnderscoreToCamelCase(true);
        new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), configuration,
                "book-fault-copy.xml", configuration.getSqlFragments()).parse();
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static Throwable rootCause(Throwable error) {
        while (error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static Book sampleBook() {
        Book book = new Book();
        book.setTitle("Fault Drill Book");
        book.setCategory("Technology");
        book.setPublishedOn(LocalDate.of(2025, 1, 6));
        book.setPrice(new BigDecimal("90.00"));
        book.setPublisherId(40);
        return book;
    }
}
