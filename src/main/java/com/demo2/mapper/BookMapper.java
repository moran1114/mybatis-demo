package com.demo2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo2.entity.Book;
import com.demo2.entity.BookQuery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface BookMapper extends BaseMapper<Book> {
    List<Book> selectAllBooks();
    List<Book> selectTitlePricePublisher();
    List<Book> selectPriceAbove100();
    List<Book> selectPublisher20Or30();
    List<Book> selectPublishedAfter2021();
    List<Book> selectWithDiscount();
    List<Book> selectFiction();
    List<Book> selectPriceBetween80And200();
    List<Book> selectWithoutDiscountAndPriceBelow150();
    List<Book> selectOrderByPriceDesc();

    Book selectBookById(@Param("bookId") Long bookId);
    int insertBook(Book book);
    int updateBook(Book book);
    int deleteBookById(@Param("bookId") Long bookId);
    List<Book> selectByCondition(@Param("query") BookQuery query);
    List<Book> selectByConditionWithTrim(@Param("query") BookQuery query);
    @Select("SELECT COUNT(*) FROM book WHERE publisher_id = #{publisherId} AND deleted = 0")
    int countByPublisher(@Param("publisherId") Integer publisherId);
    List<Book> selectSorted(@Param("sort") BookQuery.SortColumn sort);
    int updatePartial(Book book);
    int insertBatch(@Param("items") Collection<Book> items);
    int deleteBatch(@Param("ids") Collection<Long> ids);
    List<Book> selectPreferred(@Param("exactTitle") String exactTitle,
                               @Param("publisherId") Integer publisherId);

    Book selectWithDetail(@Param("bookId") Long bookId);
    Book selectWithPublisher(@Param("bookId") Long bookId);
    Book selectWithAuthors(@Param("bookId") Long bookId);
}
