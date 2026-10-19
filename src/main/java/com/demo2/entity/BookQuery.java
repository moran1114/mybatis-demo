package com.demo2.entity;

import java.math.BigDecimal;

public class BookQuery {
    private String title;
    private String category;
    private Integer publisherId;
    private BigDecimal minPrice;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPublisherId() { return publisherId; }
    public void setPublisherId(Integer publisherId) { this.publisherId = publisherId; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public enum SortColumn {
        BOOK_ID("book_id"), PRICE("price"), PUBLISHED_ON("published_on");

        private final String column;

        SortColumn(String column) { this.column = column; }
        public String getColumn() { return column; }
    }
}
