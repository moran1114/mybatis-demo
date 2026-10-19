package com.demo2.mapper;

import com.demo2.entity.Author;
import org.apache.ibatis.annotations.Param;

public interface AuthorMapper {
    Author selectWithBooks(@Param("authorId") Integer authorId);
}
