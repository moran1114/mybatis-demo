package com.demo2.mapper;

import com.demo2.entity.Dept;
import org.apache.ibatis.annotations.Param;

public interface DeptMapper {
    Dept selectDeptWithEmps(@Param("deptno") Integer deptno);
}
