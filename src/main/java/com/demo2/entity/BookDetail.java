package com.demo2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("book_detail")
public class BookDetail {
    @TableId(value = "book_id", type = IdType.INPUT)
    private Long bookId;
    private String isbn;
    private String language;
    private String summary;
    private Integer pageCount;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    @Override
    public String toString() {
        return "BookDetail{bookId=" + bookId + ", isbn='" + isbn + "', pageCount=" + pageCount + "}";
    }
}
