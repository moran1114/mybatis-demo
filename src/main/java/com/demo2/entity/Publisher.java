package com.demo2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.List;

@TableName("publisher")
public class Publisher {
    @TableId("publisher_id")
    private Integer publisherId;
    private String name;
    private String city;
    @TableField(exist = false)
    private List<Book> books;

    public Integer getPublisherId() { return publisherId; }
    public void setPublisherId(Integer publisherId) { this.publisherId = publisherId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public List<Book> getBooks() { return books; }
    public void setBooks(List<Book> books) { this.books = books; }

    @Override
    public String toString() {
        return "Publisher{publisherId=" + publisherId + ", name='" + name + "', city='" + city + "'}";
    }
}
