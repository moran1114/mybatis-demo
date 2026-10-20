package com.demo2.util;

import com.demo2.entity.Book;
import com.demo2.entity.BookQuery;
import com.demo2.mapper.BookMapper;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class BookXmlDemo {
    private BookXmlDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.openMyBatisSession()) {
            run(session.getMapper(BookMapper.class));
            session.rollback();
        }
        System.out.println("演示事务已回滚。");
    }

    public static void run(BookMapper mapper) {
        List<List<Book>> basics = Arrays.asList(
                mapper.selectAllBooks(),
                mapper.selectTitlePricePublisher(),
                mapper.selectPriceAbove100(),
                mapper.selectPublisher20Or30(),
                mapper.selectPublishedAfter2021(),
                mapper.selectWithDiscount(),
                mapper.selectFiction(),
                mapper.selectPriceBetween80And200(),
                mapper.selectWithoutDiscountAndPriceBelow150(),
                mapper.selectOrderByPriceDesc());
        for (int index = 0; index < basics.size(); index++) {
            System.out.println("基础查询 " + (index + 1) + "：" + basics.get(index).stream()
                    .map(Book::getTitle).collect(Collectors.toList()));
        }

        BookQuery query = new BookQuery();
        query.setTitle("Java");
        query.setCategory("Technology");
        query.setPublisherId(20);
        query.setMinPrice(new BigDecimal("140"));
        System.out.println("动态 WHERE（书名模糊匹配 Java，并叠加分类/出版社/最低价格）："
                + mapper.selectByCondition(query));
        System.out.println("动态 TRIM：" + mapper.selectByConditionWithTrim(query));
        System.out.println("注解查询，出版社 20 的书籍数量：" + mapper.countByPublisher(20));
        System.out.println("白名单排序：" + mapper.selectSorted(BookQuery.SortColumn.PRICE));
        System.out.println("CHOOSE 分支（优先按书名，否则按出版社）："
                + mapper.selectPreferred("Java Foundations", 30));

        Book book = newBook("XML 操作练习", 40);
        mapper.insertBook(book);
        System.out.println("主键回填：" + book.getBookId());
        Book stored = mapper.selectBookById(book.getBookId());
        stored.setTitle("XML 操作练习（修订）");
        mapper.updateBook(stored);
        Book patch = new Book();
        patch.setBookId(book.getBookId());
        patch.setPrice(new BigDecimal("98.00"));
        mapper.updatePartial(patch);
        System.out.println("全量和部分更新后：" + mapper.selectBookById(book.getBookId()));

        mapper.insertBatch(Arrays.asList(newBook("批量 A", 50), newBook("批量 B", 50)));
        List<Long> batchIds = mapper.selectPreferred(null, 50).stream()
                .map(Book::getBookId).collect(Collectors.toList());
        System.out.println("批量插入数量：" + batchIds.size());
        mapper.deleteBatch(batchIds);
        mapper.deleteBookById(book.getBookId());
        Book afterDelete = mapper.selectBookById(book.getBookId());
        System.out.println(afterDelete == null
                ? "编号 " + book.getBookId() + " 的练习书籍已删除，查询不到记录。"
                : "删除后查询：" + afterDelete);
    }

    private static Book newBook(String title, int publisherId) {
        Book book = new Book();
        book.setTitle(title);
        book.setCategory("Technology");
        book.setPublishedOn(LocalDate.of(2025, 1, 1));
        book.setPrice(new BigDecimal("88.00"));
        book.setPublisherId(publisherId);
        return book;
    }
}
