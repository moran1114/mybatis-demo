package com.demo2.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo2.entity.Book;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookAdvancedQueryTest {
    private static SqlSessionFactory factory;
    private SqlSession session;
    private BookMapper mapper;

    @BeforeClass
    public static void setup() {
        EmbeddedDatabase.initialize("book_advanced");
        factory = MyBatisUtil.build(EmbeddedDatabase.properties("book_advanced"), DbType.H2);
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
    public void chainsRangeAndInConditions() {
        List<Book> books = mapper.selectList(new QueryWrapper<Book>()
                .in("publisher_id", 20, 30)
                .between("price", new BigDecimal("80"), new BigDecimal("200"))
                .orderByDesc("price").orderByAsc("book_id"));
        Assert.assertEquals(Arrays.asList(7L, 1L, 3L, 2L), ids(books));
    }

    @Test
    public void nestedOrKeepsPublisherScope() {
        List<Book> books = mapper.selectList(new QueryWrapper<Book>().eq("publisher_id", 20)
                .and(q -> q.like("title", "Java").or().ge("price", new BigDecimal("200")))
                .orderByAsc("book_id"));
        Assert.assertEquals(Arrays.asList(1L, 6L), ids(books));

        List<Book> alternatives = mapper.selectList(Wrappers.lambdaQuery(Book.class)
                .and(q -> q.eq(Book::getPublisherId, 20).ge(Book::getPrice, new BigDecimal("140")))
                .or(q -> q.eq(Book::getPublisherId, 30).isNotNull(Book::getDiscount))
                .orderByAsc(Book::getBookId));
        Assert.assertEquals(Arrays.asList(1L, 2L, 4L, 6L, 7L), ids(alternatives));
    }

    @Test
    public void optionalLambdaConditionsAndCountAndSelectOneWork() {
        Integer publisherId = null;
        String title = " ";
        BigDecimal minPrice = new BigDecimal("200");
        Assert.assertEquals(Arrays.asList(6L, 7L), ids(mapper.selectList(Wrappers.lambdaQuery(Book.class)
                .eq(publisherId != null, Book::getPublisherId, publisherId)
                .like(title != null && !title.isBlank(), Book::getTitle, title)
                .ge(minPrice != null, Book::getPrice, minPrice)
                .orderByAsc(Book::getBookId))));
        Assert.assertEquals(Long.valueOf(8), mapper.selectCount(null));
        Assert.assertEquals(Long.valueOf(3), mapper.selectCount(
                Wrappers.lambdaQuery(Book.class).eq(Book::getPublisherId, 20)));
        Assert.assertEquals("Java Foundations", mapper.selectOne(
                Wrappers.lambdaQuery(Book.class).eq(Book::getBookId, 1L)).getTitle());
        Assert.assertNull(mapper.selectOne(Wrappers.lambdaQuery(Book.class).eq(Book::getBookId, -1L)));
        Assert.assertThrows(TooManyResultsException.class, () -> mapper.selectOne(
                Wrappers.lambdaQuery(Book.class).eq(Book::getPublisherId, 20)));
    }

    @Test
    public void projectionMapsAndGroupingReturnOnlyRequestedData() {
        Book projected = mapper.selectOne(Wrappers.lambdaQuery(Book.class)
                .select(Book::getTitle, Book::getPrice, Book::getPublisherId)
                .eq(Book::getBookId, 1L));
        Assert.assertEquals("Java Foundations", projected.getTitle());
        Assert.assertNull(projected.getBookId());
        Assert.assertNull(projected.getCategory());
        Map<String, Object> row = mapper.selectMaps(new QueryWrapper<Book>()
                .select("title AS book_title", "price", "publisher_id")
                .eq("book_id", 1L)).get(0);
        Assert.assertEquals("Java Foundations", row.get("book_title"));
        Assert.assertEquals(3, row.size());

        List<Map<String, Object>> groups = mapper.selectMaps(new QueryWrapper<Book>()
                .select("publisher_id", "COUNT(*) AS book_count", "SUM(price) AS total_price")
                .groupBy("publisher_id").having("COUNT(*) >= {0}", 3)
                .orderByAsc("publisher_id"));
        Assert.assertEquals(2, groups.size());
        Assert.assertEquals(20, ((Number) groups.get(0).get("publisher_id")).intValue());
        Assert.assertEquals(3L, ((Number) groups.get(0).get("book_count")).longValue());
        Assert.assertEquals(0, new BigDecimal("465").compareTo(
                (BigDecimal) groups.get(0).get("total_price")));
        Assert.assertEquals(30, ((Number) groups.get(1).get("publisher_id")).intValue());
        Assert.assertEquals(0, new BigDecimal("350").compareTo(
                (BigDecimal) groups.get(1).get("total_price")));
    }

    @Test
    public void mapLookupSupportsNullAndLambdaUpdateChangesOnlyMatches() {
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("publisher_id", 20);
        columns.put("discount", null);
        Assert.assertEquals(Arrays.asList(1L, 3L), ids(mapper.selectByMap(columns)).stream()
                .sorted().collect(Collectors.toList()));

        Assert.assertEquals(2, mapper.update(null, Wrappers.lambdaUpdate(Book.class)
                .eq(Book::getPublisherId, 30).lt(Book::getPrice, new BigDecimal("100"))
                .set(Book::getDiscount, BigDecimal.ZERO)));
        Assert.assertEquals(new BigDecimal("0.00"), mapper.selectById(2L).getDiscount());
        Assert.assertEquals(new BigDecimal("20.00"), mapper.selectById(7L).getDiscount());
    }

    @Test
    public void notNullStrategyPreservesDiscountAndWrapperCanClearIt() {
        Book patch = new Book();
        patch.setBookId(2L);
        patch.setTitle("Revised City");
        patch.setDiscount(null);
        Assert.assertEquals(1, mapper.updateById(patch));
        Assert.assertEquals(new BigDecimal("15.00"), mapper.selectById(2L).getDiscount());

        patch.setDiscount(BigDecimal.ZERO);
        Assert.assertEquals(1, mapper.updateById(patch));
        Assert.assertEquals(new BigDecimal("0.00"), mapper.selectById(2L).getDiscount());
        Assert.assertEquals(1, mapper.update(null, Wrappers.lambdaUpdate(Book.class)
                .eq(Book::getBookId, 2L).set(Book::getDiscount, null)));
        Assert.assertNull(mapper.selectById(2L).getDiscount());
    }

    private static List<Long> ids(List<Book> books) {
        return books.stream().map(Book::getBookId).collect(Collectors.toList());
    }
}
