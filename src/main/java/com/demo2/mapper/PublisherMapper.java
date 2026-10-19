package com.demo2.mapper;

import com.demo2.entity.Publisher;
import org.apache.ibatis.annotations.Param;

public interface PublisherMapper {
    Publisher selectWithBooks(@Param("publisherId") Integer publisherId);
}
