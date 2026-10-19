package com.demo2.mapper;

import com.demo2.entity.Book;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class BookBasicQueryTest {
    private static SqlSessionFactory factory;
    private static List<String> statements;
    private final int number;
    private final String expectedTitles;

    public BookBasicQueryTest(int number, String expectedTitles) {
        this.number = number;
        this.expectedTitles = expectedTitles;
    }

    @Parameterized.Parameters(name = "basic book query {0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {1, "Java Foundations,City of Glass,SQL Handbook,Night Market,Ledger Basics,Cloud Atlas,River Stories,Pocket Guide"},
            {2, "Java Foundations,City of Glass,SQL Handbook,Night Market,Ledger Basics,Cloud Atlas,River Stories,Pocket Guide"},
            {3, "Java Foundations,Ledger Basics,Cloud Atlas,River Stories"},
            {4, "Java Foundations,City of Glass,SQL Handbook,Night Market,Cloud Atlas,River Stories"},
            {5, "Java Foundations,SQL Handbook,Cloud Atlas,River Stories,Pocket Guide"},
            {6, "City of Glass,Night Market,Cloud Atlas,River Stories"},
            {7, "City of Glass,Night Market,River Stories"},
            {8, "Java Foundations,City of Glass,SQL Handbook,Ledger Basics,River Stories"},
            {9, "Java Foundations,SQL Handbook,Pocket Guide"},
            {10, "Cloud Atlas,River Stories,Ledger Basics,Java Foundations,SQL Handbook,City of Glass,Pocket Guide,Night Market"}
        });
    }

    @BeforeClass
    public static void setup() throws Exception {
        EmbeddedDatabase.initialize("book_basic_queries");
        factory = MyBatisUtil.buildMyBatis(EmbeddedDatabase.properties("book_basic_queries"));
        String script = Files.readString(Paths.get("sql/book-basic-queries.sql"), StandardCharsets.UTF_8);
        statements = Arrays.stream(script.replaceAll("(?m)^--.*$", "").split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isEmpty() && !sql.toUpperCase(Locale.ROOT).startsWith("USE "))
                .collect(Collectors.toList());
        Assert.assertEquals(10, statements.size());
    }

    @Test
    public void sqlFileAndMapperReturnSameBooks() throws Exception {
        try (SqlSession session = factory.openSession();
             Statement statement = session.getConnection().createStatement()) {
            List<String> scriptTitles = new ArrayList<>();
            try (ResultSet result = statement.executeQuery(statements.get(number - 1))) {
                Assert.assertEquals(number == 2 ? 3 : 11, result.getMetaData().getColumnCount());
                while (result.next()) {
                    scriptTitles.add(result.getString("title"));
                }
            }
            assertTitles(scriptTitles);

            BookMapper mapper = session.getMapper(BookMapper.class);
            List<Book> books;
            switch (number) {
                case 1: books = mapper.selectAllBooks(); break;
                case 2: books = mapper.selectTitlePricePublisher(); break;
                case 3: books = mapper.selectPriceAbove100(); break;
                case 4: books = mapper.selectPublisher20Or30(); break;
                case 5: books = mapper.selectPublishedAfter2021(); break;
                case 6: books = mapper.selectWithDiscount(); break;
                case 7: books = mapper.selectFiction(); break;
                case 8: books = mapper.selectPriceBetween80And200(); break;
                case 9: books = mapper.selectWithoutDiscountAndPriceBelow150(); break;
                case 10: books = mapper.selectOrderByPriceDesc(); break;
                default: throw new AssertionError(number);
            }
            assertTitles(books.stream().map(Book::getTitle).collect(Collectors.toList()));
            if (number == 2) {
                Assert.assertNull(books.get(0).getBookId());
                Assert.assertNull(books.get(0).getCategory());
                Assert.assertNotNull(books.get(0).getPrice());
            }
        }
    }

    private void assertTitles(List<String> actual) {
        List<String> expected = new ArrayList<>(Arrays.asList(expectedTitles.split(",")));
        if (number != 10) {
            Collections.sort(expected);
            Collections.sort(actual);
        }
        Assert.assertEquals(expected, actual);
    }
}
